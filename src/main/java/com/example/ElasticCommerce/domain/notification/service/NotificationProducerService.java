package com.example.ElasticCommerce.domain.notification.service;

import com.example.ElasticCommerce.domain.notification.dto.NotificationRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationProducerService {

    private final KafkaTemplate<String, NotificationRequest> kafkaTemplate;

    private static final String TOPIC = "notification-topic";

    public Mono<Void> sendAll(NotificationRequest req) {
        return Mono.<Void>create(sink -> {
            kafkaTemplate.send(TOPIC, req)
                         .whenComplete((result, ex) -> {
                             if (ex != null) {
                                 log.error("Kafka 알림 발행 실패: {}", ex.getMessage(), ex);
                                 sink.error(ex);
                             } else {
                                 var metadata = result.getRecordMetadata();
                                 log.info("Kafka 알림 발행 성공: topic={}, partition={}, offset={}",
                                         metadata.topic(), metadata.partition(), metadata.offset());
                                 sink.success();
                             }
                         });
        });
    }
}
