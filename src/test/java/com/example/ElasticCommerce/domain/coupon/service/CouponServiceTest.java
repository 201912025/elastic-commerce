package com.example.ElasticCommerce.domain.coupon.service;

import com.example.ElasticCommerce.domain.coupon.dto.ApplyCouponRequest;
import com.example.ElasticCommerce.domain.coupon.dto.IssueCouponRequest;
import com.example.ElasticCommerce.domain.coupon.dto.IssueUserCouponRequest;
import com.example.ElasticCommerce.domain.coupon.entity.Coupon;
import com.example.ElasticCommerce.domain.coupon.entity.DiscountType;
import com.example.ElasticCommerce.domain.coupon.entity.UserCoupon;
import com.example.ElasticCommerce.domain.coupon.repository.CouponRepository;
import com.example.ElasticCommerce.domain.coupon.repository.CouponStockRepository;
import com.example.ElasticCommerce.domain.coupon.repository.UserCouponRepository;
import com.example.ElasticCommerce.domain.coupon.service.kafka.CouponKafkaProducerService;
import com.example.ElasticCommerce.domain.user.entity.User;
import com.example.ElasticCommerce.domain.user.repository.UserRepository;
import com.example.ElasticCommerce.global.exception.type.BadRequestException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertAll;

@Testcontainers
@SpringBootTest(
        properties = {
                "spring.main.allow-bean-definition-overriding=true",
                // EmbeddedKafka가 띄워질 때 spring.embedded.kafka.brokers에 브로커 주소가 자동 세팅됩니다.
                "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}"
        }
)
@ContextConfiguration(initializers = CouponServiceTest.Initializer.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 1, topics = { "coupon-topic" })
class CouponServiceTest {

    // ─── Testcontainers로 띄운 RedisContainer ───────────────────────────────────
    @Container
    static GenericContainer<?> redisContainer =
            new GenericContainer<>("redis:6.2-alpine")
                    .withExposedPorts(6379)
                    .waitingFor(Wait.forListeningPort());

    // ─── Autowired 빈들 ──────────────────────────────────────────────────────────
    @Autowired private CouponService couponService;
    @Autowired private CouponRepository couponRepository;
    @Autowired private UserCouponRepository userCouponRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private CouponStockRepository couponStockRepository;
    @Autowired private Clock clock;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private CouponKafkaProducerService couponKafkaProducerService;
    // ─────────────────────────────────────────────────────────────────────────────

    private User testUser;

    /**
     * 각 테스트 전에 DB와 Redis를 “완전히 초기화”하고, 테스트 전용 유저를 생성합니다.
     * Propagation.REQUIRES_NEW를 사용해 해당 로직만 별도 트랜잭션에서 즉시 커밋되도록 보장합니다.
     */
    @BeforeEach
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void setUp() {
        // 1) DB 초기화 → 별도 트랜잭션(새로운 트랜잭션)에서 즉시 커밋
        userCouponRepository.deleteAll();
        couponRepository.deleteAll();
        userRepository.deleteAll();
        userRepository.flush();

        // 2) Redis 초기화 (키 전부 삭제)
        couponStockRepository.deleteAllKeys();

        // 3) 테스트용 유저 생성 → 즉시 커밋
        testUser = userRepository.save(
                User.builder()
                    .username("testUser")
                    .email("test@example.com")
                    .password("password123")
                    .role("USER")
                    .birthDay("1990-01-01")
                    .build()
        );
        userRepository.flush();
    }

    // ─── 테스트 전용 Clock 빈 (2025-06-05 00:00 KST 고정) ─────────────────────────
    @TestConfiguration
    static class ClockTestConfig {
        @Bean
        public Clock clock() {
            Instant fixedInstant = LocalDateTime
                    .of(2025, 6, 5, 0, 0)
                    .atZone(ZoneId.of("Asia/Seoul"))
                    .toInstant();
            return Clock.fixed(fixedInstant, ZoneId.of("Asia/Seoul"));
        }
    }

    // ─── 테스트 전용 ObjectMapper 빈 (JavaTimeModule 포함) ────────────────────────
    @TestConfiguration
    static class ObjectMapperConfig {
        @Bean
        public ObjectMapper objectMapper() {
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            mapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            return mapper;
        }
    }

