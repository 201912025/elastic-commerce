package com.example.ElasticCommerce.domain.coupon.service;

import com.example.ElasticCommerce.domain.coupon.dto.ApplyCouponRequest;
import com.example.ElasticCommerce.domain.coupon.dto.CouponKafkaDTO;
import com.example.ElasticCommerce.domain.coupon.dto.IssueCouponRequest;
import com.example.ElasticCommerce.domain.coupon.dto.IssueUserCouponRequest;
import com.example.ElasticCommerce.domain.coupon.entity.Coupon;
import com.example.ElasticCommerce.domain.coupon.entity.UserCoupon;
import com.example.ElasticCommerce.domain.coupon.repository.CouponStockRepository;
import com.example.ElasticCommerce.domain.coupon.repository.CouponRepository;
import com.example.ElasticCommerce.domain.coupon.repository.UserCouponRepository;
import com.example.ElasticCommerce.domain.coupon.service.kafka.CouponKafkaConsumerService;
import com.example.ElasticCommerce.domain.coupon.service.kafka.CouponKafkaProducerService;
import com.example.ElasticCommerce.domain.user.entity.User;
import com.example.ElasticCommerce.domain.user.repository.UserRepository;
import com.example.ElasticCommerce.global.exception.type.BadRequestException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.*;

@ActiveProfiles("test")
@DataJpaTest
@Import({
        CouponService.class,
        CouponKafkaConsumerService.class,
        CouponServiceTest.ClockTestConfig.class,
        CouponServiceTest.ObjectMapperConfig.class,
        EmbeddedRedisConfig.class,
        CouponStockRepository.class
})
class CouponServiceTest {

    // 카프카 프로듀서는 실제 브로커 없이 “더미”로 등록
    @MockitoBean
    private CouponKafkaProducerService couponKafkaProducerService;

    // (★ CouponStockRepository는 실제 Embedded Redis 빈을 그대로 사용합니다! ★)
    @Autowired private CouponStockRepository couponStockRepository;

    @Autowired private CouponService couponService;
    @Autowired private CouponKafkaConsumerService couponKafkaConsumerService;
    @Autowired private CouponRepository couponRepository;
    @Autowired private UserCouponRepository userCouponRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private Clock clock;
    @Autowired private ObjectMapper objectMapper;

    private User testUser;

