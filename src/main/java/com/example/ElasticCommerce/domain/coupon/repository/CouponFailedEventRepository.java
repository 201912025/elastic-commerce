package com.example.ElasticCommerce.domain.coupon.repository;

import com.example.ElasticCommerce.domain.coupon.entity.CouponFailedEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CouponFailedEventRepository extends JpaRepository<CouponFailedEvent , Long> {
}
