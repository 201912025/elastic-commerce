package com.example.ElasticCommerce.domain.product.service.kafka;

import com.example.ElasticCommerce.domain.product.dto.request.ProductElasticDTO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaProducerService {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void sendProduct(String topic, ProductElasticDTO dto) {
        String msg;
        try {
            msg = objectMapper.writeValueAsString(dto);
        } catch (JsonProcessingException e) {
            log.error("ProductElasticDTO 직렬화 실패", e);
            return;
        }

        kafkaTemplate.send(topic, msg)
                     .whenComplete((result, ex) -> {
                         if (ex != null) {
                             log.error("Kafka 전송 실패", ex);
                         } else {
                             RecordMetadata meta = result.getRecordMetadata();
                             log.info("카프카 메시지 전송 성공 topic={} partition={} offset={}",
                                     meta.topic(), meta.partition(), meta.offset());
                         }
                     });
    }
}
