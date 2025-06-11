package com.example.ElasticCommerce.domain.cart.dto;

import jakarta.validation.constraints.*;

public record UpdateQuantityRequest(
        @NotNull(message = "quantity는 필수값입니다.")
        Integer quantity
) {}
