package com.example.ElasticCommerce.domain.payment.service;

import com.example.ElasticCommerce.domain.notification.dto.NotificationRequest;
import com.example.ElasticCommerce.domain.notification.service.NotificationProducerService;
import com.example.ElasticCommerce.domain.order.entity.OrderStatus;
import com.example.ElasticCommerce.domain.payment.dto.request.PaymentRequest;
import com.example.ElasticCommerce.domain.payment.dto.response.PaymentDto;
import com.example.ElasticCommerce.domain.payment.entity.Payment;
import com.example.ElasticCommerce.domain.payment.entity.PaymentStatus;
import com.example.ElasticCommerce.domain.payment.exception.PaymentExceptionType;
import com.example.ElasticCommerce.domain.payment.repository.PaymentRepository;
import com.example.ElasticCommerce.domain.order.entity.Order;
import com.example.ElasticCommerce.domain.order.repository.OrderRepository;
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

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final NotificationProducerService notificationProducerService;

    @Transactional
    public PaymentDto createPayment(Long userId, Long orderId, PaymentRequest req) {
        log.info("결제 시작: userId={}, orderId={}, method={}, amount={}",
                userId, orderId, req.paymentMethod(), req.amount());

        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                                     .orElseThrow(() -> {
                                         log.warn("결제 실패: 주문을 찾을 수 없음 orderId={}", orderId);
                                         return new NotFoundException(PaymentExceptionType.PAYMENT_NOT_FOUND);
                                     });

        if (order.getStatus() != OrderStatus.CREATED) {
            log.warn("결제 불가 상태: orderId={}, status={}", orderId, order.getStatus());
            throw new BadRequestException(PaymentExceptionType.PAYMENT_NOT_ALLOWED);
        }

        Payment payment = Payment.builder()
                                 .order(order)
                                 .method(req.paymentMethod())
                                 .amount(req.amount())
                                 .status(PaymentStatus.PENDING)
                                 .build();

        payment.complete();
        order.changeStatus(OrderStatus.PAID);

        paymentRepository.save(payment);
        log.info("결제 완료: paymentId={}", payment.getId());

        User user = order.getUser();

        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronizationAdapter() {
                    @Override
                    public void afterCommit() {
                        notificationProducerService.sendAll(
                                                   new NotificationRequest(
                                                           order.getId(),
                                                           "PAYMENT_COMPLETED",
                                                           user.getEmail(),
                                                           payment.getAmount()
                                                   )
                                           )
                                                   .subscribe(
                                                   null,
                                                   e -> log.error("Kafka 알림 최종 실패: paymentId={}, error={}",
                                                           payment.getId(), e.getMessage())
                                           );
                    }
                }
        );

        return PaymentDto.from(payment);
    }

    @Transactional(readOnly = true)
    public PaymentDto getPayment(Long userId, Long orderId) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                                     .orElseThrow(() -> new NotFoundException(PaymentExceptionType.PAYMENT_NOT_FOUND));
        if (!payment.getOrder().getUser().getUserId().equals(userId)) {
            throw new BadRequestException(PaymentExceptionType.PAYMENT_NOT_ALLOWED);
        }
        return PaymentDto.from(payment);
    }

    @Transactional(readOnly = true)
    public Page<PaymentDto> listUserPayments(Long userId, int page, int size) {
        log.info("결제 내역 조회 시작: userId={}, page={}, size={}", userId, page, size);

        userRepository.findById(userId)
                      .orElseThrow(() -> new NotFoundException(UserExceptionType.NOT_FOUND_USER));

        PageRequest pageRequest = PageRequest.of(page, size);
        Page<Payment> payments = paymentRepository.findAllByUserId(userId, pageRequest);

        log.info("결제 내역 조회 완료: userId={}, 건수={}", userId, payments.getNumberOfElements());
        return payments.map(PaymentDto::from);
    }


    @Transactional(readOnly = true)
    public Page<PaymentDto> listAllPayments(int page, int size) {
        log.info("전체 결제 내역 조회 시작: page={}, size={}", page, size);
        PageRequest pageRequest = PageRequest.of(page, size);
        Page<Payment> payments = paymentRepository.findAll(pageRequest);
        log.info("전체 결제 내역 조회 완료: 건수={}", payments.getNumberOfElements());
        return payments.map(PaymentDto::from);
    }
}
