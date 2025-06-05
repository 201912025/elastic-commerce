package com.example.ElasticCommerce.domain.coupon.service;

import com.example.ElasticCommerce.domain.coupon.dto.ApplyCouponRequest;
import com.example.ElasticCommerce.domain.coupon.dto.IssueCouponRequest;
import com.example.ElasticCommerce.domain.coupon.dto.IssueUserCouponRequest;
import com.example.ElasticCommerce.domain.coupon.entity.Coupon;
import com.example.ElasticCommerce.domain.coupon.entity.DiscountType;
import com.example.ElasticCommerce.domain.coupon.entity.UserCoupon;
import com.example.ElasticCommerce.domain.coupon.repository.CouponStockRepository;
import com.example.ElasticCommerce.domain.coupon.repository.CouponRepository;
import com.example.ElasticCommerce.domain.coupon.repository.UserCouponRepository;
import com.example.ElasticCommerce.domain.user.entity.User;
import com.example.ElasticCommerce.domain.user.repository.UserRepository;
import com.example.ElasticCommerce.global.exception.type.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertAll;

@ActiveProfiles("test")
@DataJpaTest
@Import({
        CouponService.class,
        CouponServiceTest.ClockTestConfig.class,
        EmbeddedRedisConfig.class,
        CouponStockRepository.class
})
class CouponServiceTest {

    @Autowired private CouponService couponService;
    @Autowired private CouponRepository couponRepository;
    @Autowired private UserCouponRepository userCouponRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private CouponStockRepository couponStockRepository;
    @Autowired private Clock clock;

    private User testUser;

    @BeforeEach
    void setUp() {
        userCouponRepository.deleteAll();
        couponRepository.deleteAll();
        userRepository.deleteAll();

        testUser = userRepository.save(
                User.builder()
                    .username("testUser")
                    .email("test@example.com")
                    .password("password123")
                    .role("USER")
                    .birthDay("1990-01-01")
                    .build()
        );
    }

    @TestConfiguration
    static class ClockTestConfig {
        /** 테스트 시 고정된 시점을 반환하는 Clock 빈을 등록합니다. */
        @Bean
        public Clock clock() {
            Instant fixedInstant = LocalDateTime
                    .of(2025, 6, 5, 0, 0)
                    .atZone(ZoneId.of("Asia/Seoul"))
                    .toInstant();
            return Clock.fixed(fixedInstant, ZoneId.of("Asia/Seoul"));
        }
    }

    @Test
    @DisplayName("회사용 쿠폰 발급 성공")
    @Transactional
    void testIssueCompanyCoupon_Success() {
        LocalDateTime now = LocalDateTime.now(clock);
        IssueCouponRequest req = new IssueCouponRequest(
                "WELCOME100",
                DiscountType.FIXED,
                100L,
                1000L,
                now.plusDays(7),
                50
        );

        Long couponId = couponService.issueCompanyCoupon(req);

        Optional<Coupon> opt = couponRepository.findById(couponId);
        assertThat(opt).isPresent();

        Coupon saved = opt.get();
        assertThat(saved.getCouponCode()).isEqualTo("WELCOME100");
        assertThat(saved.getDiscountType()).isEqualTo(DiscountType.FIXED);
        assertThat(saved.getDiscountValue()).isEqualTo(100L);
        assertThat(saved.getMinimumOrderAmount()).isEqualTo(1000L);
        assertThat(saved.getExpirationDate()).isEqualTo(now.plusDays(7));
        assertThat(saved.hasStock()).isTrue();
        assertThat(saved.getQuantity()).isEqualTo(50);
    }

