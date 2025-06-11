package com.example.ElasticCommerce.domain.product.service.kafka;

import com.example.ElasticCommerce.domain.product.service.ProductService;
import com.example.ElasticCommerce.domain.review.dto.kafka.ProductRatingKafkaDTO;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProductRatingConsumer {

    private final ProductService productService;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics           = "review-rating-topic",
            groupId          = "review-rating-group",
            containerFactory = "kafkaListenerContainerFactory",
            concurrency      = "3"
    )
    public void consumeRatingEvent(String message, Acknowledgment ack) {
        ProductRatingKafkaDTO productRatingKafkaDTO;
        try {
            productRatingKafkaDTO = objectMapper.readValue(message, ProductRatingKafkaDTO.class);
        } catch (JsonProcessingException e) {
            log.error("[ProductRating][PARSE_ERROR] 메시지 파싱 오류: {}", message, e);
            ack.acknowledge();
            return;
        }

        Long productId = productRatingKafkaDTO.productId();
        productService.updateAverageRating(productId);
        log.info("[상품평점갱신완료] 상품ID={} 평점 재계산 및 동기화 트리거 완료", productId);

        ack.acknowledge();
    }
}
