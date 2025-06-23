package com.example.ElasticCommerce.domain.notification.dto;

import jakarta.validation.constraints.NotNull;

public record NotificationRequest(
        @NotNull Long   targetId,
        @NotNull String eventType,
        String          userEmail,
        @NotNull Long   totalPrice
) {}
