package com.example.ElasticCommerce.domain.coupon.dto.request;

import com.example.ElasticCommerce.domain.coupon.entity.Coupon;
import com.example.ElasticCommerce.domain.coupon.entity.DiscountType;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

public record IssueCouponRequest(
        @NotBlank(message = "couponCode는 빈 문자열일 수 없습니다.")
        @Size(max = 50, message = "couponCode는 최대 {max}자까지 입력 가능합니다.")
        String couponCode,

        @NotNull(message = "discountType은 필수값입니다.")
        DiscountType discountType,

        @NotNull(message = "discountValue는 필수값입니다.")
        @Positive(message = "discountValue는 양수여야 합니다.")
        Long discountValue,

        @NotNull(message = "minimumOrderAmount는 필수값입니다.")
        @PositiveOrZero(message = "minimumOrderAmount는 음수일 수 없습니다.")
        Long minimumOrderAmount,

        @NotNull(message = "expirationDate는 필수값입니다.")
        @Future(message = "expirationDate는 현재 시각 이후여야 합니다.")
        LocalDateTime expirationDate,

        @NotNull(message = "quantity는 필수값입니다.")
        @Min(value = 1, message = "quantity는 최소 {value} 이상이어야 합니다.")
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
