package com.example.ElasticCommerce.domain.order.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import com.example.ElasticCommerce.domain.order.entity.Address;
import com.example.ElasticCommerce.domain.order.entity.OrderItem;
import com.example.ElasticCommerce.domain.product.repository.ProductRepository;
import com.example.ElasticCommerce.global.exception.type.NotFoundException;
import com.example.ElasticCommerce.domain.order.exception.OrderExceptionType;

import java.util.List;
import java.util.stream.Collectors;

public record CreateOrderRequest(
        @NotEmpty(message = "최소 한 개 이상의 주문 아이템이 필요합니다.")
        @Valid
        List<OrderItemRequest> items,

        @NotNull(message = "배송지 정보가 필요합니다.")
        @Valid
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

    public record OrderItemRequest(
            @NotNull(message = "productId는 필수값입니다.")
            Long productId,

            @Min(value = 1, message = "quantity는 최소 {value} 이상이어야 합니다.")
            int quantity
    ) {}

    public record AddressRequest(
            @NotBlank(message = "recipientName은 빈 문자열일 수 없습니다.")
            @Size(max = 50, message = "recipientName은 최대 {max}자까지 입력 가능합니다.")
            String recipientName,

            @NotBlank(message = "street은 빈 문자열일 수 없습니다.")
            @Size(max = 100, message = "street은 최대 {max}자까지 입력 가능합니다.")
            String street,

            @NotBlank(message = "city는 빈 문자열일 수 없습니다.")
            @Size(max = 50, message = "city는 최대 {max}자까지 입력 가능합니다.")
            String city,

            @NotBlank(message = "postalCode는 빈 문자열일 수 없습니다.")
            @Pattern(regexp = "\\d{5,10}", message = "postalCode는 숫자 5~10자리여야 합니다.")
            String postalCode,

            @NotBlank(message = "phoneNumber는 빈 문자열일 수 없습니다.")
            @Pattern(regexp = "\\+?\\d{7,15}", message = "phoneNumber는 국제전화 형식의 숫자 7~15자리여야 합니다.")
            String phoneNumber
    ) {}
}
