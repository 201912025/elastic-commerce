package com.example.ElasticCommerce.domain.review.dto.kafka;

import com.example.ElasticCommerce.domain.review.entity.Review;

import java.time.LocalDateTime;

public record ReviewElasticDTO(
        String id,
        Long productId,
        Long userId,
        String title,
        double rating,
        String comment,
        LocalDateTime createdAt,
        LocalDateTime modifiedAt,
        String eventType
) {
    public static ReviewElasticDTO from(
            Review review,
            String eventType
    ) {
        return new ReviewElasticDTO(
                review.getId().toString(),
                review.getProductId(),
                review.getUserId(),
                review.getTitle(),
                review.getRating(),
                review.getComment(),
                review.getCreatedAt(),
                review.getModifiedAt(),
                eventType
        );
    }
}
