package com.example.ElasticCommerce.domain.cart.dto;

import java.math.BigDecimal;

public record AddCartItemRequest(
        Long productId,
        String productName,
        BigDecimal unitPrice,
        Integer quantity
) {}
