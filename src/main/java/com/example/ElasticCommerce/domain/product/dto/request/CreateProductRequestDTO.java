package com.example.ElasticCommerce.domain.product.dto.request;

public record CreateProductRequestDTO(
        String productCode,
        String name,
        String description,
        int price
) {
}
