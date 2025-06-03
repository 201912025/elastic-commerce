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
}
