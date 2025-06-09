package com.example.ElasticCommerce.domain.review.dto.response;

import com.example.ElasticCommerce.domain.review.entity.Review;
import java.time.LocalDateTime;

public record ReviewResponse(
        Long id,
        Long productId,
        Long userId,
        String title,
        double rating,
        String comment,
        LocalDateTime createdAt,
        LocalDateTime modifiedAt
) {
    public static ReviewResponse from(Review r) {
        return new ReviewResponse(
                r.getId(),
                r.getProductId(),
                r.getUserId(),
                r.getTitle(),
                r.getRating(),
                r.getComment(),
                r.getCreatedAt(),
                r.getModifiedAt()
        );
    }
}
