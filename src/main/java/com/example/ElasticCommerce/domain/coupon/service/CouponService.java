package com.example.ElasticCommerce.domain.coupon.service;

import com.example.ElasticCommerce.domain.coupon.dto.ApplyCouponRequest;
import com.example.ElasticCommerce.domain.coupon.dto.IssueCouponRequest;
import com.example.ElasticCommerce.domain.coupon.dto.IssueUserCouponRequest;
import com.example.ElasticCommerce.domain.coupon.entity.Coupon;
import com.example.ElasticCommerce.domain.coupon.entity.UserCoupon;
import com.example.ElasticCommerce.domain.coupon.exception.CouponExceptionType;
import com.example.ElasticCommerce.domain.coupon.repository.CouponStockRepository;
import com.example.ElasticCommerce.domain.coupon.repository.CouponRepository;
import com.example.ElasticCommerce.domain.coupon.repository.UserCouponRepository;
import com.example.ElasticCommerce.domain.user.entity.User;
import com.example.ElasticCommerce.domain.user.exception.UserExceptionType;
import com.example.ElasticCommerce.domain.user.repository.UserRepository;
import com.example.ElasticCommerce.global.exception.type.BadRequestException;
import com.example.ElasticCommerce.global.exception.type.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponRepository couponRepository;
    private final UserCouponRepository userCouponRepository;
    private final UserRepository userRepository;
    private final CouponStockRepository couponStockRepository;
    private final Clock clock;

    @Transactional
    public Long issueCompanyCoupon(IssueCouponRequest request) {
        LocalDateTime now = LocalDateTime.now(clock);

        if (couponRepository.findByCouponCode(request.couponCode()).isPresent()) {
            throw new BadRequestException(CouponExceptionType.COUPON_APPLICATION_FAILED);
        }

        if (request.discountValue() == null || request.discountValue() <= 0 ||
                request.minimumOrderAmount() == null || request.minimumOrderAmount() < 0 ||
                request.expirationDate() == null || request.expirationDate().isBefore(now) ||
                request.quantity() == null || request.quantity() <= 0) {
            throw new BadRequestException(CouponExceptionType.COUPON_APPLICATION_FAILED);
        }

        Coupon coupon = Coupon.builder()
                              .couponCode(request.couponCode())
                              .discountType(request.discountType())
                              .discountValue(request.discountValue())
                              .minimumOrderAmount(request.minimumOrderAmount())
                              .expirationDate(request.expirationDate())
                              .quantity(request.quantity())
                              .build();

        couponRepository.save(coupon);
        couponStockRepository.setIfAbsent(request.couponCode(), request.quantity());
        return coupon.getCouponId();
    }

    @Transactional
    public void issueUserCoupon(IssueUserCouponRequest issueUserCouponRequest) {
        Long userId = issueUserCouponRequest.userId();
        String couponCode = issueUserCouponRequest.couponCode();
        LocalDateTime now = LocalDateTime.now(clock);

        // ─── 1) DB에서 Coupon 조회 (유효성 체크 용도) ───
        Coupon coupon = couponRepository.findByCouponCode(couponCode)
                                        .orElseThrow(() -> new NotFoundException(CouponExceptionType.COUPON_NOT_FOUND));

        // 2) 쿠폰 만료 여부 체크
        if (coupon.isExpired(now)) {
            throw new BadRequestException(CouponExceptionType.COUPON_EXPIRED);
        }

        // 3) 중복 발급 검사
        /* userCouponRepository.findByUserIdAndCouponCode(userId, couponCode)
                            .ifPresent(uc -> {
                                throw new BadRequestException(CouponExceptionType.COUPON_DUPLICATE_ISSUE);
                            });
                             */

        // 4) DB에서 User 조회 (없는 경우 Redis를 전혀 건드리지 않고 예외)
        User user = userRepository.findById(userId)
                                  .orElseThrow(() -> new NotFoundException(UserExceptionType.NOT_FOUND_USER));

        couponStockRepository.setIfAbsent(couponCode, coupon.getQuantity());

        // ─── 5) Redis DECR 실행 → 재고를 1 감소시키고 newStock을 얻어옴 ───
        //     (테스트 전, 혹은 쿠폰 생성 시에 couponStockRepository.setInitialStock(couponCode, initialQuantity) 로 세팅 완료)
        Long newStock = couponStockRepository.decrement(couponCode);

        // 6) newStock < 0 → 이미 재고가 없는 상태(= Redis 키가 0이었는데 DECR 됐으므로 -1)
        if (newStock < 0) {
            // “재고 부족” 처리, DECR로 –1 빠진 값을 다시 +1(=0)으로 복구
            couponStockRepository.increment(couponCode);
            throw new BadRequestException(CouponExceptionType.COUPON_OUT_OF_STOCK);
        }
        //    newStock ≥ 0이면 “발급 허용”

        try {
            // ─── 7) DB에서 원자적 UPDATE로 quantity를 1 감소 ───
            int updatedRows = couponRepository.decrementQuantity(couponCode);
            if (updatedRows != 1) {
                // DB 재고가 이미 0이었거나, 업데이트가 안 된 경우
                // Redis에서 방금 DECR한 값을 다시 INCR 해서 복구
                couponStockRepository.increment(couponCode);
                throw new BadRequestException(CouponExceptionType.COUPON_OUT_OF_STOCK);
            }

            // ─── 8) UserCoupon 에 발급 내역 저장 ───
            UserCoupon userCoupon = UserCoupon.builder()
                                              .coupon(coupon)
                                              .user(user)
                                              .build();
            userCouponRepository.save(userCoupon);

        } catch (RuntimeException ex) {
            // ─── 9) DB 저장 중 예외 발생 시, Redis 재고를 복구 ───
            couponStockRepository.increment(couponCode);
            throw ex;
        }
    }

    @Transactional
    public Long applyCoupon(ApplyCouponRequest applyCouponRequest) {
        Long userId = applyCouponRequest.userId();
        String couponCode = applyCouponRequest.couponCode();
        Long orderAmount = applyCouponRequest.orderAmount();
        LocalDateTime now = LocalDateTime.now(clock);

        // 1) UserCoupon 조회: “해당 유저가 couponCode로 발급받은 이력이 있는지” 확인
        UserCoupon userCoupon = userCouponRepository
                .findByUserIdAndCouponCode(userId, couponCode)
                .orElseThrow(() -> new NotFoundException(CouponExceptionType.COUPON_NOT_FOUND));

        // 2) 이미 사용된 쿠폰인지 체크
        if (userCoupon.isUsed()) {
            throw new BadRequestException(CouponExceptionType.COUPON_APPLICATION_FAILED);
        }

        Coupon coupon = userCoupon.getCoupon();

        // 3) 쿠폰 만료 여부 체크
        if (coupon.isExpired(now)) {
            throw new BadRequestException(CouponExceptionType.COUPON_EXPIRED);
        }

        if (orderAmount < coupon.getMinimumOrderAmount()) {
            throw new BadRequestException(CouponExceptionType.COUPON_MINIMUM_AMOUNT_NOT_MET);
        }

        // 6) UserCoupon을 사용 처리 (used = true)
        userCoupon.apply();
        userCouponRepository.save(userCoupon);

        // 7) 할인 금액 계산 후 리턴 (정액/정률 모두 지원)
        return coupon.calculateDiscountAmount(orderAmount);
    }
}
