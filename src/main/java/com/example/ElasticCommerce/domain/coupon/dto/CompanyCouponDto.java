package com.example.ElasticCommerce.domain.coupon.dto;

import com.example.ElasticCommerce.domain.coupon.entity.Coupon;
import java.time.LocalDateTime;

public record CompanyCouponDto(
        Long couponId,
        String couponCode,
        String discountType,
        Long discountValue,
        Long minimumOrderAmount,
        LocalDateTime expirationDate,
        Integer quantity
) {
    public static CompanyCouponDto from(Coupon c) {
        return new CompanyCouponDto(
                c.getCouponId(),
                c.getCouponCode(),
                c.getDiscountType().name(),
                c.getDiscountValue(),
                c.getMinimumOrderAmount(),
                c.getExpirationDate(),
                c.getQuantity()
        );
    }
}

