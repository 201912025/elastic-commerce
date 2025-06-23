package com.example.ElasticCommerce.global.config.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    //  토픽을 파티션 6, 브로커 1으로 생성
    @Bean
    public NewTopic productTopic() {
        return TopicBuilder.name("product-topic")
                           .partitions(6)
                           .replicas(1)
                           .build();
    }

    @Bean
    public NewTopic productDltTopic() {
        return TopicBuilder.name("product-topic.DLT")
                           .partitions(6)
                           .replicas(1)
                           .build();
    }

    @Bean
    public NewTopic couponTopic() {
        return TopicBuilder.name("coupon-topic")
                           .partitions(6)
                           .replicas(1)
                           .build();
    }

    @Bean
    public NewTopic couponDltTopic() {
        return TopicBuilder.name("coupon-topic.DLT")
                           .partitions(6)
                           .replicas(1)
                           .build();
    }

    @Bean
    public NewTopic reviewTopic() {
        return TopicBuilder.name("review-topic")
                           .partitions(6)
                           .replicas(1)
                           .build();
    }

    @Bean
    public NewTopic reviewDltTopic() {
        return TopicBuilder.name("review-topic.DLT")
                           .partitions(6)
                           .replicas(1)
                           .build();
    }

    @Bean
    public NewTopic reviewRatingTopic() {
        return TopicBuilder.name("review-rating-topic")
                           .partitions(3)
                           .replicas(1)
                           .build();
    }

    @Bean
    public NewTopic reviewRatingDltTopic() {
        return TopicBuilder.name("review-rating-topic.DLT")
                           .partitions(3)
                           .replicas(1)
                           .build();
    }

    @Bean
    public NewTopic notificationTopic() {
        return TopicBuilder.name("notification-topic")
                           .partitions(3)
                           .replicas(1)
                           .build();
    }
}
