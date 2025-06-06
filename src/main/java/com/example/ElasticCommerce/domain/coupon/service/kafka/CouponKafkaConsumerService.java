package com.example.ElasticCommerce.domain.coupon.service.kafka;

import com.example.ElasticCommerce.domain.coupon.dto.CouponKafkaDTO;
import com.example.ElasticCommerce.domain.coupon.entity.Coupon;
import com.example.ElasticCommerce.domain.coupon.entity.UserCoupon;
import com.example.ElasticCommerce.domain.coupon.exception.CouponExceptionType;
import com.example.ElasticCommerce.domain.coupon.repository.CouponRepository;
import com.example.ElasticCommerce.domain.coupon.repository.UserCouponRepository;
import com.example.ElasticCommerce.domain.coupon.repository.CouponStockRepository;
import com.example.ElasticCommerce.domain.user.entity.User;
import com.example.ElasticCommerce.domain.user.exception.UserExceptionType;
import com.example.ElasticCommerce.domain.user.repository.UserRepository;
import com.example.ElasticCommerce.global.exception.type.NotFoundException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Slf4j
@RequiredArgsConstructor
public class CouponKafkaConsumerService {

    private final CouponRepository           couponRepository;
    private final UserCouponRepository       userCouponRepository;
    private final UserRepository             userRepository;
    private final CouponStockRepository      couponStockRepository; // Redis 재고 복구용
    private final ObjectMapper               objectMapper;

    /**
     * KafkaListener: "coupon-topic"을 구독
     * 1) 메시지(JSON)를 CouponKafkaDTO로 파싱
     * 2) DB에서 Coupon, User 조회
     * 3) DB에서 재고 원자적 감소 → 성공 시 UserCoupon 저장, 실패 시 Redis 재고 복구
     * 4) UserCoupon 저장 중 예외 발생 시도 Redis 재고 복구
     * 5) ack.acknowledge() 호출
     */
    @KafkaListener(
            topics = "coupon-topic",
            groupId = "coupon-group",
            containerFactory = "kafkaListenerContainerFactory",
            concurrency = "6"
    )
    @Transactional
    public void consumeCoupon(String message, Acknowledgment ack) {
        CouponKafkaDTO couponKafkaDTO;
        try {
            couponKafkaDTO = objectMapper.readValue(message, CouponKafkaDTO.class);
        } catch (JsonProcessingException e) {
            log.error("[KAFKA][PARSE_ERROR] 메시지 파싱 오류: {}", message, e);
            // 파싱 자체가 안 되면 복구할 Redis 재고도 없으므로 그냥 ack
            ack.acknowledge();
            return;
        }

        String couponCode = couponKafkaDTO.couponCode();

        // 1) DB 재고 UPDATE (원자적 감소)
        int updatedRows = couponRepository.decrementQuantity(couponCode);
        if (updatedRows != 1) {
            // 재고가 없거나 couponCode가 잘못된 경우 → Redis 재고 복구
            log.warn("[KAFKA] 재고 부족 또는 쿠폰 없음: couponCode={}", couponCode);
            couponStockRepository.increment(couponCode);
            ack.acknowledge();
            return;
        }

        // 2) User 조회 및 UserCoupon 저장
        try {
            User user = userRepository.findById(couponKafkaDTO.userId())
                                      .orElseThrow(() -> new NotFoundException(UserExceptionType.NOT_FOUND_USER));

            Coupon coupon = couponRepository.findById(couponKafkaDTO.couponId())
                                            .orElseThrow(() -> new NotFoundException(CouponExceptionType.COUPON_NOT_FOUND));

            UserCoupon userCoupon = UserCoupon.builder()
                                              .coupon(coupon)
                                              .user(user)
                                              .build();
            userCouponRepository.save(userCoupon);
            log.info("[KAFKA] 쿠폰 발급 완료: userId={}, couponCode={}", couponKafkaDTO.userId(), couponCode);

        } catch (DataAccessException | NotFoundException ex) {
            // DB 저장 중 예외 발생 시, 이미 감소된 DB 재고는 롤백(트랜잭션으로 자동 복구되지만),
            // Redis 재고는 수동 복구 필요
            log.error("[KAFKA] UserCoupon 저장 실패, Redis 재고 복구: couponCode={}", couponCode, ex);
            couponStockRepository.increment(couponCode);
            // 트랜잭션이 롤백되므로 DB quantity는 원래값으로 돌아갑니다.
        }

        // 3) 정상 처리(또는 복구) 후 ACK
        ack.acknowledge();
    }
}
