package com.example.ElasticCommerce.domain.review.dto.response;

import com.example.ElasticCommerce.domain.product.entity.Product;
import com.example.ElasticCommerce.domain.review.entity.Review;

import java.time.LocalDateTime;

public record ReviewDetailResponse(
        Long id,
        String productCode,
        String productName,
        String productCategory,
        String userName,
        String title,
        double rating,
        String comment,
        LocalDateTime createdAt,
        LocalDateTime modifiedAt
) {
    public static ReviewDetailResponse from(Review review, Product product, String userName) {
        return new ReviewDetailResponse(
                review.getId(),
                product.getProductCode(),
                product.getName(),
                product.getCategory(),
                userName,
                review.getTitle(),
                review.getRating(),
                review.getComment(),
                review.getCreatedAt(),
                review.getModifiedAt()
        );
    }
}
