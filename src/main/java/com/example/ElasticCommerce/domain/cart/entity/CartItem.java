package com.example.ElasticCommerce.domain.cart.entity;

import com.example.ElasticCommerce.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "cart_items")
public class CartItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long cartItemId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cart_id")
    private Cart cart;

    @Column(nullable = false)
    private Long productId;

    @Column(nullable = false)
    private String productName;

    @Column(nullable = false, precision = 13, scale = 2)
    private BigDecimal unitPrice;

    @Column(nullable = false)
    private Integer quantity;

    @Builder
    public CartItem(Long productId, String productName, BigDecimal unitPrice, Integer quantity) {
        this.productId   = productId;
        this.productName = productName;
        this.unitPrice   = unitPrice;
        this.quantity    = quantity;
    }

    public void setCart(Cart cart) {
        this.cart = cart;
    }

    public void updateQuantity(Integer qty) {
        this.quantity = qty;
    }

    public BigDecimal getTotalPrice() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
