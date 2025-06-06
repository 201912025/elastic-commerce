package com.example.ElasticCommerce.domain.coupon.dto;

import com.example.ElasticCommerce.domain.coupon.entity.Coupon;
import com.example.ElasticCommerce.domain.coupon.entity.DiscountType;

import java.time.LocalDateTime;

public record CouponKafkaDTO(
        Long userId,
        Long couponId,
        String couponCode,
        DiscountType discountType,
        Long discountValue,
        Long minimumOrderAmount,
        LocalDateTime expirationDate,
        Integer quantity,
        LocalDateTime requestedAt
) {
    public static CouponKafkaDTO from(Long userId, Coupon coupon, LocalDateTime requestedAt) {
        return new CouponKafkaDTO(
                userId,
                coupon.getCouponId(),
                coupon.getCouponCode(),
                coupon.getDiscountType(),
                coupon.getDiscountValue(),
                coupon.getMinimumOrderAmount(),
                coupon.getExpirationDate(),
                coupon.getQuantity(),
                requestedAt
        );
    }
}
