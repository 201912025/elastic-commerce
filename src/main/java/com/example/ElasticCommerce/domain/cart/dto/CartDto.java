package com.example.ElasticCommerce.domain.cart.dto;

import com.example.ElasticCommerce.domain.cart.entity.Cart;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

public record CartDto(
        Long cartId,
        Long userId,
        List<CartItemDto> items,
        BigDecimal totalPrice
) {
    public static CartDto from(Cart cart) {
        List<CartItemDto> itemDtos = cart.getItems().stream()
                                         .map(CartItemDto::from)
                                         .collect(Collectors.toList());

        BigDecimal sum = itemDtos.stream()
                                 .map(CartItemDto::itemTotal)
                                 .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new CartDto(
                cart.getCartId(),
                cart.getUser().getUserId(),
                itemDtos,
                sum
        );
    }
}
