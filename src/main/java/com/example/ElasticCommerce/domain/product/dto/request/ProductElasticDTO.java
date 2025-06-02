package com.example.ElasticCommerce.domain.product.dto.request;

import com.example.ElasticCommerce.domain.product.dto.response.ProductResponse;

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
        double rating
) {
    public static ProductElasticDTO from(ProductResponse productResponse) {
        return new ProductElasticDTO(
                productResponse.id().toString(),
                productResponse.productCode(),
                productResponse.name(),
                productResponse.category(),
                productResponse.stockQuantity(),
                productResponse.brand(),
                productResponse.imageUrl(),
                productResponse.available(),
                productResponse.description(),
                productResponse.price(),
                productResponse.rating()
        );
    }
}
