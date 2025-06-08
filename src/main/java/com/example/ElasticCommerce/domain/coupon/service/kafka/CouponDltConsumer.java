package com.example.ElasticCommerce.domain.coupon.service.kafka;

import com.example.ElasticCommerce.domain.coupon.dto.CouponKafkaDTO;
import com.example.ElasticCommerce.domain.coupon.entity.Coupon;
import com.example.ElasticCommerce.domain.coupon.entity.CouponFailedEvent;
import com.example.ElasticCommerce.domain.coupon.entity.UserCoupon;
import com.example.ElasticCommerce.domain.coupon.exception.CouponExceptionType;
import com.example.ElasticCommerce.domain.coupon.repository.CouponFailedEventRepository;
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
public class CouponDltConsumer {

    private final ObjectMapper objectMapper;
    private final CouponRepository couponRepository;
    private final UserRepository userRepository;
    private final UserCouponRepository userCouponRepository;
    private final CouponStockRepository couponStockRepository;
    private final CouponFailedEventRepository failedEventRepository;

    @KafkaListener(
            topics           = "coupon-topic.DLT",
            groupId          = "coupon-group.dlt",
            containerFactory = "kafkaListenerContainerFactory",
            concurrency      = "6"
    )
    @Transactional
    public void consumeCouponDlt(String message, Acknowledgment ack) {
        log.info("[DLT] 메시지 수신: {}", message);

        // 1) DTO 파싱
        CouponKafkaDTO dto;
        try {
            dto = objectMapper.readValue(message, CouponKafkaDTO.class);
        } catch (JsonProcessingException e) {
            log.error("[DLT] JSON 파싱 실패", e);
            recordFailure(message, 0);
            ack.acknowledge();
            return;
        }

        String code = dto.couponCode();
        int updated = couponRepository.decrementQuantity(code);
        if (updated != 1) {
            log.warn("[DLT] 재고 부족 또는 잘못된 코드: {}", code);
            couponStockRepository.increment(code);
            ack.acknowledge();
            return;
        }

        try {
            // 4) 발급 기록 저장
            User user = userRepository.findById(dto.userId())
                                      .orElseThrow(() -> new NotFoundException(UserExceptionType.NOT_FOUND_USER));
            Coupon coupon = couponRepository.findByCouponCode(code)
                                            .orElseThrow(() -> new NotFoundException(CouponExceptionType.COUPON_NOT_FOUND));
            userCouponRepository.save(UserCoupon.builder()
                                                .user(user)
                                                .coupon(coupon)
                                                .build());

            log.info("[DLT] 재처리 성공: couponCode={}, userId={}", code, dto.userId());
        } catch (DataAccessException | NotFoundException ex) {
            log.error("[DLT] 재처리 중 예외 발생", ex);
            recordFailure(message, 1);
            // 롤백된 DB 재고 → 다시 복구
            couponStockRepository.increment(code);
        } finally {
            ack.acknowledge();
        }
    }

    private void recordFailure(String payload, int retryCount) {
        failedEventRepository.save(CouponFailedEvent.builder()
                                                    .payload(payload)
                                                    .topic("coupon-topic.DLT")
                                                    .errorMessage(truncate(payload, 500))
                                                    .retryCount(retryCount)
                                                    .build());
    }

    private String truncate(String msg, int maxLen) {
        return msg == null
                ? null
                : msg.length() <= maxLen ? msg : msg.substring(0, maxLen);
    }
}

