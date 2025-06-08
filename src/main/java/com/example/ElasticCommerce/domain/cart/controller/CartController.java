package com.example.ElasticCommerce.domain.cart.controller;

import com.example.ElasticCommerce.domain.cart.dto.*;
import com.example.ElasticCommerce.domain.cart.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @PostMapping("/{userId}/items")
    public ResponseEntity<CartDto> addItem(
            @PathVariable Long userId,
            @RequestBody AddCartItemRequest req) {
        return ResponseEntity.ok(cartService.addItem(userId, req));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<CartDto> getCart(@PathVariable Long userId) {
        return ResponseEntity.ok(cartService.getCart(userId));
    }

    @PatchMapping("/{userId}/items/{productId}")
    public ResponseEntity<CartDto> updateQuantity(
            @PathVariable Long userId,
            @PathVariable Long productId,
            @RequestBody UpdateQuantityRequest req) {
        return ResponseEntity.ok(cartService.updateQuantity(userId, productId, req));
    }

    @DeleteMapping("/{userId}/items/{productId}")
    public ResponseEntity<Void> removeItem(
            @PathVariable Long userId,
            @PathVariable Long productId) {
        cartService.removeItem(userId, productId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> clearCart(@PathVariable Long userId) {
        cartService.clearCart(userId);
        return ResponseEntity.ok().build();
    }
}
