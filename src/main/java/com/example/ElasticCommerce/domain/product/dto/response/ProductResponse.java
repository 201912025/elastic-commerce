package com.example.ElasticCommerce.domain.product.dto.response;

import com.example.ElasticCommerce.domain.product.entity.Product;
import com.example.ElasticCommerce.domain.product.entity.ProductDocument;

public record ProductResponse(
        Long id,
        String productCode,
        String name,
        String description,
        Integer price,
        double rating
) {
    public static ProductResponse from(Product p) {
        return new ProductResponse(
                p.getId(),
                p.getProductCode(),
                p.getName(),
                p.getDescription(),
                p.getPrice(),
                p.getRating()
        );
    }

    public static ProductResponse from(ProductDocument doc) {
        return new ProductResponse(
                Long.parseLong(doc.getId()),
                doc.getProductCode(),
                doc.getName(),
                doc.getDescription(),
                doc.getPrice(),
                doc.getRating()
        );
    }
}
