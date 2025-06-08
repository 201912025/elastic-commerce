package com.example.ElasticCommerce.domain.coupon.service;

import com.example.ElasticCommerce.domain.coupon.dto.IssueUserCouponRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedissonLockStockFacade {

    private final RedissonClient redissonClient;
    private final CouponService couponService;

    /**
     * RLock#lock() 을 사용해 락이 풀릴 때까지 대기합니다.
     * leaseTime 이후 자동으로 락이 풀리므로,
     * 서비스 수행 시간보다 길게 설정하세요.
     */
    public void issueUserCouponWithRedissonLock(IssueUserCouponRequest dto) {
        String lockKey = "lock:coupon:" + dto.couponCode();
        RLock lock = redissonClient.getLock(lockKey);

        // 최대 5초 대기 후 30초간 락 획득
        try {
            boolean acquired = lock.tryLock(5, 30, TimeUnit.SECONDS);
            if (!acquired) {
                throw new CannotAcquireLockException("락 획득 실패: 잠시 후 다시 시도해주세요.");
            }

            log.info("Redisson lock 획득: {}", lockKey);
            couponService.issueUserCoupon(dto);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CannotAcquireLockException("락 대기 중 인터럽트 발생");
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.info("Redisson lock 해제: {}", lockKey);
            }
        }
    }
}
