package com.example.ElasticCommerce.domain.coupon.dto;

public record IssueUserCouponRequest(
        Long userId,
        String couponCode
) {
    public static IssueUserCouponRequest from(Long userId, String couponCode) {
        return new IssueUserCouponRequest(userId, couponCode);
    }
}
