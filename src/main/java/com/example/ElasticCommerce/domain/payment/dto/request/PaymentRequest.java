package com.example.ElasticCommerce.domain.payment.dto.request;

import jakarta.validation.constraints.*;

public record PaymentRequest(
        @NotBlank(message = "paymentMethod는 빈 문자열일 수 없습니다.")
        @Size(max = 50, message = "paymentMethod는 최대 {max}자까지 입력 가능합니다.")
        String paymentMethod,

        @Positive(message = "amount는 양수여야 합니다.")
        long amount
) {}
