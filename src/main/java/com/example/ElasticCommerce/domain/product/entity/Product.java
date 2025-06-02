package com.example.ElasticCommerce.domain.product.entity;

import com.example.ElasticCommerce.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "products")
public class Product extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String productCode;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, length = 20)
    private String category;

    @Column(nullable = false)
    private Integer stockQuantity;

    @Column(length = 50)
    private String brand;

    private String imageUrl;

    @Column(nullable = false)
    private boolean available = true;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false)
    private Long price;

    @Column(nullable = false)
    private double rating = 0.0;

    @Builder
    public Product(String productCode, String name, String category, Integer stockQuantity, String brand, String imageUrl, String description, Long price) {
        this.productCode = productCode;
        this.name = name;
        this.category = category;
        this.stockQuantity = stockQuantity;
        this.brand = brand;
        this.imageUrl = imageUrl;
        this.description = description;
        this.price = price;
    }

    public void updateRating(double updateRating) {
        this.rating = updateRating;
    }
}
