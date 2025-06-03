package com.example.ElasticCommerce.domain.product.dto.request;

import jakarta.validation.constraints.*;

public record StockUpdateRequestDTO(
        @NotNull(message = "재고 수량은 필수입니다.")
        @Min(value = 0, message = "재고 수량은 0 이상이어야 합니다.")
        @Max(value = 10000, message = "재고 수량은 최대 10,000까지 허용됩니다.")
        Integer stockQuantity
) { }