    @BeforeEach
    void setUp() {
        // DB 초기화
        userCouponRepository.deleteAll();
        couponRepository.deleteAll();
        userRepository.deleteAll();

        // Redis 초기화
        couponStockRepository.deleteAllKeys();

        // 테스트용 유저 생성
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

    // ──────── Clock 고정 빈 ────────
    @TestConfiguration
    static class ClockTestConfig {
        @Bean
        public Clock clock() {
            // 2025-06-05 00:00 (Asia/Seoul 기준) 고정
            Instant fixedInstant = LocalDateTime
                    .of(2025, 6, 5, 0, 0)
                    .atZone(ZoneId.of("Asia/Seoul"))
                    .toInstant();
            return Clock.fixed(fixedInstant, ZoneId.of("Asia/Seoul"));
        }
    }

    // ──────── ObjectMapper 빈 ────────
    @TestConfiguration
    static class ObjectMapperConfig {
        @Bean
        public ObjectMapper objectMapper() {
            ObjectMapper mapper = new ObjectMapper();
            // LocalDateTime 직렬화/역직렬화용 모듈
            mapper.registerModule(new JavaTimeModule());
            // 타임스탬프로 쓰지 않도록 설정
            mapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            return mapper;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // 1) 회사용 쿠폰 발급 테스트 (issueCompanyCoupon) → 기존 그대로
    // ─────────────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("회사용 쿠폰 발급 성공")
    @Transactional
    void testIssueCompanyCoupon_Success() {
        LocalDateTime now = LocalDateTime.now(clock);
        IssueCouponRequest req = new IssueCouponRequest(
                "WELCOME100",
                com.example.ElasticCommerce.domain.coupon.entity.DiscountType.FIXED,
                100L,
                1000L,
                now.plusDays(7),
                50
        );

        Long couponId = couponService.issueCompanyCoupon(req);

        Coupon saved = couponRepository.findById(couponId).orElseThrow();
        assertThat(saved.getCouponCode()).isEqualTo("WELCOME100");
        assertThat(saved.getDiscountType()).isEqualTo(com.example.ElasticCommerce.domain.coupon.entity.DiscountType.FIXED);
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
        Coupon existing = Coupon.builder()
                                .couponCode("DUPLICATE")
                                .discountType(com.example.ElasticCommerce.domain.coupon.entity.DiscountType.FIXED)
                                .discountValue(100L)
                                .minimumOrderAmount(0L)
                                .expirationDate(now.plusDays(5))
                                .quantity(10)
                                .build();
        couponRepository.save(existing);

        IssueCouponRequest req = new IssueCouponRequest(
                "DUPLICATE",
                com.example.ElasticCommerce.domain.coupon.entity.DiscountType.PERCENT,
                10L,
                500L,
                now.plusDays(2),
                20
        );

        assertThatThrownBy(() -> couponService.issueCompanyCoupon(req))
                .isInstanceOf(BadRequestException.class);
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // 2) 사용자에게 쿠폰 발급 테스트 (issueUserCoupon → Redis 선점 → Consumer 검증)
    // ─────────────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("사용자에게 쿠폰 발급 요청 → Redis 선점 → 컨슈머 로직 직접 호출 → DB 저장 및 quantity 감소")
    @Transactional
    void testIssueUserCoupon_Success() throws Exception {
        LocalDateTime now = LocalDateTime.now(clock);

        // (1) DB에 쿠폰 생성 (quantity = 5)
        Coupon coupon = Coupon.builder()
                              .couponCode("USER50")
                              .discountType(com.example.ElasticCommerce.domain.coupon.entity.DiscountType.FIXED)
                              .discountValue(50L)
                              .minimumOrderAmount(200L)
                              .expirationDate(now.plusDays(3))
                              .quantity(5)
                              .build();
        couponRepository.saveAndFlush(coupon);

        // (2) Redis에 초기 재고 5 세팅
        couponStockRepository.setInitialStock("USER50", 5);

        // (3) 서비스 호출 → Redis 선점만 수행(5 → 4), DB에는 아직 UserCoupon 없음
        IssueUserCouponRequest dto = new IssueUserCouponRequest(testUser.getUserId(), "USER50");
        couponService.issueUserCoupon(dto);

        // DB에는 아직 UserCoupon이 없고, coupon.quantity도 5 그대로
        assertThat(userCouponRepository.findByUserIdAndCouponCode(testUser.getUserId(), "USER50")).isEmpty();
        Coupon unchanged = couponRepository.findById(coupon.getCouponId()).orElseThrow();
        assertThat(unchanged.getQuantity()).isEqualTo(5);

        // Redis 재고가 4로 감소했는지 확인
        Long redisStock = couponStockRepository.getStock("USER50");
        assertThat(redisStock).isEqualTo(4L);

        // (4) “Kafka 브로커를 통해 메시지 전달”을 흉내 내기 위해, DTO를 JSON 직렬화 후 컨슈머 호출
        Coupon savedCoupon = couponRepository.findByCouponCode("USER50").orElseThrow();
        CouponKafkaDTO kafkaDto = CouponKafkaDTO.from(testUser.getUserId(), savedCoupon, now);
        String jsonMessage = objectMapper.writeValueAsString(kafkaDto);

        Acknowledgment ackStub = new Acknowledgment() {
            @Override public void acknowledge() { /* no-op */ }
        };
        couponKafkaConsumerService.consumeCoupon(jsonMessage, ackStub);

        // (5) 이제 DB에 UserCoupon이 저장되고 coupon.quantity도 4로 감소했는지 검증
        UserCoupon uc = userCouponRepository.findByUserIdAndCouponCode(testUser.getUserId(), "USER50")
                                            .orElseThrow();
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
                              .discountType(com.example.ElasticCommerce.domain.coupon.entity.DiscountType.FIXED)
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
                              .discountType(com.example.ElasticCommerce.domain.coupon.entity.DiscountType.FIXED)
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

    // ─────────────────────────────────────────────────────────────────────────────
    // 3) 쿠폰 적용 테스트 (applyCoupon) → 기존 로직 그대로
    // ─────────────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("쿠폰 적용 성공 - 정액 할인")
    @Transactional
    void testApplyCoupon_Success_Fixed() throws Exception {
        LocalDateTime now = LocalDateTime.now(clock);

        // (1) DB에 쿠폰 생성
        Coupon coupon = Coupon.builder()
                              .couponCode("FIXED100")
                              .discountType(com.example.ElasticCommerce.domain.coupon.entity.DiscountType.FIXED)
                              .discountValue(100L)
                              .minimumOrderAmount(500L)
                              .expirationDate(now.plusDays(2))
                              .quantity(10)
                              .build();
        couponRepository.saveAndFlush(coupon);

        // (2) 발급 → 컨슈머
        IssueUserCouponRequest issueDto = new IssueUserCouponRequest(testUser.getUserId(), "FIXED100");
        couponService.issueUserCoupon(issueDto);

        Coupon savedCoupon = couponRepository.findByCouponCode("FIXED100").orElseThrow();
        CouponKafkaDTO kafkaDto = CouponKafkaDTO.from(testUser.getUserId(), savedCoupon, now);
        String message = objectMapper.writeValueAsString(kafkaDto);
        couponKafkaConsumerService.consumeCoupon(message, new Acknowledgment() {
            @Override public void acknowledge() { }
        });

        // (3) 쿠폰 적용
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
    void testApplyCoupon_Fail_AlreadyUsed() throws Exception {
        LocalDateTime now = LocalDateTime.now(clock);

        // (1) DB에 쿠폰 생성
        Coupon coupon = Coupon.builder()
                              .couponCode("USEDCOUPON")
                              .discountType(com.example.ElasticCommerce.domain.coupon.entity.DiscountType.FIXED)
                              .discountValue(50L)
                              .minimumOrderAmount(100L)
                              .expirationDate(now.plusDays(1))
                              .quantity(5)
                              .build();
        couponRepository.saveAndFlush(coupon);

        // (2) 발급 → 컨슈머
        IssueUserCouponRequest issueDto1 = new IssueUserCouponRequest(testUser.getUserId(), "USEDCOUPON");
        couponService.issueUserCoupon(issueDto1);

        Coupon savedCoupon = couponRepository.findByCouponCode("USEDCOUPON").orElseThrow();
        CouponKafkaDTO kafkaDto1 = CouponKafkaDTO.from(testUser.getUserId(), savedCoupon, now);
        String msg1 = objectMapper.writeValueAsString(kafkaDto1);
        couponKafkaConsumerService.consumeCoupon(msg1, new Acknowledgment() {
            @Override public void acknowledge() { }
        });

        // (3) 첫 번째 적용
        ApplyCouponRequest applyDto1 = new ApplyCouponRequest(testUser.getUserId(), "USEDCOUPON", 200L);
        Long amount1 = couponService.applyCoupon(applyDto1);
        assertThat(amount1).isEqualTo(50L);

        // (4) 두 번째 적용 시도 → 실패
        ApplyCouponRequest applyDto2 = new ApplyCouponRequest(testUser.getUserId(), "USEDCOUPON", 200L);
        assertThatThrownBy(() -> couponService.applyCoupon(applyDto2))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("쿠폰 적용 실패 - 최소 금액 미달")
    @Transactional
    void testApplyCoupon_Fail_MinimumAmountNotMet() throws Exception {
        LocalDateTime now = LocalDateTime.now(clock);

        // (1) DB에 쿠폰 생성
        Coupon coupon = Coupon.builder()
                              .couponCode("MINIMUM500")
                              .discountType(com.example.ElasticCommerce.domain.coupon.entity.DiscountType.FIXED)
                              .discountValue(50L)
                              .minimumOrderAmount(500L)
                              .expirationDate(now.plusDays(1))
                              .quantity(3)
                              .build();
        couponRepository.saveAndFlush(coupon);

        // (2) 발급 → 컨슈머
        IssueUserCouponRequest issueDto = new IssueUserCouponRequest(testUser.getUserId(), "MINIMUM500");
        couponService.issueUserCoupon(issueDto);

        Coupon savedCoupon = couponRepository.findByCouponCode("MINIMUM500").orElseThrow();
        CouponKafkaDTO kafkaDto = CouponKafkaDTO.from(testUser.getUserId(), savedCoupon, now);
        String message = objectMapper.writeValueAsString(kafkaDto);
        couponKafkaConsumerService.consumeCoupon(message, new Acknowledgment() {
            @Override public void acknowledge() { }
        });

        // (3) 적용 시도 → 실패
        ApplyCouponRequest applyDto = new ApplyCouponRequest(testUser.getUserId(), "MINIMUM500", 400L);
        assertThatThrownBy(() -> couponService.applyCoupon(applyDto))
                .isInstanceOf(BadRequestException.class);
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // 4) 동시성 테스트: Redis 선점 로직 + 컨슈머 직접 호출
    // ─────────────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("동시성 테스트: 재고 100개 쿠폰을 1000명이 동시에 발급 시도")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @DirtiesContext
    void testConcurrentIssueUserCoupon() throws Exception {
        LocalDateTime now = LocalDateTime.now(clock);

        // (1) DB에 쿠폰 생성 (quantity = 100)
        Coupon coupon = Coupon.builder()
                              .couponCode("CONC100")
                              .discountType(com.example.ElasticCommerce.domain.coupon.entity.DiscountType.FIXED)
                              .discountValue(10L)
                              .minimumOrderAmount(0L)
                              .expirationDate(now.plusDays(1))
                              .quantity(100)
                              .build();
        couponRepository.saveAndFlush(coupon);

        // (2) Redis에 초기 재고 100 세팅
        couponStockRepository.setInitialStock("CONC100", 100);

        // (3) 1000명의 쓰레드가 동시에 issueUserCoupon 호출 → Redis만 DECR 선점
        int threadCount = 1000;
        ExecutorService executor = Executors.newFixedThreadPool(32);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (long uid = 1; uid <= threadCount; uid++) {
            long userId = uid;
            executor.execute(() -> {
                try {
                    IssueUserCouponRequest dto = new IssueUserCouponRequest(userId, "CONC100");
                    try {
                        couponService.issueUserCoupon(dto);
                    } catch (RuntimeException ignore) {
                        // 재고 부족 예외 무시
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();

        // (4) Redis 재고가 정확히 0인지 확인
        Long redisStock = couponStockRepository.getStock("CONC100");
        assertThat(redisStock).isEqualTo(0L);

        // (5) “컨슈머”를 100번 호출해 DB에 반영
        for (int i = 1; i <= 100; i++) {
            Coupon savedCoupon = couponRepository.findByCouponCode("CONC100").orElseThrow();
            CouponKafkaDTO kafkaDto = CouponKafkaDTO.from((long) i, savedCoupon, now);
            String jsonMessage = objectMapper.writeValueAsString(kafkaDto);

            Acknowledgment ackStub = new Acknowledgment() {
                @Override public void acknowledge() { /* no-op */ }
            };
            couponKafkaConsumerService.consumeCoupon(jsonMessage, ackStub);
        }

        // (6) DB 최종 검증: coupon.quantity가 0, UserCoupon 개수 100
        Coupon finalCoupon = couponRepository.findByCouponCode("CONC100").orElseThrow();
        assertThat(finalCoupon.getQuantity()).isEqualTo(0);

        long ucCount = userCouponRepository.count();
        assertThat(ucCount).isEqualTo(100);
    }
}
