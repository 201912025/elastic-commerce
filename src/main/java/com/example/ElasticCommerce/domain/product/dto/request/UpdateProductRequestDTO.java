package com.example.ElasticCommerce.domain.product.dto.request;

import com.example.ElasticCommerce.domain.product.entity.Product;

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
