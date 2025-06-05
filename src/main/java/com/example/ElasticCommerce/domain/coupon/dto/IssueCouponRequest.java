package com.example.ElasticCommerce.domain.coupon.dto;


import com.example.ElasticCommerce.domain.coupon.entity.Coupon;
import com.example.ElasticCommerce.domain.coupon.entity.DiscountType;

import java.time.LocalDateTime;

public record IssueCouponRequest(
        String couponCode,
        DiscountType discountType,
        Long discountValue,
        Long minimumOrderAmount,
        LocalDateTime expirationDate,
        Integer quantity
) {

    public static IssueCouponRequest from(Coupon coupon) {
        return new IssueCouponRequest(
                coupon.getCouponCode(),
                coupon.getDiscountType(),
                coupon.getDiscountValue(),
                coupon.getMinimumOrderAmount(),
                coupon.getExpirationDate(),
                coupon.getQuantity()
        );
    }
}
