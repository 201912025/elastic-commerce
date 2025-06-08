package com.example.ElasticCommerce.domain.cart.service;

import com.example.ElasticCommerce.domain.cart.dto.AddCartItemRequest;
import com.example.ElasticCommerce.domain.cart.dto.CartDto;
import com.example.ElasticCommerce.domain.cart.dto.UpdateQuantityRequest;
import com.example.ElasticCommerce.domain.cart.entity.Cart;
import com.example.ElasticCommerce.domain.cart.entity.CartItem;
import com.example.ElasticCommerce.domain.cart.exception.CartExceptionType;
import com.example.ElasticCommerce.domain.cart.repository.CartItemRepository;
import com.example.ElasticCommerce.domain.cart.repository.CartRepository;
import com.example.ElasticCommerce.domain.user.entity.User;
import com.example.ElasticCommerce.domain.user.exception.UserExceptionType;
import com.example.ElasticCommerce.domain.user.repository.UserRepository;
import com.example.ElasticCommerce.global.exception.type.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;

    @Transactional
    public CartDto addItem(Long userId, AddCartItemRequest req) {
        log.info("addItem 호출됨: 사용자Id={}, 상품Id={}, 상품명={}, 단가={}, 수량={}",
                userId, req.productId(), req.productName(), req.unitPrice(), req.quantity());

        User user = userRepository.findById(userId)
                                  .orElseThrow(() -> {
                                      log.warn("사용자를 찾을 수 없습니다: 사용자Id={}", userId);
                                      return new NotFoundException(UserExceptionType.NOT_FOUND_USER);
                                  });

        Cart cart = cartRepository.findByUserUserId(userId)
                                  .orElseGet(() -> {
                                      log.info("장바구니가 없어 새로 생성합니다: 사용자Id={}", userId);
                                      return cartRepository.save(Cart.builder()
                                                                     .user(user)
                                                                     .build());
                                  });

        Optional<CartItem> existingOpt = cartItemRepository
                .findByCartCartIdAndProductId(cart.getCartId(), req.productId());

        CartItem item;
        if (existingOpt.isPresent()) {
            item = existingOpt.get();
            log.info("기존 아이템 발견: 장바구니Id={}, 상품Id={}, 이전수량={}, 변경수량={}",
                    cart.getCartId(), req.productId(), item.getQuantity(), req.quantity());
            item.updateQuantity(req.quantity());
        } else {
            log.info("새 아이템 추가: 장바구니Id={}, 상품Id={}, 상품명={}, 단가={}, 수량={}",
                    cart.getCartId(), req.productId(), req.productName(), req.unitPrice(), req.quantity());
            item = CartItem.builder()
                           .productId(req.productId())
                           .productName(req.productName())
                           .unitPrice(req.unitPrice())
                           .quantity(req.quantity())
                           .build();
            cart.addItem(item);
        }

        cartItemRepository.save(item);
        return CartDto.from(cart);
    }

    public CartDto getCart(Long userId) {
        log.info("getCart 호출됨: 사용자Id={}", userId);
        Cart cart = cartRepository.findByUserUserId(userId)
                                  .orElseThrow(() -> {
                                      log.warn("장바구니를 찾을 수 없습니다: 사용자Id={}", userId);
                                      return new NotFoundException(UserExceptionType.NOT_FOUND_USER);
                                  });
        return CartDto.from(cart);
    }

    @Transactional
    public CartDto updateQuantity(Long userId, Long productId, UpdateQuantityRequest req) {
        log.info("updateQuantity 호출됨: 사용자Id={}, 상품Id={}, 새수량={}",
                userId, productId, req.quantity());
        if (req.quantity() == null || req.quantity() < 1) {
            log.warn("잘못된 수량 요청: 수량={}", req.quantity());
            throw new NotFoundException(CartExceptionType.INVALID_ITEM_QUANTITY);
        }
        Cart cart = cartRepository.findByUserUserId(userId)
                                  .orElseThrow(() -> {
                                      log.warn("장바구니를 찾을 수 없습니다: 사용자Id={}", userId);
                                      return new NotFoundException(UserExceptionType.NOT_FOUND_USER);
                                  });
        CartItem item = cartItemRepository.findByCartCartIdAndProductId(cart.getCartId(), productId)
                                          .orElseThrow(() -> {
                                              log.warn("장바구니 아이템을 찾을 수 없습니다: 장바구니Id={}, 상품Id={}",
                                                      cart.getCartId(), productId);
                                              return new NotFoundException(CartExceptionType.CART_ITEM_NOT_FOUND);
                                          });

        log.info("수량 변경: 장바구니Id={}, 상품Id={}, 이전수량={}, 새수량={}",
                cart.getCartId(), productId, item.getQuantity(), req.quantity());
        item.updateQuantity(req.quantity());
        cartItemRepository.save(item);
        return CartDto.from(cart);
    }

    @Transactional
    public void removeItem(Long userId, Long productId) {
        log.info("removeItem 호출됨: 사용자Id={}, 상품Id={}", userId, productId);
        Cart cart = cartRepository.findByUserUserId(userId)
                                  .orElseThrow(() -> {
                                      log.warn("장바구니를 찾을 수 없습니다: 사용자Id={}", userId);
                                      return new NotFoundException(UserExceptionType.NOT_FOUND_USER);
                                  });
        CartItem item = cartItemRepository.findByCartCartIdAndProductId(cart.getCartId(), productId)
                                          .orElseThrow(() -> {
                                              log.warn("장바구니 아이템을 찾을 수 없습니다: 장바구니Id={}, 상품Id={}",
                                                      cart.getCartId(), productId);
                                              return new NotFoundException(CartExceptionType.CART_ITEM_NOT_FOUND);
                                          });
        log.info("아이템 제거: 장바구니Id={}, 상품Id={}", cart.getCartId(), productId);
        cart.removeItem(item);
        cartItemRepository.delete(item);
    }

    @Transactional
    public void clearCart(Long userId) {
        log.info("clearCart 호출됨: 사용자Id={}", userId);
        Cart cart = cartRepository.findByUserUserId(userId)
                                  .orElseThrow(() -> {
                                      log.warn("장바구니를 찾을 수 없습니다: 사용자Id={}", userId);
                                      return new NotFoundException(UserExceptionType.NOT_FOUND_USER);
                                  });
        List<CartItem> allItems = cart.getItems().stream().collect(Collectors.toList());
        log.info("장바구니 전체 삭제: 장바구니Id={}, 아이템수={}", cart.getCartId(), allItems.size());
        allItems.forEach(cart::removeItem);
        cartItemRepository.deleteAll(allItems);
    }
}
