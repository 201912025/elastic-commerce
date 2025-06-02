package com.example.ElasticCommerce.domain.product.service;

import com.example.ElasticCommerce.domain.product.dto.request.ProductElasticDTO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaProducerService {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final ThreadPoolTaskExecutor kafkaExecutor;

    public void sendProduct(String topic, ProductElasticDTO productElasticDTO) {
        kafkaExecutor.execute(() -> {
            String msg;
            try {
                msg = objectMapper.writeValueAsString(productElasticDTO);
            } catch (JsonProcessingException e) {
                log.error("ProductResponse 직렬화 실패", e);
                return;
            }

            // send()가 반환하는 CompletableFuture를 그대로 사용
            kafkaTemplate.send(topic, msg)
                         .whenComplete((result, ex) -> {
                             if (ex != null) {
                                 // 전송 실패 핸들링
                                 log.error("Kafka 전송 실패", ex);
                             } else {
                                 // 전송 성공 처리
                                 RecordMetadata meta = result.getRecordMetadata();
                                 log.debug("전송 성공 topic={} partition={} offset={}",
                                         meta.topic(), meta.partition(), meta.offset());
                             }
                         });
        });
    }
}
