package com.example.ElasticCommerce.domain.coupon.controller;

import com.example.ElasticCommerce.domain.coupon.dto.ApplyCouponRequest;
import com.example.ElasticCommerce.domain.coupon.dto.IssueCouponRequest;
import com.example.ElasticCommerce.domain.coupon.dto.IssueUserCouponRequest;
import com.example.ElasticCommerce.domain.coupon.service.CouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    @PostMapping("/company")
    public ResponseEntity<Long> createCompanyCoupon(@RequestBody IssueCouponRequest request) {
        Long couponId = couponService.issueCompanyCoupon(request);
        return ResponseEntity.ok(couponId);
    }

    @PostMapping("/issue")
    public ResponseEntity<Void> issueUserCoupon(@RequestBody IssueUserCouponRequest dto) {
        couponService.issueUserCoupon(dto);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/apply")
    public ResponseEntity<Long> applyCoupon(@RequestBody ApplyCouponRequest dto) {
        Long discountAmount = couponService.applyCoupon(dto);
        return ResponseEntity.ok(discountAmount);
    }

    @PostMapping("/issue/no-redis")
    public void issueWithoutRedis(@RequestBody IssueUserCouponRequest dto) {
        // 기존 applyUserCoupon 로직에서 Redis 관련 코드만 빼고 DB 저장 로직만 그대로 호출
        couponService.issueUserCouponInsertOnly(dto);
    }
}
