package com.example.ElasticCommerce.domain.coupon.repository;

import com.example.ElasticCommerce.domain.coupon.entity.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CouponRepository extends JpaRepository<Coupon, Long> {

    Optional<Coupon> findByCouponCode(String couponCode);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Coupon c " +
            "SET c.quantity = c.quantity - 1 " +
            "WHERE c.couponCode = :couponCode")
    int decrementQuantity(@Param("couponCode") String couponCode);

    @Modifying
    @Query("UPDATE Coupon c SET c.quantity = c.quantity + 1 " +
            "WHERE c.couponCode = :couponCode")
    int incrementQuantity(String couponCode);
}
