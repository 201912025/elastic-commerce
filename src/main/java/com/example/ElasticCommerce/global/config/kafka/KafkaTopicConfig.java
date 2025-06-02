package com.example.ElasticCommerce.global.config.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    //  토픽을 파티션 3, 브로커 3으로 생성
    @Bean
    public NewTopic productTopic() {
        return TopicBuilder.name("product-topic")
                           .partitions(3)
                           .replicas(1)
                           .build();
    }
}

