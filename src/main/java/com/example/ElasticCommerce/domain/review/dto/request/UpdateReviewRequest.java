package com.example.ElasticCommerce.domain.review.dto.request;

public record UpdateReviewRequest(
        String title,
        double rating,
        String comment
) {}