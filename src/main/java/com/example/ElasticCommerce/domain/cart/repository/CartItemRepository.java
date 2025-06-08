package com.example.ElasticCommerce.domain.cart.repository;

import com.example.ElasticCommerce.domain.cart.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    Optional<CartItem> findByCartCartIdAndProductId(Long cartId, Long productId);
}
