package com.example.ElasticCommerce.domain.product.dto.request;

import com.example.ElasticCommerce.domain.product.entity.Product;

// 모든 필드를 nullable로 정의해서, PATCH 시 필요한 필드만 전달하도록 함
public record UpdateProductRequestDTO(
        String name,
        String category,
        Integer stockQuantity,
        String brand,
        String imageUrl,
        String description,
        Long price
) {
    public static UpdateProductRequestDTO from(Product product) {
        return new UpdateProductRequestDTO(
                product.getName(),
                product.getCategory(),
                product.getStockQuantity(),
                product.getBrand(),
                product.getImageUrl(),
                product.getDescription(),
                product.getPrice()
        );
    }
}
