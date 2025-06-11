package com.example.ElasticCommerce.domain.coupon.controller;

import com.example.ElasticCommerce.domain.coupon.dto.request.ApplyCouponRequest;
import com.example.ElasticCommerce.domain.coupon.dto.request.IssueCouponRequest;
import com.example.ElasticCommerce.domain.coupon.dto.request.IssueUserCouponRequest;
import com.example.ElasticCommerce.domain.coupon.dto.response.CompanyCouponDto;
import com.example.ElasticCommerce.domain.coupon.dto.response.UserCouponDto;
import com.example.ElasticCommerce.domain.coupon.service.CouponService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    @PostMapping("/company")
    public ResponseEntity<Long> createCompanyCoupon(@Valid @RequestBody IssueCouponRequest request) {
        Long couponId = couponService.issueCompanyCoupon(request);
        return ResponseEntity.ok(couponId);
    }

    @PostMapping("/issue")
    public ResponseEntity<Void> issueUserCoupon(@Valid @RequestBody IssueUserCouponRequest dto) {
        couponService.issueUserCoupon(dto);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/apply")
    public ResponseEntity<Long> applyCoupon(@Valid @RequestBody ApplyCouponRequest dto) {
        Long discountAmount = couponService.applyCoupon(dto);
        return ResponseEntity.ok(discountAmount);
    }

    @GetMapping("/company")
    public ResponseEntity<Page<CompanyCouponDto>> getAllCompanyCoupons(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<CompanyCouponDto> result = couponService.getAllCompanyCoupons(page, size);
        return ResponseEntity.ok(result);
    }


    @GetMapping("/company/{couponId}")
    public ResponseEntity<CompanyCouponDto> getCompanyCoupon(@PathVariable Long couponId) {
        CompanyCouponDto dto = couponService.getCompanyCoupon(couponId);
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<UserCouponDto>> getUserCoupons(@PathVariable Long userId) {
        List<UserCouponDto> page = couponService.getUserCoupons(userId);
        return ResponseEntity.ok(page);
    }

    @PostMapping("/issue/no-redis")
    public void issueWithoutRedis(@RequestBody IssueUserCouponRequest dto) {
        couponService.issueUserCouponInsertOnly(dto);
    }
}
