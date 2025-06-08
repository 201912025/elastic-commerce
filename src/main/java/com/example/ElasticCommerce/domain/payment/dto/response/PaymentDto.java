package com.example.ElasticCommerce.domain.payment.dto.response;

import com.example.ElasticCommerce.domain.payment.entity.Payment;
import java.time.LocalDateTime;

public record PaymentDto(
        Long paymentId,
        Long orderId,
        String method,
        long amount,
        String status,
        LocalDateTime createdAt
) {
    public static PaymentDto from(Payment p) {
        return new PaymentDto(
                p.getId(),
                p.getOrder().getId(),
                p.getMethod(),
                p.getAmount(),
                p.getStatus().name(),
                p.getCreatedAt()
        );
    }
}
