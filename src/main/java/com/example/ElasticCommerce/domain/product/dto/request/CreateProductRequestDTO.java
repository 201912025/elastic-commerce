package com.example.ElasticCommerce.domain.product.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateProductRequestDTO(
        @NotBlank(message = "상품 이름은 필수입니다.")
        String name,

        @NotBlank(message = "카테고리는 필수입니다.")
        String category,

        @Positive(message = "재고 수량은 0보다 커야 합니다.")
        int stockQuantity,

        @NotBlank(message = "브랜드는 필수입니다.")
        String brand,

        @NotBlank(message = "이미지 URL은 필수입니다.")
        String imageUrl,

        @NotBlank(message = "설명은 필수입니다.")
        String description,

        @NotNull(message = "가격은 필수입니다.")
        @Positive(message = "가격은 0보다 커야 합니다.")
        Long price
) {}
