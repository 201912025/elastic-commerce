package com.example.ElasticCommerce.domain.product.service;

import com.example.ElasticCommerce.domain.product.dto.request.ProductElasticDTO;
import com.example.ElasticCommerce.domain.product.entity.ProductDocument;
import com.example.ElasticCommerce.domain.product.repository.ProductDocumentRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;


@Component
@Slf4j
@RequiredArgsConstructor
public class ProductElasticConsumer {

    private final ProductDocumentRepository productDocumentRepository;
    private final ObjectMapper objectMapper;
    private final ThreadPoolTaskExecutor threadPoolTaskExecutor;

    @KafkaListener(
            topics = "product-topic",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeProduct(String message, Acknowledgment ack) {
        log.info("Kafka 컨슈머가 product 메시지를 수신했습니다.");

        threadPoolTaskExecutor.execute(() -> {
            // 1) JSON 파싱
            ProductElasticDTO dto;
            try {
                dto = objectMapper.readValue(message, ProductElasticDTO.class);
            } catch (JsonProcessingException e) {
                log.error("메시지 파싱 오류: {}", message, e);
                ack.acknowledge(); // 파싱 자체가 불가능하면, 이 레코드는 재시도 의미가 없으므로 커밋
                return;
            }

            // 2) 비즈니스 로직 (Elasticsearch 저장)
            try {
                ProductDocument doc = ProductDocument.builder()
                                                     .id(dto.id())
                                                     .productCode(dto.productCode())
                                                     .name(dto.name())
                                                     .description(dto.description())
                                                     .price(dto.price())
                                                     .build();
                productDocumentRepository.save(doc);

                // 정상 저장 시점에만 커밋
                ack.acknowledge();
                log.info("메시지 처리 완료, 오프셋 커밋함: {}", dto.id());

            } catch (Exception ex) {
                log.error("Elasticsearch 저장 실패, ErrorHandler로 위임: {}", message, ex);
                // 재시도/Dead Letter 처리를 위해 예외를 던집니다.
                throw ex;
            }
        });
    }
}

