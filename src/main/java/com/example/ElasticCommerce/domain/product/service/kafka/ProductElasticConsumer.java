package com.example.ElasticCommerce.domain.product.service.kafka;

import com.example.ElasticCommerce.domain.product.dto.request.ProductElasticDTO;
import com.example.ElasticCommerce.domain.product.entity.ProductDocument;
import com.example.ElasticCommerce.domain.product.repository.ProductDocumentRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class ProductElasticConsumer {

    private final ProductDocumentRepository productDocumentRepository;
    private final ObjectMapper              objectMapper;

    @KafkaListener(
            topics           = "product-topic",
            groupId          = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory",
            concurrency      = "6"
    )
    public void consumeProduct(String message, Acknowledgment ack) {
        ProductElasticDTO dto;
        try {
            dto = objectMapper.readValue(message, ProductElasticDTO.class);
        } catch (JsonProcessingException e) {
            log.error("[Elasticsearch][PARSE_ERROR] 메시지 파싱 오류: {}", message, e);
            ack.acknowledge();
            return;
        }

        String id        = dto.id();
        String eventType = dto.eventType();

        if ("DELETE".equals(eventType)) {
            productDocumentRepository.deleteById(id);
            log.info("[Elasticsearch][DELETE] 문서 삭제 완료: id={}", id);

        } else {
            // CREATE / UPDATE / OPEN / CLOSE / UPDATE_STOCK / UPDATE_RATING 등 모든 비-DELETE 이벤트: upsert
            ProductDocument doc = ProductDocument.builder()
                                                 .id(id)
                                                 .productCode(dto.productCode())
                                                 .name(dto.name())
                                                 .description(dto.description())
                                                 .price(dto.price())
                                                 .category(dto.category())
                                                 .stockQuantity(dto.stockQuantity())
                                                 .brand(dto.brand())
                                                 .imageUrl(dto.imageUrl())
                                                 .available(dto.available())
                                                 .rating(dto.rating())
                                                 .build();

            productDocumentRepository.save(doc);
            log.info("[Elasticsearch][UPSERT][{}] 처리 완료: id={}", eventType, id);
        }

        ack.acknowledge();
        log.info("[Elasticsearch] 오프셋 커밋: {}", id);
    }
}
