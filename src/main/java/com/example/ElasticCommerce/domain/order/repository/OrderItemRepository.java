package com.example.ElasticCommerce.domain.order.repository;

import com.example.ElasticCommerce.domain.order.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
}