    @Test
    @DisplayName("회사용 쿠폰 발급 실패 - 중복 코드")
    @Transactional
    void testIssueCompanyCoupon_Fail_DuplicateCode() {
        LocalDateTime now = LocalDateTime.now(clock);
        // 이미 존재하는 쿠폰 저장
        Coupon existing = Coupon.builder()
                                .couponCode("DUPLICATE")
                                .discountType(DiscountType.FIXED)
                                .discountValue(100L)
                                .minimumOrderAmount(0L)
                                .expirationDate(now.plusDays(5))
                                .quantity(10)
                                .build();
        couponRepository.save(existing);

        IssueCouponRequest req = new IssueCouponRequest(
                "DUPLICATE",
                DiscountType.PERCENT,
                10L,
                500L,
                now.plusDays(2),
                20
        );

        assertThatThrownBy(() -> couponService.issueCompanyCoupon(req))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("사용자에게 쿠폰 발급 성공")
    @Transactional
    void testIssueUserCoupon_Success() {
        LocalDateTime now = LocalDateTime.now(clock);
        Coupon coupon = Coupon.builder()
                              .couponCode("USER50")
                              .discountType(DiscountType.FIXED)
                              .discountValue(50L)
                              .minimumOrderAmount(200L)
                              .expirationDate(now.plusDays(3))
                              .quantity(5)
                              .build();
        couponRepository.save(coupon);

        // 변경: record 기반 DTO를 생성하여 서비스 호출
        IssueUserCouponRequest dto = new IssueUserCouponRequest(testUser.getUserId(), "USER50");
        couponService.issueUserCoupon(dto);

        Optional<UserCoupon> optUc = userCouponRepository.findByUserIdAndCouponCode(
                testUser.getUserId(), "USER50"
        );
        assertThat(optUc).isPresent();
        UserCoupon uc = optUc.get();
        assertThat(uc.getCoupon().getCouponCode()).isEqualTo("USER50");
        assertThat(uc.getUser().getUserId()).isEqualTo(testUser.getUserId());
        assertThat(uc.isUsed()).isFalse();

        Coupon updated = couponRepository.findById(coupon.getCouponId()).orElseThrow();
        assertThat(updated.getQuantity()).isEqualTo(4);
    }

    @Test
    @DisplayName("사용자에게 쿠폰 발급 실패 - 쿠폰 만료")
    @Transactional
    void testIssueUserCoupon_Fail_Expired() {
        LocalDateTime now = LocalDateTime.now(clock);
        Coupon coupon = Coupon.builder()
                              .couponCode("EXPIRED")
                              .discountType(DiscountType.FIXED)
                              .discountValue(100L)
                              .minimumOrderAmount(0L)
                              .expirationDate(now.minusDays(1))
                              .quantity(10)
                              .build();
        couponRepository.save(coupon);

        IssueUserCouponRequest dto = new IssueUserCouponRequest(testUser.getUserId(), "EXPIRED");
        assertThatThrownBy(() -> couponService.issueUserCoupon(dto))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("사용자에게 쿠폰 발급 실패 - 재고 없음")
    @Transactional
    void testIssueUserCoupon_Fail_OutOfStock() {
        LocalDateTime now = LocalDateTime.now(clock);
        Coupon coupon = Coupon.builder()
                              .couponCode("ZERO_STOCK")
                              .discountType(DiscountType.FIXED)
                              .discountValue(100L)
                              .minimumOrderAmount(0L)
                              .expirationDate(now.plusDays(1))
                              .quantity(0)
                              .build();
        couponRepository.save(coupon);

        IssueUserCouponRequest dto = new IssueUserCouponRequest(testUser.getUserId(), "ZERO_STOCK");
        assertThatThrownBy(() -> couponService.issueUserCoupon(dto))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("쿠폰 적용 성공 - 정액 할인")
    @Transactional
    void testApplyCoupon_Success_Fixed() {
        LocalDateTime now = LocalDateTime.now(clock);
        Coupon coupon = Coupon.builder()
                              .couponCode("FIXED100")
                              .discountType(DiscountType.FIXED)
                              .discountValue(100L)
                              .minimumOrderAmount(500L)
                              .expirationDate(now.plusDays(2))
                              .quantity(10)
                              .build();
        couponRepository.save(coupon);

        // 먼저 사용자에게 쿠폰 발급
        IssueUserCouponRequest issueDto = new IssueUserCouponRequest(testUser.getUserId(), "FIXED100");
        couponService.issueUserCoupon(issueDto);

        // 변경: ApplyCouponRequest record 생성하여 호출
        ApplyCouponRequest applyDto = new ApplyCouponRequest(testUser.getUserId(), "FIXED100", 1000L);
        Long discountAmount = couponService.applyCoupon(applyDto);
        assertThat(discountAmount).isEqualTo(100L);

        UserCoupon uc = userCouponRepository.findByUserIdAndCouponCode(testUser.getUserId(), "FIXED100")
                                            .orElseThrow();
        assertThat(uc.isUsed()).isTrue();
    }

    @Test
    @DisplayName("쿠폰 적용 실패 - 이미 사용된 쿠폰")
    @Transactional
    void testApplyCoupon_Fail_AlreadyUsed() {
        LocalDateTime now = LocalDateTime.now(clock);
        Coupon coupon = Coupon.builder()
                              .couponCode("USEDCOUPON")
                              .discountType(DiscountType.FIXED)
                              .discountValue(50L)
                              .minimumOrderAmount(100L)
                              .expirationDate(now.plusDays(1))
                              .quantity(5)
                              .build();
        couponRepository.save(coupon);

        IssueUserCouponRequest issueDto = new IssueUserCouponRequest(testUser.getUserId(), "USEDCOUPON");
        couponService.issueUserCoupon(issueDto);

        ApplyCouponRequest applyDto1 = new ApplyCouponRequest(testUser.getUserId(), "USEDCOUPON", 200L);
        couponService.applyCoupon(applyDto1);

        ApplyCouponRequest applyDto2 = new ApplyCouponRequest(testUser.getUserId(), "USEDCOUPON", 200L);
        assertThatThrownBy(() -> couponService.applyCoupon(applyDto2))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("쿠폰 적용 실패 - 최소 금액 미달")
    @Transactional
    void testApplyCoupon_Fail_MinimumAmountNotMet() {
        LocalDateTime now = LocalDateTime.now(clock);
        Coupon coupon = Coupon.builder()
                              .couponCode("MINIMUM500")
                              .discountType(DiscountType.FIXED)
                              .discountValue(50L)
                              .minimumOrderAmount(500L)
                              .expirationDate(now.plusDays(1))
                              .quantity(3)
                              .build();
        couponRepository.save(coupon);

        IssueUserCouponRequest issueDto = new IssueUserCouponRequest(testUser.getUserId(), "MINIMUM500");
        couponService.issueUserCoupon(issueDto);

        ApplyCouponRequest applyDto = new ApplyCouponRequest(testUser.getUserId(), "MINIMUM500", 400L);
        assertThatThrownBy(() -> couponService.applyCoupon(applyDto))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("동시성 테스트: 재고 100개 쿠폰을 1000명이 동시에 발급 시도")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @DirtiesContext
    void testConcurrentIssueUserCoupon() throws InterruptedException {
        // 1) 미리 쿠폰을 DB에 저장 → saveAndFlush()로 즉시 커밋
        LocalDateTime now = LocalDateTime.now(clock);
        Coupon coupon = Coupon.builder()
                              .couponCode("CONC100")
                              .discountType(DiscountType.FIXED)
                              .discountValue(10L)
                              .minimumOrderAmount(0L)
                              .expirationDate(now.plusDays(1))
                              .quantity(100)
                              .build();
        couponRepository.saveAndFlush(coupon);

        // 2) 1000명의 유저를 미리 저장 → save() 후 flush()로 커밋
        for (int i = 0; i < 1000; i++) {
            User u = User.builder()
                         .username("user" + i)
                         .email("user" + i + "@example.com")
                         .password("pw" + i)
                         .role("USER")
                         .birthDay("1990-01-01")
                         .build();
            userRepository.save(u);
        }
        userRepository.flush();

        // 3) 1000개의 요청을 동시에 실행할 쓰레드 풀과 래치 준비
        int threadCount = 1000;
        ExecutorService executor = Executors.newFixedThreadPool(32);
        CountDownLatch latch = new CountDownLatch(threadCount);

        // 4) 각 쓰레드에서 record 기반 DTO를 생성하여 서비스 호출
        for (long uid = 1; uid <= threadCount; uid++) {
            long userId = uid;
            executor.execute(() -> {
                try {
                    IssueUserCouponRequest dto = new IssueUserCouponRequest(userId, "CONC100");
                    couponService.issueUserCoupon(dto);
                } finally {
                    latch.countDown();
                }
            });
        }

        // 5) 모든 쓰레드가 종료될 때까지 대기
        latch.await();

        assertAll(
                () -> {
                    // 실제 DB 상에서 쿠폰 재고가 0인지 확인
                    Coupon finalCoupon = couponRepository.findByCouponCode("CONC100")
                                                         .orElseThrow();
                    assertThat(finalCoupon.getQuantity()).isEqualTo(0);
                },
                () -> {
                    // UserCoupon 테이블에는 100개 레코드만 존재
                    long savedUserCouponCount = userCouponRepository.count();
                    assertThat(savedUserCouponCount).isEqualTo(100);
                }
        );
    }
}
