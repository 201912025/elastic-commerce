package com.example.ElasticCommerce.global.config.kafka;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
@Slf4j
@Profile("!test")
public class KafkaConsumerConfig {

    @Value("${spring.kafka.consumer.group-id}")
    private String groupId;

    @Bean
    public CommonErrorHandler errorHandler(KafkaTemplate<Object, Object> kafkaTemplate) {
        // Dead Letter Queue 토픽은 원본 토픽 뒤에 ".DLT"를 붙이는 형태로 설정
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                kafkaTemplate,
                (ConsumerRecord<?, ?> record, Exception ex) ->
                        new org.apache.kafka.common.TopicPartition(
                                record.topic() + ".DLT",
                                record.partition()
                        )
        );

        // 1초 간격으로 최대 3회 재시도
        FixedBackOff backOff = new FixedBackOff(1000L, 3);

        DefaultErrorHandler handler = new DefaultErrorHandler(recoverer, backOff);

        // 재시도 시마다 로그 남기기
        handler.setRetryListeners((record, ex, attempt) ->
                log.error(
                        "Kafka 레코드 재시도 시도 #{} → topic={} / partition={} / offset={} / error={}",
                        attempt,
                        record.topic(),
                        record.partition(),
                        record.offset(),
                        ex.getMessage()
                )
        );

        return handler;
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory(
            ConsumerFactory<String, String> consumerFactory,
            CommonErrorHandler errorHandler
    ) {
        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(consumerFactory);

        // 파티션 수만큼 스레드 병렬 처리
        factory.setConcurrency(6);

        // 배치 리스닝 활성화
        factory.setBatchListener(false);

        // 수동 커밋
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);

        // 에러 핸들러 등록
        factory.setCommonErrorHandler(errorHandler);

        return factory;
    }
}

