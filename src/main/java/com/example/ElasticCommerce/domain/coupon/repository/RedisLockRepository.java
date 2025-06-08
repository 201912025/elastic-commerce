package com.example.ElasticCommerce.domain.coupon.repository;

import java.time.Duration;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisLockRepository {
    // 레디스를 사용하기 위해 RedisTemplate 클래스를 변수로 추가한다.
    private RedisTemplate<String, String> redisTemplate;

    public RedisLockRepository(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public boolean lock(String key) {
        // key 단위로 3초 동안만 락을 잡는다
        return redisTemplate.opsForValue()
                            .setIfAbsent(key, "LOCKED", Duration.ofMillis(3_000));
    }

    public void unlock(String key) {
        redisTemplate.delete(key);
    }
}
