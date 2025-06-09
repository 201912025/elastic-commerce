package com.example.ElasticCommerce.domain.review.dto.request;

public record CreateReviewRequest(
        Long productId,
        Long userId,
        String title,
        double rating,
        String comment
) {}
