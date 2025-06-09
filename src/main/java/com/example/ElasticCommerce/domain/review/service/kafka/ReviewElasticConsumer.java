package com.example.ElasticCommerce.domain.review.service.kafka;

import com.example.ElasticCommerce.domain.review.dto.kafka.ReviewElasticDTO;
import com.example.ElasticCommerce.domain.review.entity.ReviewDocument;
import com.example.ElasticCommerce.domain.review.repository.ReviewDocumentRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneId;

@Component
@Slf4j
@RequiredArgsConstructor
public class ReviewElasticConsumer {

    private final ReviewDocumentRepository reviewDocumentRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics           = "request-topic",
            groupId          = "request-group",
            containerFactory = "kafkaListenerContainerFactory",
            concurrency      = "6"
    )
    public void consumeReview(String message, Acknowledgment ack) {
        ReviewElasticDTO dto;
        try {
            dto = objectMapper.readValue(message, ReviewElasticDTO.class);
        } catch (JsonProcessingException e) {
            log.error("[ReviewElastic][PARSE_ERROR] 메시지 파싱 오류: {}", message, e);
            ack.acknowledge();
            return;
        }

        String id        = dto.id();
        String eventType = dto.eventType();
        Instant createdAtInstant  = dto.createdAt()
                                       .atZone(ZoneId.of("Asia/Seoul"))
                                       .toInstant();
        Instant modifiedAtInstant = dto.modifiedAt() != null
                ? dto.modifiedAt().atZone(ZoneId.of("Asia/Seoul")).toInstant()
                : null;

        if ("DELETE".equals(eventType)) {
            reviewDocumentRepository.deleteById(id);
            log.info("[ReviewElastic][DELETE] 문서 삭제 완료: id={}", id);

        } else {
            // CREATE / UPDATE / OPEN / CLOSE / UPDATE_STOCK / UPDATE_RATING 등 모든 비-DELETE 이벤트: upsert
            ReviewDocument reviewDocument = ReviewDocument.builder()
                                                          .reviewId(dto.id())
                                                          .productId(dto.productId().toString())
                                                          .userId(dto.userId().toString())
                                                          .title(dto.title())
                                                          .rating(dto.rating())
                                                          .comment(dto.comment())
                                                          .createdAt(createdAtInstant)
                                                          .modifiedAt(modifiedAtInstant)
                                                          .build();

            reviewDocumentRepository.save(reviewDocument);
            log.info("[ReviewElastic][UPSERT][{}] 처리 완료: id={}", eventType, id);
        }

        ack.acknowledge();
        log.info("[ReviewElastic] 오프셋 커밋: {}", id);
    }
}
