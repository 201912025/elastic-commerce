package com.example.ElasticCommerce.domain.coupon.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class CouponStockRepository {
    private final RedisTemplate<String, String> redisTemplate;

    private String stockKey(String couponCode) {
        return "coupon-stock:" + couponCode;
    }

    public Long decrement(String couponCode) {
        return redisTemplate.opsForValue().decrement(stockKey(couponCode));
    }

    /**
     * Redis INCR 명령을 호출해 “DECR로 줄어든 재고”를 1 증가시켜 복구합니다.
     * (예: DECR 호출 결과가 -1이었는데, 이는 “발급 불가”이므로, 다시 INCR을 호출해 Redis 재고를 0으로 되돌립니다.)
     *
     * @param couponCode 쿠폰 코드
     * @return 증가 후의 재고 (Long)
     */
    public Long increment(String couponCode) {
        return redisTemplate.opsForValue().increment(stockKey(couponCode));
    }

    /**
     * 테스트 준비 혹은 쿠폰 생성 시 호출해서 Redis에 초기 재고를 세팅합니다.
     * 예를 들어 quantity=100이라면,
     *   redisTemplate.opsForValue().set("coupon-stock:CONC100", "100")
     * 을 수행해야 합니다.
     *
     * @param couponCode 쿠폰 코드
     * @param quantity   초기 재고 (예: 100)
     */
    public void setInitialStock(String couponCode, int quantity) {
        redisTemplate.opsForValue().set(stockKey(couponCode), String.valueOf(quantity));
    }

    public Boolean existsKey(String redisKey) {
        return redisTemplate.hasKey(redisKey);
    }
}
