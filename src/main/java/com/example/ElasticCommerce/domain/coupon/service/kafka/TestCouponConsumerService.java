package com.example.ElasticCommerce.domain.coupon.service.kafka;

import com.example.ElasticCommerce.domain.coupon.dto.CouponKafkaDTO;
import com.example.ElasticCommerce.domain.coupon.entity.Coupon;
import com.example.ElasticCommerce.domain.coupon.entity.UserCoupon;
import com.example.ElasticCommerce.domain.coupon.repository.CouponRepository;
import com.example.ElasticCommerce.domain.coupon.repository.UserCouponRepository;
import com.example.ElasticCommerce.domain.user.entity.User;
import com.example.ElasticCommerce.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.CountDownLatch;

@Service
@RequiredArgsConstructor
public class TestCouponConsumerService {

    private final CouponRepository couponRepository;
    private final UserCouponRepository userCouponRepository;
    private final UserRepository userRepository;

    // 테스트가 setLatch()로 주입할 래치
    private CountDownLatch latch;

    public void setLatch(CountDownLatch latch) {
        this.latch = latch;
    }

    /**
     * KafkaListener는 "coupon-topic"을 구독하도록 설정합니다.
     * 메시지를 받으면—
     *   1) dto.userId()로 User 엔티티 조회
     *   2) dto.couponCode()로 Coupon 엔티티 조회
     *   3) 재고가 있으면 UserCoupon 저장 + 쿠폰 재고 감소(decreaseQuantity())
     *   4) 재고가 없거나 Coupon/​User가 없더라도 예외 없이 넘어갑니다.
     *   5) 무조건 latch.countDown()을 호출해 테스트 쪽에서 await()가 풀리도록 보장
     */
    @KafkaListener(
            topics = "coupon-topic",
            groupId = "coupon-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(CouponKafkaDTO dto) {
        try {
            Optional<User> optionalUser = userRepository.findById(dto.userId());
            Optional<Coupon> optionalCoupon = couponRepository.findByCouponCode(dto.couponCode());

            if (optionalUser.isPresent() && optionalCoupon.isPresent()) {
                User user = optionalUser.get();
                Coupon coupon = optionalCoupon.get();

                if (coupon.hasStock()) {
                    // UserCoupon 생성 시 used는 기본값(false)으로 설정됨
                    UserCoupon uc = UserCoupon.builder()
                                              .user(user)
                                              .coupon(coupon)
                                              .build();
                    userCouponRepository.save(uc);

                    // 재고 차감 메서드는 decreaseQuantity()
                    coupon.decreaseQuantity();
                    couponRepository.save(coupon);
                }
            }
        } catch (Exception ignore) {
            // 테스트 전용 컨슈머이므로 예외는 무시합니다.
        } finally {
            if (latch != null) {
                latch.countDown();
            }
        }
    }
}
