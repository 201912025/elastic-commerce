package com.example.ElasticCommerce.domain.order.dto.request;

import com.example.ElasticCommerce.domain.order.entity.Address;
import com.example.ElasticCommerce.domain.order.entity.OrderItem;
import com.example.ElasticCommerce.domain.product.repository.ProductRepository;
import com.example.ElasticCommerce.global.exception.type.NotFoundException;
import com.example.ElasticCommerce.domain.order.exception.OrderExceptionType;

import java.util.List;
import java.util.stream.Collectors;

public record CreateOrderRequest(
        List<OrderItemRequest> items,
        AddressRequest address
) {
    public List<OrderItem> toEntities(ProductRepository productRepo) {
        return items.stream()
                    .map(req -> {
                        var prod = productRepo.findById(req.productId())
                                              .orElseThrow(() -> new NotFoundException(OrderExceptionType.PRODUCT_NOT_FOUND));
                        if (prod.getStockQuantity() < req.quantity()) {
                            throw new IllegalStateException("재고 부족: productId=" + req.productId());
                        }
                        prod.updateStockQuantity(prod.getStockQuantity() - req.quantity());
                        return OrderItem.builder()
                                        .product(prod)
                                        .quantity(req.quantity())
                                        .price(prod.getPrice())
                                        .build();
                    })
                    .collect(Collectors.toList());
    }

    public Address toAddressEntity() {
        return Address.builder()
                      .order(null) // service 에서 setOrder 이후 overwrite
                      .recipientName(address.recipientName())
                      .street(address.street())
                      .city(address.city())
                      .postalCode(address.postalCode())
                      .phoneNumber(address.phoneNumber())
                      .build();
    }

    public record OrderItemRequest(Long productId, int quantity) {}

    public record AddressRequest(
            String recipientName,
            String street,
            String city,
            String postalCode,
            String phoneNumber
    ) {}
}