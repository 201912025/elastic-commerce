package com.example.ElasticCommerce.domain.product.entity;

import com.example.ElasticCommerce.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "failed_event")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FailedEvent extends BaseEntity {

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
    public FailedEvent(String payload, String topic, String errorMessage, int retryCount) {
        this.payload = payload;
        this.topic = topic;
        this.errorMessage = errorMessage;
        this.retryCount = retryCount;
    }
}
