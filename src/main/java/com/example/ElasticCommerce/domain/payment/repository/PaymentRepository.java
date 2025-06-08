package com.example.ElasticCommerce.domain.payment.repository;

import com.example.ElasticCommerce.domain.payment.entity.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByOrderId(Long orderId);

    @Query("""
        SELECT p
          FROM Payment p
         WHERE p.order.user.userId = :userId
    """)
    Page<Payment> findAllByUserId(@Param("userId") Long userId, Pageable pageable);

}
