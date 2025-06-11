package com.example.ElasticCommerce.domain.cart.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record AddCartItemRequest(
        @NotNull(message = "productId는 필수값입니다.")
        @Positive(message = "productId는 양수여야 합니다.")
        Long productId,

        @NotBlank(message = "productName은 빈 문자열일 수 없습니다.")
        @Size(max = 100, message = "productName은 최대 {max}자까지 입력 가능합니다.")
        String productName,

        @NotNull(message = "unitPrice는 필수값입니다.")
        @DecimalMin(value = "0.01", inclusive = true, message = "unitPrice는 0보다 커야 합니다.")
        BigDecimal unitPrice,

        @NotNull(message = "quantity는 필수값입니다.")
        @Min(value = 1, message = "quantity는 최소 {value} 이상이어야 합니다.")
        Integer quantity
) {}
