package com.example.ElasticCommerce.domain.coupon.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record IssueUserCouponRequest(
        @NotNull(message = "userId는 필수값입니다.")
        Long userId,

        @NotBlank(message = "couponCode는 빈 문자열일 수 없습니다.")
        @Size(max = 50, message = "couponCode는 최대 {max}자까지 입력 가능합니다.")
        String couponCode
) {
    public static IssueUserCouponRequest from(Long userId, String couponCode) {
        return new IssueUserCouponRequest(userId, couponCode);
    }
}
