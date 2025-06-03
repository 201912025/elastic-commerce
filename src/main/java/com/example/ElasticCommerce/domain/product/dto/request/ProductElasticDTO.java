package com.example.ElasticCommerce.domain.product.dto.request;

import com.example.ElasticCommerce.domain.product.entity.Product;

public record ProductElasticDTO(
        String id,
        String productCode,
        String name,
        String category,
        int stockQuantity,
        String brand,
        String imageUrl,
        boolean available,
        String description,
        Long price,
        double rating,
        String eventType
) {
    public static ProductElasticDTO from(Product product, String eventType) {
        return new ProductElasticDTO(
                product.getId().toString(),
                product.getProductCode(),
                product.getName(),
                product.getCategory(),
                product.getStockQuantity(),
                product.getBrand(),
                product.getImageUrl(),
                product.isAvailable(),
                product.getDescription(),
                product.getPrice(),
                product.getRating(),
                eventType
        );
    }
}
