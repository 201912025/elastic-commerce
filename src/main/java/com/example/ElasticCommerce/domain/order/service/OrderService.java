package com.example.ElasticCommerce.domain.order.service;

import com.example.ElasticCommerce.domain.notification.dto.NotificationRequest;
import com.example.ElasticCommerce.domain.notification.service.NotificationProducerService;
import com.example.ElasticCommerce.domain.order.dto.request.CreateOrderRequest;
import com.example.ElasticCommerce.domain.order.dto.request.UpdateOrderStatusRequest;
import com.example.ElasticCommerce.domain.order.dto.response.OrderDto;
import com.example.ElasticCommerce.domain.order.entity.Address;
import com.example.ElasticCommerce.domain.order.entity.Order;
import com.example.ElasticCommerce.domain.order.entity.OrderItem;
import com.example.ElasticCommerce.domain.order.entity.OrderStatus;
import com.example.ElasticCommerce.domain.order.exception.OrderExceptionType;
import com.example.ElasticCommerce.domain.order.repository.OrderRepository;
import com.example.ElasticCommerce.domain.product.entity.Product;
import com.example.ElasticCommerce.domain.product.repository.ProductRepository;
import com.example.ElasticCommerce.domain.user.entity.User;
import com.example.ElasticCommerce.domain.user.exception.UserExceptionType;
import com.example.ElasticCommerce.domain.user.repository.UserRepository;
import com.example.ElasticCommerce.global.exception.type.BadRequestException;
import com.example.ElasticCommerce.global.exception.type.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final NotificationProducerService notificationService;


    @Transactional
    public OrderDto createOrder(Long userId, CreateOrderRequest req) {
        log.info("주문 생성 시작: userId={}, items={}, address={}", userId, req.items(), req.address());
        User user = userRepository.findById(userId)
                                  .orElseThrow(() -> {
                                      log.warn("주문 생성 실패: 사용자 없음 userId={}", userId);
                                      return new NotFoundException(UserExceptionType.NOT_FOUND_USER);
                                  });

        List<OrderItem> items = req.toEntities(productRepository);
        Address address = req.toAddressEntity();
        Order order = Order.builder()
                           .user(user)
                           .items(items)
                           .address(address)
                           .build();
        address.setOrder(order);

        orderRepository.save(order);
        log.info("주문 생성 완료: orderId={}, userId={}", order.getId(), userId);

        long totalPrice = items.stream()
                               .mapToLong(item -> item.getQuantity() * item.getProduct().getPrice())
                               .sum();

        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronizationAdapter() {
                    @Override
                    public void afterCommit() {
                        notificationService.sendAll(
                                                   new NotificationRequest(
                                                           order.getId(),
                                                           "ORDER_COMPLETED",
                                                           user.getEmail(),
                                                           totalPrice
                                                   )
                                           )
                                           .subscribe(
                                                   null,
                                                   e -> log.error("Kafka 알림 최종 실패 (주문 생성): orderId={}, error={}",
                                                           order.getId(), e.getMessage())
                                           );
                    }
                }
        );

        return order.toDto();
    }

    public OrderDto getOrder(Long userId, Long orderId) {
        log.info("단일 주문 조회 시작: userId={}, orderId={}", userId, orderId);
        Order order = findOrderOrThrow(userId, orderId);
        log.info("단일 주문 조회 완료: orderId={}", orderId);
        return order.toDto();
    }

    public Page<OrderDto> listOrders(Long userId, int page, int size) {
        log.info("주문 목록 조회 시작: userId={}, page={}, size={}", userId, page, size);
        userRepository.findById(userId)
                      .orElseThrow(() -> {
                          log.warn("주문 목록 조회 실패: 사용자 없음 userId={}", userId);
                          return new NotFoundException(UserExceptionType.NOT_FOUND_USER);
                      });

        PageRequest pageRequest = PageRequest.of(page, size);
        Page<OrderDto> result = orderRepository.findAllByUserUserId(userId, pageRequest)
                                               .map(Order::toDto);
        log.info("주문 목록 조회 완료: userId={}, 조회건수={}", userId, result.getNumberOfElements());
        return result;
    }

    @Transactional
    public OrderDto updateOrderStatus(Long userId, Long orderId, UpdateOrderStatusRequest req) {
        log.info("주문 상태 변경 시작: userId={}, orderId={}, newStatus={}", userId, orderId, req.newStatus());
        Order order = findOrderOrThrow(userId, orderId);
        OrderStatus newStatus = OrderStatus.valueOf(req.newStatus());
        order.changeStatus(newStatus);
        log.info("주문 상태 변경 완료: orderId={}, 상태={}", orderId, newStatus);
        return order.toDto();
    }

    @Transactional
    public OrderDto cancelOrder(Long userId, Long orderId) {
        log.info("주문 취소 시작: userId={}, orderId={}", userId, orderId);
        Order order = findOrderOrThrow(userId, orderId);

        OrderStatus status = order.getStatus();
        if (status != OrderStatus.CREATED && status != OrderStatus.PAID) {
            log.warn("취소 불가 상태: orderId={}, status={}", orderId, status);
            throw new BadRequestException(OrderExceptionType.ORDER_CANNOT_CANCEL);
        }

        long totalPrice = order.getItems().stream()
                               .mapToLong(item -> item.getQuantity() * item.getProduct().getPrice())
                               .sum();

        order.getItems().forEach(item -> {
            Product product = item.getProduct();
            int restored = product.getStockQuantity() + item.getQuantity();
            product.updateStockQuantity(restored);
            log.info("재고 복원: productId={}, restoredQuantity={}", product.getId(), restored);
        });

        order.cancel();
        log.info("주문 취소 완료: orderId={}", orderId);

        User user = order.getUser();
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronizationAdapter() {
                    @Override
                    public void afterCommit() {
                        notificationService.sendAll(
                                                   new NotificationRequest(
                                                           order.getId(),
                                                           "ORDER_CANCELLED",
                                                           user.getEmail(),
                                                           totalPrice
                                                   )
                                           )
                                           .subscribe(
                                                   null,
                                                   e -> log.error("Kafka 알림 최종 실패 (주문 취소): orderId={}, error={}",
                                                           order.getId(), e.getMessage())
                                           );
                    }
                }
        );

        return order.toDto();
    }

    private Order findOrderOrThrow(Long userId, Long orderId) {
        return orderRepository.findByIdAndUserId(orderId, userId)
                              .orElseThrow(() -> {
                                  log.warn("주문 조회 실패: userId={}, orderId={}", userId, orderId);
                                  return new NotFoundException(OrderExceptionType.ORDER_NOT_FOUND);
                              });
    }
}
