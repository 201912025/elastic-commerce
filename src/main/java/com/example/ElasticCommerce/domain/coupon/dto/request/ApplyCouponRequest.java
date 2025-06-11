package com.example.ElasticCommerce.domain.coupon.dto.request;

import jakarta.validation.constraints.*;

public record ApplyCouponRequest(
        @NotNull(message = "userId는 필수값입니다.")
        Long userId,

        @NotBlank(message = "couponCode는 빈 문자열일 수 없습니다.")
        @Size(max = 50, message = "couponCode는 최대 {max}자까지 입력 가능합니다.")
        String couponCode,

        @NotNull(message = "orderAmount는 필수값입니다.")
        @Positive(message = "orderAmount는 양수여야 합니다.")
        Long orderAmount
) {
    public static ApplyCouponRequest from(Long userId, String couponCode, Long orderAmount) {
        return new ApplyCouponRequest(userId, couponCode, orderAmount);
    }
}
