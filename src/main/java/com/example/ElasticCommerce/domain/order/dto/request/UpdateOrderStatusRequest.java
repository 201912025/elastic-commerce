package com.example.ElasticCommerce.domain.order.dto.request;

import jakarta.validation.constraints.*;

public record UpdateOrderStatusRequest(
        @NotBlank(message = "newStatus는 빈 문자열일 수 없습니다.")
        @Pattern(
                regexp = "PENDING|PAID|SHIPPED|DELIVERED|CANCELLED",
                message = "newStatus는 PENDING, PAID, SHIPPED, DELIVERED, CANCELLED 중 하나여야 합니다."
        )
        String newStatus
) {}
