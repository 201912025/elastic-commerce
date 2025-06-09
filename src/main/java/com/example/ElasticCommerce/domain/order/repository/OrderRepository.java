package com.example.ElasticCommerce.domain.order.repository;

import com.example.ElasticCommerce.domain.order.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    @Query("""
        SELECT o 
          FROM Order o 
         WHERE o.id = :orderId 
           AND o.user.userId = :userId
    """)
    Optional<Order> findByIdAndUserId(
            @Param("orderId") Long orderId,
            @Param("userId")   Long userId
    );

    @Query("""
        SELECT o
          FROM Order o
         WHERE o.user.userId = :userId
    """)
    Page<Order> findAllByUserUserId(
            @Param("userId") Long userId,
            Pageable pageable
    );

}
