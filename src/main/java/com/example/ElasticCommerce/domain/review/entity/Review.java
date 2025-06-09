package com.example.ElasticCommerce.domain.review.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "reviews")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Review {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long productId;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private double rating;

    @Column(length = 1000)
    private String comment;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime modifiedAt;

    @Builder
    public Review(Long id,
                  Long productId,
                  Long userId,
                  String title,
                  double rating,
                  String comment,
                  LocalDateTime createdAt,
                  LocalDateTime modifiedAt) {
        this.id = id;
        this.productId = productId;
        this.userId = userId;
        this.title = title;
        this.rating = rating;
        this.comment = comment;
        this.createdAt = createdAt;
        this.modifiedAt = modifiedAt;
    }

    public void update(String title, double rating, String comment) {
        this.title = title;
        this.rating = rating;
        this.comment = comment;
        this.modifiedAt = LocalDateTime.now();
    }
}
