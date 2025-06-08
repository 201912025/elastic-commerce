package com.example.ElasticCommerce.domain.order.dto.response;

import com.example.ElasticCommerce.domain.order.entity.OrderItem;

public record OrderItemDto(
        Long productId,
        String productName,
        int quantity,
        long price
) {
    public static OrderItemDto from(OrderItem item) {
        return new OrderItemDto(
                item.getProduct().getId(),
                item.getProduct().getName(),
                item.getQuantity(),
                item.getPrice()
        );
    }
}
