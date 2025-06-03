package com.example.ElasticCommerce.domain.product.dto.request;

import com.example.ElasticCommerce.domain.product.entity.Product;
import jakarta.validation.constraints.*;

public record UpdateProductRequestDTO(
        @Size(max = 100, message = "상품명은 최대 100자까지 입력할 수 있습니다.")
        String name,

        @Size(max = 50, message = "카테고리는 최대 50자까지 입력할 수 있습니다.")
        String category,

        @Min(value = 0, message = "재고 수량은 0 이상이어야 합니다.")
        @Max(value = 10000, message = "재고 수량은 최대 10,000까지 허용됩니다.")
        Integer stockQuantity,

        @Size(max = 50, message = "브랜드명은 최대 50자까지 입력할 수 있습니다.")
        String brand,

        @Size(max = 255, message = "이미지 URL은 최대 255자까지 입력할 수 있습니다.")
        String imageUrl,

        @Size(max = 1000, message = "설명은 최대 1000자까지 입력할 수 있습니다.")
        String description,

        @Min(value = 0, message = "가격은 0 이상이어야 합니다.")
        Long price
) {
    public static UpdateProductRequestDTO from(Product product) {
        return new UpdateProductRequestDTO(
                product.getName(),
                product.getCategory(),
                product.getStockQuantity(),
                product.getBrand(),
                product.getImageUrl(),
                product.getDescription(),
                product.getPrice()
        );
    }
}
