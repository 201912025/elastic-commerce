package com.example.ElasticCommerce.domain.payment.dto.request;

public record PaymentRequest(
        String paymentMethod,
        long amount
) {}
