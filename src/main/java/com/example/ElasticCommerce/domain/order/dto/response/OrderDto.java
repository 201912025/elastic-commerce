package com.example.ElasticCommerce.domain.order.dto.response;

import com.example.ElasticCommerce.domain.order.entity.Order;
import com.example.ElasticCommerce.domain.order.entity.OrderItem;

import java.time.LocalDateTime;
import java.util.List;

public record OrderDto(
        Long orderId,
        Long userId,
        List<OrderItemDto> items,
        String status,
        long totalAmount,
        AddressDto address,
        LocalDateTime createdAt
) {
    public static OrderDto from(Order order) {
        long sum = order.getItems().stream()
                        .mapToLong(OrderItem::getTotalPrice)
                        .sum();
        return new OrderDto(
                order.getId(),
                order.getUser().getUserId(),
                order.getItems().stream().map(OrderItemDto::from).toList(),
                order.getStatus().name(),
                sum,
                AddressDto.from(order.getAddress()),
                order.getCreatedAt()
        );
    }
}