package com.example.ElasticCommerce.domain.coupon.service;

import com.example.ElasticCommerce.domain.coupon.dto.ApplyCouponRequest;
import com.example.ElasticCommerce.domain.coupon.dto.CouponKafkaDTO;
import com.example.ElasticCommerce.domain.coupon.dto.IssueCouponRequest;
import com.example.ElasticCommerce.domain.coupon.dto.IssueUserCouponRequest;
import com.example.ElasticCommerce.domain.coupon.entity.Coupon;
import com.example.ElasticCommerce.domain.coupon.entity.UserCoupon;
import com.example.ElasticCommerce.domain.coupon.exception.CouponExceptionType;
import com.example.ElasticCommerce.domain.coupon.repository.CouponRepository;
import com.example.ElasticCommerce.domain.coupon.repository.CouponStockRepository;
import com.example.ElasticCommerce.domain.coupon.repository.UserCouponRepository;
import com.example.ElasticCommerce.domain.coupon.service.kafka.CouponKafkaProducerService;
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
    private final CouponKafkaProducerService couponKafkaProducerService;
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
    public void issueUserCoupon(IssueUserCouponRequest dto) {
        Long userId       = dto.userId();
        String couponCode = dto.couponCode();
        LocalDateTime now = LocalDateTime.now(clock);

        // 1) 락 걸어서 Coupon 조회
        Coupon coupon = couponRepository
                .findByCouponCodeForUpdate(couponCode)
                .orElseThrow(() -> new NotFoundException(CouponExceptionType.COUPON_NOT_FOUND));

        // 2) 만료 여부 검사
        if (coupon.isExpired(now)) {
            throw new BadRequestException(CouponExceptionType.COUPON_EXPIRED);
        }

        // 3) 중복 발급 검사
        userCouponRepository.findByUserIdAndCouponCode(userId, couponCode)
                            .ifPresent(uc -> { throw new BadRequestException(CouponExceptionType.COUPON_DUPLICATE_ISSUE); });

        // 4) User 조회
        User user = userRepository.findById(userId)
                                  .orElseThrow(() -> new NotFoundException(UserExceptionType.NOT_FOUND_USER));

        // 5) DB 원자적 재고 차감
        int updated = couponRepository.decrementIfAvailable(coupon.getCouponId());
        if (updated != 1) {
            throw new BadRequestException(CouponExceptionType.COUPON_OUT_OF_STOCK);
        }

        // 6) 발급 기록 저장
        UserCoupon userCoupon = UserCoupon.builder()
                                          .user(user)
                                          .coupon(coupon)
                                          .build();
        userCouponRepository.save(userCoupon);
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

    @Transactional
    public void issueUserCouponInsertOnly(IssueUserCouponRequest request) {
        Long userId = request.userId();
        String couponCode = request.couponCode();
        LocalDateTime now = LocalDateTime.now(clock);

        // 1) DB에서 Coupon 조회 (유효성만 확인)
        Coupon coupon = couponRepository.findByCouponCode(couponCode)
                                        .orElseThrow(() -> new NotFoundException(CouponExceptionType.COUPON_NOT_FOUND));

        // 2) 쿠폰 만료 여부 체크
        if (coupon.isExpired(now)) {
            throw new BadRequestException(CouponExceptionType.COUPON_EXPIRED);
        }

        userRepository.findById(userId)
                      .orElseThrow(() -> new NotFoundException(UserExceptionType.NOT_FOUND_USER));

        CouponKafkaDTO kafkaDTO = CouponKafkaDTO.from(userId, coupon, now);
        couponKafkaProducerService.sendCoupon("coupon-topic", kafkaDTO);
    }
}
