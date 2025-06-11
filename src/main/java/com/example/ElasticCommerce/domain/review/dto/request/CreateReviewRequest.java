package com.example.ElasticCommerce.domain.review.dto.request;

import jakarta.validation.constraints.*;

public record CreateReviewRequest(
        @NotNull(message = "productId는 필수값입니다.")
        Long productId,

        @NotNull(message = "userId는 필수값입니다.")
        Long userId,

        @NotBlank(message = "title은 빈 문자열일 수 없습니다.")
        @Size(max = 100, message = "title은 최대 {max}자까지 입력 가능합니다.")
        String title,

        @Min(value = 1, message = "rating은 최소 {value} 이상이어야 합니다.")
        @Max(value = 5, message = "rating은 최대 {value} 이하여야 합니다.")
        double rating,

        @NotBlank(message = "comment는 빈 문자열일 수 없습니다.")
        @Size(max = 1000, message = "comment는 최대 {max}자까지 입력 가능합니다.")
        String comment
) {}
