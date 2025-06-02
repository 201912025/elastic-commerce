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
    public static ProductResponse from(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getProductCode(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getRating()
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
