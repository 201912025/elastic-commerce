package com.example.ElasticCommerce.domain.product.dto.request;

public record CreateProductRequestDTO(
        String name,
        String category,
        int stockQuantity,
        String brand,
        String imageUrl,
        String description,
        Long price
) {
}
