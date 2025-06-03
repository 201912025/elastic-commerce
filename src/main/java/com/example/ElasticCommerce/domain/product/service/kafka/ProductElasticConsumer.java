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
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics           = "product-topic",
            groupId          = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory",
            concurrency      = "6"
    )
    public void consumeProduct(String message, Acknowledgment ack) {
        log.info("Kafka 컨슈머가 product 메시지를 수신했습니다.");

        // 1) JSON 파싱
        ProductElasticDTO dto;
        try {
            dto = objectMapper.readValue(message, ProductElasticDTO.class);
        } catch (JsonProcessingException e) {
            log.error("메시지 파싱 오류: {}", message, e);
            ack.acknowledge(); // 파싱 불가능 시 오프셋만 커밋
            return;
        }

        // 2) 비즈니스 로직 (Elasticsearch 저장)
        ProductDocument doc = ProductDocument.builder()
                                             .id(dto.id())
                                             .productCode(dto.productCode())
                                             .name(dto.name())
                                             .description(dto.description())
                                             .price(dto.price())
                                             .category(dto.category())
                                             .stockQuantity(dto.stockQuantity())
                                             .brand(dto.brand())
                                             .imageUrl(dto.imageUrl())
                                             .build();

        productDocumentRepository.save(doc);

        // 정상 저장 시에만 커밋
        ack.acknowledge();
        log.info("Elasticsearch 저장 성공, 오프셋 커밋: {}", dto.id());
    }
}

