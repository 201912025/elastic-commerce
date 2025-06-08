package com.example.ElasticCommerce.domain.coupon.dto;

import com.example.ElasticCommerce.domain.coupon.entity.UserCoupon;
import java.time.LocalDateTime;

public record UserCouponDto(
        Long id,
        Long userId,
        String couponCode,
        boolean used,
        LocalDateTime issuedAt,
        LocalDateTime expirationDate,
        String discountType,
        Long discountValue,
        Long minimumOrderAmount
) {
    public static UserCouponDto from(UserCoupon uc) {
        return new UserCouponDto(
                uc.getUserCouponId(),
                uc.getUser().getUserId(),
                uc.getCoupon().getCouponCode(),
                uc.isUsed(),
                uc.getCreatedAt(),
                uc.getCoupon().getExpirationDate(),
                uc.getCoupon().getDiscountType().name(),
                uc.getCoupon().getDiscountValue(),
                uc.getCoupon().getMinimumOrderAmount()
        );
    }
}