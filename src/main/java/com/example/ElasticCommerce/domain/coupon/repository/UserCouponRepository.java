package com.example.ElasticCommerce.domain.coupon.repository;

import com.example.ElasticCommerce.domain.coupon.entity.UserCoupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserCouponRepository extends JpaRepository<UserCoupon, Long> {

    @Query("SELECT uc " +
            "FROM UserCoupon uc " +
            "WHERE uc.user.userId = :userId " +
            "  AND uc.coupon.couponCode = :couponCode")
    Optional<UserCoupon> findByUserIdAndCouponCode(
            @Param("userId") Long userId,
            @Param("couponCode") String couponCode
    );

    @Query("""
        select uc
        from UserCoupon uc
        join fetch uc.coupon c
        join fetch uc.user u
        where u.userId = :userId
          and c.couponCode = :couponCode
        """)
    Optional<UserCoupon> findByUserIdAndCouponCodeFetchCoupon(
            @Param("userId") Long userId,
            @Param("couponCode") String couponCode
    );

    @Query("SELECT uc FROM UserCoupon uc WHERE uc.user.userId = :userId")
    List<UserCoupon> findAllByUserId(@Param("userId") Long userId);
}
