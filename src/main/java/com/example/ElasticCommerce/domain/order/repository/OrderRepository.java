package com.example.ElasticCommerce.domain.order.repository;

import com.example.ElasticCommerce.domain.order.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findByIdAndUserId(Long orderId, Long userId);
    Page<Order> findAllByUserUserId(Long userId, Pageable pageable);

}
