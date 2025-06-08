package com.example.ElasticCommerce.domain.payment.service;

import com.example.ElasticCommerce.domain.order.entity.Address;
import com.example.ElasticCommerce.domain.order.entity.Order;
import com.example.ElasticCommerce.domain.order.entity.OrderItem;
import com.example.ElasticCommerce.domain.order.entity.OrderStatus;
import com.example.ElasticCommerce.domain.order.repository.OrderRepository;
import com.example.ElasticCommerce.domain.payment.dto.request.PaymentRequest;
import com.example.ElasticCommerce.domain.payment.dto.response.PaymentDto;
import com.example.ElasticCommerce.domain.payment.entity.Payment;
import com.example.ElasticCommerce.domain.payment.entity.PaymentStatus;
import com.example.ElasticCommerce.domain.payment.exception.PaymentExceptionType;
import com.example.ElasticCommerce.domain.payment.repository.PaymentRepository;
import com.example.ElasticCommerce.domain.user.entity.User;
import com.example.ElasticCommerce.domain.user.exception.UserExceptionType;
import com.example.ElasticCommerce.domain.user.repository.UserRepository;
import com.example.ElasticCommerce.global.exception.type.BadRequestException;
import com.example.ElasticCommerce.global.exception.type.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private UserRepository userRepository;
    @InjectMocks
    private PaymentService paymentService;

    private User testUser;
    private Order testOrder;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                       .username("user")
                       .email("u@example.com")
                       .password("pass")
                       .role("USER")
                       .birthDay("1990-01-01")
                       .build();
        ReflectionTestUtils.setField(testUser, "userId", 1L);

        Address dummyAddress = Address.builder()
                                      .order(null)
                                      .recipientName("테스트")
                                      .street("테스트로 1")
                                      .city("도시")
                                      .postalCode("12345")
                                      .phoneNumber("010-0000-0000")
                                      .build();

        List<OrderItem> noItems = List.of();

        testOrder = Order.builder()
                         .user(testUser)
                         .items(noItems)
                         .address(dummyAddress)
                         .build();
        ReflectionTestUtils.setField(dummyAddress, "order", testOrder);
        ReflectionTestUtils.setField(testOrder, "id", 100L);
    }

    @Nested
    @DisplayName("createPayment 테스트")
    class CreatePayment {

        @Test
        @DisplayName("성공: 정상 결제 시 PaymentDto 반환")
        void 성공_정상_결제() {
            PaymentRequest req = new PaymentRequest("CARD", 500L);
            when(orderRepository.findByIdAndUserId(100L, 1L))
                    .thenReturn(Optional.of(testOrder));
            when(paymentRepository.save(any(Payment.class)))
                    .thenAnswer(i -> i.getArgument(0));

            PaymentDto dto = paymentService.createPayment(1L, 100L, req);

            assertThat(dto).isNotNull();
            assertThat(dto.orderId()).isEqualTo(100L);
            assertThat(dto.method()).isEqualTo("CARD");
            assertThat(dto.amount()).isEqualTo(500L);
            assertThat(dto.status()).isEqualTo(PaymentStatus.COMPLETED.name());
            assertThat(testOrder.getStatus()).isEqualTo(OrderStatus.PAID);

            verify(paymentRepository).save(any(Payment.class));
        }

        @Test
        @DisplayName("실패: 주문이 없으면 NotFoundException")
        void 실패_주문없음() {
            when(orderRepository.findByIdAndUserId(100L, 1L))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> paymentService.createPayment(1L, 100L, new PaymentRequest("CARD", 100L)))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining(PaymentExceptionType.PAYMENT_NOT_FOUND.getMessage());
        }

        @Test
        @DisplayName("실패: 상태가 CREATED가 아니면 BadRequestException")
        void 실패_잘못된_상태() {
            ReflectionTestUtils.setField(testOrder, "status", OrderStatus.PAID);
            when(orderRepository.findByIdAndUserId(100L, 1L))
                    .thenReturn(Optional.of(testOrder));

            assertThatThrownBy(() -> paymentService.createPayment(1L, 100L, new PaymentRequest("CARD", 100L)))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining(PaymentExceptionType.PAYMENT_NOT_ALLOWED.getMessage());
        }
    }

    @Nested
    @DisplayName("getPayment 테스트")
    class GetPayment {

        @Test
        @DisplayName("성공: 정상 조회 시 PaymentDto 반환")
        void 성공_정상조회() {
            Payment p = Payment.builder()
                               .order(testOrder)
                               .method("KAKAOPAY")
                               .amount(300L)
                               .status(PaymentStatus.COMPLETED)
                               .build();
            ReflectionTestUtils.setField(p, "id", 200L);
            when(paymentRepository.findByOrderId(100L))
                    .thenReturn(Optional.of(p));

            PaymentDto dto = paymentService.getPayment(1L, 100L);

            assertThat(dto.paymentId()).isEqualTo(200L);
            assertThat(dto.orderId()).isEqualTo(100L);
        }

        @Test
        @DisplayName("실패: 결제 내역 없으면 NotFoundException")
        void 실패_결제없음() {
            when(paymentRepository.findByOrderId(100L))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> paymentService.getPayment(1L, 100L))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining(PaymentExceptionType.PAYMENT_NOT_FOUND.getMessage());
        }

        @Test
        @DisplayName("실패: 사용자 불일치 시 BadRequestException")
        void 실패_사용자불일치() {
            User other = User.builder()
                             .username("other")
                             .email("o@example.com")
                             .password("pass")
                             .role("USER")
                             .birthDay("1990-01-01")
                             .build();
            ReflectionTestUtils.setField(other, "userId", 2L);

            ReflectionTestUtils.setField(testOrder, "user", other);

            Payment p = Payment.builder()
                               .order(testOrder)
                               .method("CARD")
                               .amount(100L)
                               .status(PaymentStatus.COMPLETED)
                               .build();
            ReflectionTestUtils.setField(p, "id", 300L);

            when(paymentRepository.findByOrderId(100L))
                    .thenReturn(Optional.of(p));

            assertThatThrownBy(() -> paymentService.getPayment(1L, 100L))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining(PaymentExceptionType.PAYMENT_NOT_ALLOWED.getMessage());
        }


        @Nested
        @DisplayName("listUserPayments 테스트")
        class ListUserPayments {

            @Test
            @DisplayName("성공: 페이징된 사용자 결제 내역 반환")
            void 성공_페이징조회() {
                when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
                Payment p1 = Payment.builder()
                                    .order(testOrder)
                                    .method("CARD")
                                    .amount(100L)
                                    .status(PaymentStatus.COMPLETED)
                                    .build();
                ReflectionTestUtils.setField(p1, "id", 400L);
                Page<Payment> page = new PageImpl<>(List.of(p1));
                when(paymentRepository.findAllByUserId(eq(1L), any(PageRequest.class)))
                        .thenReturn(page);

                Page<PaymentDto> result = paymentService.listUserPayments(1L, 0, 5);

                assertThat(result.getTotalElements()).isEqualTo(1);
                assertThat(result.getContent().get(0).paymentId()).isEqualTo(400L);
            }

            @Test
            @DisplayName("실패: 사용자가 없으면 NotFoundException")
            void 실패_사용자없음() {
                when(userRepository.findById(1L)).thenReturn(Optional.empty());

                assertThatThrownBy(() -> paymentService.listUserPayments(1L, 0, 5))
                        .isInstanceOf(NotFoundException.class)
                        .hasMessageContaining(UserExceptionType.NOT_FOUND_USER.getMessage());
            }
        }

        @Nested
        @DisplayName("listAllPayments 테스트")
        class ListAllPayments {

            @Test
            @DisplayName("성공: 페이징된 전체 결제 내역 반환")
            void 성공_전체페이징조회() {
                Payment p1 = Payment.builder()
                                    .order(testOrder)
                                    .method("CARD")
                                    .amount(150L)
                                    .status(PaymentStatus.COMPLETED)
                                    .build();
                ReflectionTestUtils.setField(p1, "id", 500L);
                Page<Payment> page = new PageImpl<>(List.of(p1));
                when(paymentRepository.findAll(any(PageRequest.class)))
                        .thenReturn(page);

                Page<PaymentDto> result = paymentService.listAllPayments(0, 10);

                assertThat(result.getTotalElements()).isEqualTo(1);
                assertThat(result.getContent().get(0).paymentId()).isEqualTo(500L);
            }
        }
    }
}
