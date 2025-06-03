package com.example.ElasticCommerce.domain.product.service.kafka;

import com.example.ElasticCommerce.domain.product.dto.request.ProductElasticDTO;
import com.example.ElasticCommerce.domain.product.entity.FailedEvent;
import com.example.ElasticCommerce.domain.product.entity.ProductDocument;
import com.example.ElasticCommerce.domain.product.repository.FailedEventRepository;
import com.example.ElasticCommerce.domain.product.repository.ProductDocumentRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Slf4j
@RequiredArgsConstructor
public class ProductElasticDltConsumer {

    private final ProductDocumentRepository productDocumentRepository;
    private final FailedEventRepository failedEventRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics           = "product-topic.DLT",
            groupId          = "${spring.kafka.consumer.group-id}.dlt",
            containerFactory = "kafkaListenerContainerFactory",
            concurrency      = "6"
    )
    @Transactional
    public void consumeDlt(String message, Acknowledgment ack) {
        log.info("[DLT] 메시지 수신: {}", message);

        // 1) 실패 메시지를 다시 DTO로 파싱
        ProductElasticDTO dto;
        try {
            dto = objectMapper.readValue(message, ProductElasticDTO.class);
        } catch (JsonProcessingException e) {
            log.error("[DLT] JSON 파싱 실패, 메시지 버림: {}", message, e);

            FailedEvent failedEvent = FailedEvent.builder()
                                                 .payload(message)
                                                 .topic("product-topic.DLT")
                                                 .errorMessage(truncate(e.getMessage(), 500))
                                                 .retryCount(0) // 파싱 단계부터 실패했으므로 재시도 카운트를 0으로 설정
                                                 .build();
            failedEventRepository.save(failedEvent);

            // 파싱이 불가하면 재시도 의미가 없으니 DLT도 버리고 오프셋만 커밋
            ack.acknowledge();
            return;
        }

        // 2) Elasticsearch 저장 재시도
        try {
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

            // 재처리 성공: 오프셋 커밋
            ack.acknowledge();
            log.info("[DLT] 재처리 성공, 오프셋 커밋: {}", dto.id());

        } catch (Exception ex) {
            log.error("[DLT] 재처리 중 예외 발생: {}", message, ex);

            // 여기서 FailedEvent 엔티티로 오류 정보 저장
            FailedEvent failedEvent = FailedEvent.builder()
                                                 .payload(message)
                                                 .topic("product-topic.DLT")
                                                 .errorMessage(truncate(ex.getMessage(), 500))
                                                 .retryCount(1) // DLT 재처리이므로 1로 시작
                                                 .build();
            failedEventRepository.save(failedEvent);

            // 이 메시지는 더 이상 재시도하지 않도록 오프셋만 커밋
            ack.acknowledge();
        }
    }

    private String truncate(String message, int maxLen) {
        if (message == null) {
            return null;
        }
        return message.length() <= maxLen ? message : message.substring(0, maxLen);
    }
}
