package com.example.ElasticCommerce.domain.review.entity;

import com.example.ElasticCommerce.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "review_failed_event")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReviewFailedEvent extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(nullable = false)
    private String topic;

    @Column(nullable = true, length = 500)
    private String errorMessage;

    @Column(nullable = false)
    private int retryCount;

    @Builder
    public ReviewFailedEvent(String payload, String topic, String errorMessage, int retryCount) {
        this.payload = payload;
        this.topic = topic;
        this.errorMessage = errorMessage;
        this.retryCount = retryCount;
    }
}

