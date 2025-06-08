package com.example.ElasticCommerce.domain.coupon.service;

import com.example.ElasticCommerce.domain.coupon.dto.IssueUserCouponRequest;
import com.example.ElasticCommerce.domain.coupon.repository.RedisLockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class LettuceLockStockFacade {

    private final RedisLockRepository lockRepository;
    private final CouponService couponService;

    public void issueUserCouponWithLock(IssueUserCouponRequest dto) {
        String lockKey = "lock:coupon:" + dto.couponCode();
        // 락이 획득될 때까지 반복
        while (!lockRepository.lock(lockKey)) {
            log.info("락 획득 실패, 재시도 중…");
            try {
                Thread.sleep(100);  // 잠시 대기
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new CannotAcquireLockException("락 재시도 중 인터럽트 발생");
            }
        }

        log.info("락 획득: {}", lockKey);
        try {
            couponService.issueUserCoupon(dto);
        } finally {
            lockRepository.unlock(lockKey);
            log.info("락 해제: {}", lockKey);
        }
    }

}