    // ─── ApplicationContextInitializer: Testcontainers Redis 포트 값을 스프링 프로퍼티로 주입 ─────────────────
    static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override
        public void initialize(ConfigurableApplicationContext context) {
            String redisHost = redisContainer.getHost();
            Integer redisPort = redisContainer.getMappedPort(6379);

            TestPropertyValues.of(
                    "spring.redis.host=" + redisHost,
                    "spring.redis.port=" + redisPort
            ).applyTo(context.getEnvironment());
        }
    }


    // ─────────────────────────────────────────────────────────────────────────────
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
    void testIssueUserCoupon_Success() {
        // ─── 1) 쿠폰 저장 + 즉시 DB에 반영 ─────────────────────────────────────────
        LocalDateTime now = LocalDateTime.now(clock);
        Coupon coupon = Coupon.builder()
                              .couponCode("USER50")
                              .discountType(DiscountType.FIXED)
                              .discountValue(50L)
                              .minimumOrderAmount(200L)
                              .expirationDate(now.plusDays(3))
                              .quantity(5)
                              .build();
        // saveAndFlush를 호출하면 JPA가 바로 INSERT 쿼리를 날려 DB에 커밋합니다.
        couponRepository.saveAndFlush(coupon);

        // ─── 2) 사용자에게 쿠폰 발급 요청 (Kafka 프로듀싱 → Consumer가 처리) ────────
        IssueUserCouponRequest dto = new IssueUserCouponRequest(testUser.getUserId(), "USER50");
        couponService.issueUserCoupon(dto);

        // ─── 3) Consumer가 DB에 UserCoupon/quantity 업데이트를 완료할 때까지 최대 5초간 대기 ──
        Awaitility.await()
                  .pollInterval(Duration.ofMillis(100))
                  .atMost(Duration.ofSeconds(5))
                  .until(() ->
                          // findByUserIdAndCouponCodeFetchCoupon 를 사용해서
                          // JOIN FETCH된 엔티티가 리턴되는지 확인
                          userCouponRepository
                                  .findByUserIdAndCouponCodeFetchCoupon(testUser.getUserId(), "USER50")
                                  .isPresent()
                  );

        // ─── 4) 실제 DB에 반영된 결과를 JOIN FETCH 메서드로 조회 ─────────────────────────
        UserCoupon uc = userCouponRepository
                .findByUserIdAndCouponCodeFetchCoupon(testUser.getUserId(), "USER50")
                .orElseThrow();

        // 이제 uc.getCoupon() 까지 미리 로드되었으므로 LazyInitializationException이 발생하지 않습니다.
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
    void testApplyCoupon_Success_Fixed() {
        // ─── 1) 기본 쿠폰 생성 및 저장 ─────────────────────────────────────────────
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

        // ─── 2) 사용자에게 쿠폰 발급 요청 (Kafka 비동기 처리) ────────────────────────
        IssueUserCouponRequest issueDto =
                new IssueUserCouponRequest(testUser.getUserId(), "FIXED100");
        couponService.issueUserCoupon(issueDto);

        // ─── 3) Consumer가 발급을 완료할 때까지 최대 5초간 대기 ────────────────────────
        Awaitility.await()
                  .pollInterval(Duration.ofMillis(100))
                  .atMost(Duration.ofSeconds(5))
                  .until(() ->
                          userCouponRepository
                                  .findByUserIdAndCouponCode(testUser.getUserId(), "FIXED100")
                                  .isPresent()
                  );

        // ─── 4) 쿠폰 적용 시도 → 리턴된 할인 금액 검증 ───────────────────────────────
        ApplyCouponRequest applyDto =
                new ApplyCouponRequest(testUser.getUserId(), "FIXED100", 1000L);
        Long discountAmount = couponService.applyCoupon(applyDto);
        assertThat(discountAmount).isEqualTo(100L);

        // ─── 5) “사용 처리”가 Consumer에 의해 DB에 반영될 때까지 최대 5초간 대기 ───────────
        Awaitility.await()
                  .pollInterval(Duration.ofMillis(100))
                  .atMost(Duration.ofSeconds(5))
                  .until(() -> {
                      Optional<UserCoupon> optUc =
                              userCouponRepository
                                      .findByUserIdAndCouponCode(testUser.getUserId(), "FIXED100");
                      return optUc.isPresent() && optUc.get().isUsed();
                  });

        // ─── 6) 실제 DB 상태 검증 ──────────────────────────────────────────────────
        UserCoupon uc = userCouponRepository
                .findByUserIdAndCouponCode(testUser.getUserId(), "FIXED100")
                .orElseThrow();
        assertThat(uc.isUsed()).isTrue();
    }

    @Test
    @DisplayName("쿠폰 적용 실패 - 이미 사용된 쿠폰")
    void testApplyCoupon_Fail_AlreadyUsed() {
        // 1) 테스트용 쿠폰 생성 및 저장
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

        // 2) 사용자에게 쿠폰 발급 호출 (Kafka 프로듀싱 → Consumer가 비동기로 DB에 삽입)
        IssueUserCouponRequest issueDto =
                new IssueUserCouponRequest(testUser.getUserId(), "USEDCOUPON");
        couponService.issueUserCoupon(issueDto);

        // 3) Consumer가 실제 DB에 발급을 완료할 때까지 최대 5초간 대기
        Awaitility.await()
                  .pollInterval(Duration.ofMillis(100))
                  .atMost(Duration.ofSeconds(5))
                  .until(() ->
                          userCouponRepository
                                  .findByUserIdAndCouponCode(testUser.getUserId(), "USEDCOUPON")
                                  .isPresent()
                  );

        // 4) 첫 번째 사용 시도(정상적으로 DB에 user_coupon이 있고, isUsed==false여야 함)
        ApplyCouponRequest applyDto1 =
                new ApplyCouponRequest(testUser.getUserId(), "USEDCOUPON", 200L);
        couponService.applyCoupon(applyDto1);

        // 5) “사용 처리”가 끝나고, DB에 isUsed=true, quantity=4로 반영될 때까지 최대 5초간 대기
        Awaitility.await()
                  .pollInterval(Duration.ofMillis(100))
                  .atMost(Duration.ofSeconds(5))
                  .until(() -> {
                      Optional<UserCoupon> ucOpt =
                              userCouponRepository
                                      .findByUserIdAndCouponCode(testUser.getUserId(), "USEDCOUPON");
                      return ucOpt.isPresent() && ucOpt.get().isUsed();
                  });

        // 6) 두 번째 재사용 시도: 이미 isUsed=true 이므로 BadRequestException 발생 예상
        ApplyCouponRequest applyDto2 =
                new ApplyCouponRequest(testUser.getUserId(), "USEDCOUPON", 200L);
        assertThatThrownBy(() -> couponService.applyCoupon(applyDto2))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("쿠폰 적용에 실패했습니다.");
    }

    @Test
    @DisplayName("동시성 테스트: 재고 100개 쿠폰을 1000명이 동시에 발급 시도")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @DirtiesContext
    void testConcurrentIssueUserCoupon() throws InterruptedException {
        // 1) DB에 쿠폰 저장 (saveAndFlush → 즉시 커밋)
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

        // 2) 1000명 유저 미리 저장 (flush → 즉시 커밋)
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

        // 3) 1000개 동시 요청 처리 스레드풀 + 래치
        int threadCount = 1000;
        ExecutorService executor = Executors.newFixedThreadPool(32);
        CountDownLatch latch = new CountDownLatch(threadCount);

        // 4) 각 스레드에서 issueUserCoupon 호출
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

        // 5) 모든 스레드 완료 대기
        latch.await();
        // Consumer가 남은 비동기 처리를 마치도록 잠시 대기
        Thread.sleep(5_000);

        assertAll(
                () -> {
                    // DB 상에서 쿠폰 재고가 0인지 확인
                    Coupon finalCoupon = couponRepository.findByCouponCode("CONC100")
                                                         .orElseThrow();
                    assertThat(finalCoupon.getQuantity()).isEqualTo(0);
                },
                () -> {
                    // UserCoupon 테이블에는 100개만 생성되어야 한다
                    long savedCount = userCouponRepository.count();
                    assertThat(savedCount).isEqualTo(100);
                }
        );
    }
}
