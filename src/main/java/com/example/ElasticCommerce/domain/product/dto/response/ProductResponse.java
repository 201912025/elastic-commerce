package com.example.ElasticCommerce.domain.product.dto.response;

import com.example.ElasticCommerce.domain.product.entity.Product;
import com.example.ElasticCommerce.domain.product.entity.ProductDocument;

public record ProductResponse(
        Long id,
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
    public static ProductResponse from(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getProductCode(),
                product.getName(),
                product.getCategory(),
                product.getStockQuantity(),
                product.getBrand(),
                product.getImageUrl(),
                product.isAvailable(),
                product.getDescription(),
                product.getPrice().longValue(),
                product.getRating()
        );
    }

    public static ProductResponse from(ProductDocument doc) {
        return new ProductResponse(
                Long.parseLong(doc.getId()),
                doc.getProductCode(),
                doc.getName(),
                doc.getCategory(),
                doc.getStockQuantity(),
                doc.getBrand(),
                doc.getImageUrl(),
                doc.getAvailable(),
                doc.getDescription(),
                doc.getPrice().longValue(),
                doc.getRating()
        );
    }
}
