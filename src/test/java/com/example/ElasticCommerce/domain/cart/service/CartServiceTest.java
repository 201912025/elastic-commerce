package com.example.ElasticCommerce.domain.cart.service;

import com.example.ElasticCommerce.domain.cart.dto.AddCartItemRequest;
import com.example.ElasticCommerce.domain.cart.dto.CartDto;
import com.example.ElasticCommerce.domain.cart.dto.UpdateQuantityRequest;
import com.example.ElasticCommerce.domain.cart.entity.Cart;
import com.example.ElasticCommerce.domain.cart.entity.CartItem;
import com.example.ElasticCommerce.domain.cart.repository.CartItemRepository;
import com.example.ElasticCommerce.domain.cart.repository.CartRepository;
import com.example.ElasticCommerce.domain.user.entity.User;
import com.example.ElasticCommerce.domain.user.exception.UserExceptionType;
import com.example.ElasticCommerce.domain.user.repository.UserRepository;
import com.example.ElasticCommerce.global.exception.type.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.annotation.Profile;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Profile("test")
class CartServiceTest {

    @Mock private CartRepository cartRepository;
    @Mock private CartItemRepository cartItemRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks private CartService cartService;

    private final Long USER_ID = 1L;
    private User testUser;
    private Cart emptyCart;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                       .username("alice")
                       .email("alice@example.com")
                       .password("pw")
                       .role("USER")
                       .birthDay("1990-01-01")
                       .build();
        ReflectionTestUtils.setField(testUser, "userId", USER_ID);

        emptyCart = Cart.builder()
                        .user(testUser)
                        .build();
        ReflectionTestUtils.setField(emptyCart, "cartId", 10L);
    }

    @Test
    @DisplayName("아이템 처음 추가 시 새 장바구니가 생성되고 아이템이 담긴다")
    void addItem_NewCart() {
        // given
        AddCartItemRequest req = new AddCartItemRequest(100L, "제품A", new BigDecimal("9.99"), 2);

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
        when(cartRepository.findByUserUserId(USER_ID)).thenReturn(Optional.empty());
        when(cartRepository.save(any(Cart.class))).thenReturn(emptyCart);

        // when
        CartDto dto = cartService.addItem(USER_ID, req);

        // then
        assertThat(dto.userId()).isEqualTo(USER_ID);
        assertThat(dto.items()).hasSize(1);
        assertThat(dto.items().get(0).productId()).isEqualTo(100L);
        assertThat(dto.items().get(0).quantity()).isEqualTo(2);
        assertThat(dto.totalPrice()).isEqualByComparingTo(new BigDecimal("19.98"));

        ArgumentCaptor<CartItem> captor = ArgumentCaptor.forClass(CartItem.class);
        verify(cartItemRepository).save(captor.capture());
        CartItem saved = captor.getValue();
        assertThat(saved.getProductName()).isEqualTo("제품A");
        assertThat(saved.getUnitPrice()).isEqualByComparingTo(new BigDecimal("9.99"));
    }

    @Test
    @DisplayName("동일 아이템 재추가 시 수량만 덮어쓰기 된다")
    void addItem_UpdateQuantity() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
        when(cartRepository.findByUserUserId(USER_ID)).thenReturn(Optional.of(emptyCart));

        CartItem existing = CartItem.builder()
                                    .productId(101L).productName("제품B")
                                    .unitPrice(new BigDecimal("5.00")).quantity(1)
                                    .build();
        existing.setCart(emptyCart);
        emptyCart.addItem(existing);

        when(cartItemRepository.findByCartCartIdAndProductId(10L, 101L))
                .thenReturn(Optional.of(existing));

        CartDto dto2 = cartService.addItem(
                USER_ID,
                new AddCartItemRequest(101L, "제품B", new BigDecimal("5.00"), 3)
        );

        assertThat(dto2.items()).hasSize(1);
        assertThat(dto2.items().get(0).quantity()).isEqualTo(3);
        assertThat(dto2.totalPrice()).isEqualByComparingTo(new BigDecimal("15.00"));

        verify(cartItemRepository, never()).save(argThat(item -> item.getCart() == null));
        verify(cartItemRepository).save(existing);
    }

    @Test
    @DisplayName("장바구니 조회 시 없으면 예외 발생")
    void getCart_NotFound() {
        when(cartRepository.findByUserUserId(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.getCart(USER_ID))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining(UserExceptionType.NOT_FOUND_USER.getMessage());
    }

    @Test
    @DisplayName("수량 변경 전용 메서드 동작")
    void updateQuantity_Success() {
        when(cartRepository.findByUserUserId(USER_ID)).thenReturn(Optional.of(emptyCart));
        CartItem existing = CartItem.builder()
                                    .productId(120L).productName("제품Z")
                                    .unitPrice(new BigDecimal("4.00")).quantity(2)
                                    .build();
        existing.setCart(emptyCart);
        emptyCart.addItem(existing);

        when(cartItemRepository.findByCartCartIdAndProductId(10L, 120L))
                .thenReturn(Optional.of(existing));

        CartDto result = cartService.updateQuantity(
                USER_ID, 120L, new UpdateQuantityRequest(5)
        );

        assertThat(result.items().get(0).quantity()).isEqualTo(5);
        assertThat(result.totalPrice()).isEqualByComparingTo(new BigDecimal("20.00"));
    }

    @Test
    @DisplayName("전체 비우기 시 모든 아이템 삭제")
    void clearCart_Success() {
        when(cartRepository.findByUserUserId(USER_ID)).thenReturn(Optional.of(emptyCart));
        emptyCart.getItems().addAll(Collections.singletonList(
                CartItem.builder()
                        .productId(200L).productName("X")
                        .unitPrice(new BigDecimal("1.00")).quantity(1)
                        .build()
        ));
        // when
        cartService.clearCart(USER_ID);
        // then
        verify(cartItemRepository).deleteAll(anyList());
    }
}
