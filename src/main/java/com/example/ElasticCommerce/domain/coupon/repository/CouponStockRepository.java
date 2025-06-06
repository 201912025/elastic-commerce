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

    /**
     * Redis DECR 명령으로 재고를 1만큼 감소시킵니다.
     * @param couponCode 쿠폰 코드
     * @return 감소 전 재고가 Long으로 리턴됩니다. (키가 없으면 null 또는 -1 반환 가능)
     */
    public Long decrement(String couponCode) {
        return redisTemplate.opsForValue().decrement(stockKey(couponCode));
    }

    /**
     * Redis INCR 명령을 호출해 “DECR로 줄어든 재고”를 1 증가시켜 복구합니다.
     * (예: DECR 호출 결과가 -1이었는데, 이는 “발급 불가”이므로, 다시 INCR을 호출해 Redis 재고를 0으로 되돌립니다.)
     * @param couponCode 쿠폰 코드
     * @return 증가 후의 재고 (Long)
     */
    public Long increment(String couponCode) {
        return redisTemplate.opsForValue().increment(stockKey(couponCode));
    }

    /**
     * SETNX(set if not exists)처럼 동작하여, 키가 존재하지 않을 때만 초기 재고를 세팅합니다.
     * 이미 키가 있으면 아무 동작도 하지 않습니다.
     *
     * @param couponCode 쿠폰 코드
     * @param quantity   초기 재고 (예: 100)
     */
    public void setIfAbsent(String couponCode, int quantity) {
        redisTemplate.opsForValue().setIfAbsent(stockKey(couponCode), String.valueOf(quantity));
    }

    /**
     * 기존의 setInitialStock 기능을 그대로 두고 싶은 경우 함께 사용 가능합니다.
     * 테스트나 쿠폰 생성 시, Redis에 강제로 값을 덮어써야 한다면 이 메서드를 호출하세요.
     *
     * @param couponCode 쿠폰 코드
     * @param quantity   초기 재고 (예: 100)
     */
    public void setInitialStock(String couponCode, int quantity) {
        redisTemplate.opsForValue().set(stockKey(couponCode), String.valueOf(quantity));
    }

    /**
     * 특정 키가 존재하는지 확인합니다.
     *
     * @param couponCode 쿠폰 코드
     * @return 키가 있으면 true, 없으면 false
     */
    public Boolean existsKey(String couponCode) {
        return redisTemplate.hasKey(stockKey(couponCode));
    }

    /**
     * 현재 Redis에 저장된 재고 수량을 조회합니다.
     * (키가 없으면 null을 반환하거나, "false" 분기 처리 가능)
     *
     * @param couponCode 쿠폰 코드
     * @return 현재 재고 (Long) 또는 null
     */
    public Long getStock(String couponCode) {
        String value = redisTemplate.opsForValue().get(stockKey(couponCode));
        return (value == null) ? null : Long.valueOf(value);
    }

    /**
     * 테스트나 초기화 용도로, Redis에 저장된 모든 “coupon-stock:*” 키를 삭제합니다.
     */
    public void deleteAllKeys() {
        // RedisTemplate.keys("coupon-stock:*") 호출 후 해당 키들 전부 삭제
        redisTemplate.keys("coupon-stock:*")
                     .forEach(redisTemplate::delete);
    }
}
