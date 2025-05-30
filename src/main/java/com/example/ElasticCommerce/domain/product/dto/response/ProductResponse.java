package com.example.ElasticCommerce.domain.product.dto.response;

import com.example.ElasticCommerce.domain.product.entity.Product;
import com.example.ElasticCommerce.domain.product.entity.ProductDocument;

public record ProductResponse(
        Long id,
        String productCode,
        String name,
        String description,
        Integer price,
        double rating,
        Boolean available
) {
    public static ProductResponse from(Product p) {
        return new ProductResponse(
                p.getId(),
                p.getProductCode(),
                p.getName(),
                p.getDescription(),
                p.getPrice(),
                p.getRating(),
                p.getAvailable()
        );
    }

    public static ProductResponse from(ProductDocument doc) {
        return new ProductResponse(
                Long.parseLong(doc.getId()),
                doc.getProductCode(),
                doc.getName(),
                doc.getDescription(),
                doc.getPrice(),
                doc.getRating(),
                doc.isAvailable()
        );
    }
}
