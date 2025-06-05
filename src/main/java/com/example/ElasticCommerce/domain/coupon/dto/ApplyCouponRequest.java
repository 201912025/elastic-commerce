package com.example.ElasticCommerce.domain.coupon.dto;

public record ApplyCouponRequest(
        Long userId,
        String couponCode,
        Long orderAmount
) {
    public static ApplyCouponRequest from(Long userId, String couponCode, Long orderAmount) {
        return new ApplyCouponRequest(userId, couponCode, orderAmount);
    }
}
