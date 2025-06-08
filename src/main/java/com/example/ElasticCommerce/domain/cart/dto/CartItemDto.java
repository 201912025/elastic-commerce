package com.example.ElasticCommerce.domain.cart.dto;

import com.example.ElasticCommerce.domain.cart.entity.CartItem;

import java.math.BigDecimal;

public record CartItemDto(
        Long productId,
        String productName,
        BigDecimal unitPrice,
        Integer quantity,
        BigDecimal itemTotal
) {
    public static CartItemDto from(CartItem item) {
        return new CartItemDto(
                item.getProductId(),
                item.getProductName(),
                item.getUnitPrice(),
                item.getQuantity(),
                item.getTotalPrice()
        );
    }
}
