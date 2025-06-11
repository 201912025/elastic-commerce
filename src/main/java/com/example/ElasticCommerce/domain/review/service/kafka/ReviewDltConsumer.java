package com.example.ElasticCommerce.domain.review.service.kafka;

import com.example.ElasticCommerce.domain.review.dto.kafka.ReviewElasticDTO;
import com.example.ElasticCommerce.domain.review.entity.ReviewDocument;
import com.example.ElasticCommerce.domain.review.entity.ReviewFailedEvent;
import com.example.ElasticCommerce.domain.review.repository.ReviewDocumentRepository;
import com.example.ElasticCommerce.domain.review.repository.ReviewFailedEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneId;

@Component
@Slf4j
@RequiredArgsConstructor
public class ReviewDltConsumer {

    private static final int MAX_PAYLOAD_LENGTH = 1000;

    private final ObjectMapper objectMapper;
    private final ReviewDocumentRepository documentRepository;
    private final ReviewFailedEventRepository failedEventRepository;

    @KafkaListener(
            topics           = "review-topic.DLT",
            groupId          = "review-group.dlt",
            containerFactory = "kafkaListenerContainerFactory",
            concurrency      = "6"
    )
    @Transactional
    public void consumeReviewDlt(String message, Acknowledgment ack) {
        String truncated = truncate(message, 200);
        log.info("[DLT] 메시지 수신: {}", truncated);

        ReviewElasticDTO dto;
        try {
            dto = objectMapper.readValue(message, ReviewElasticDTO.class);
        } catch (JsonProcessingException e) {
            log.error("[DLT][PARSE_ERROR] JSON 파싱 실패", e);
            recordFailure(message, 0, e.getMessage());
            ack.acknowledge();
            return;
        }

        String eventType = dto.eventType();
        String id        = dto.id();
        Instant createdAtInstant  = dto.createdAt().atZone(ZoneId.of("Asia/Seoul")).toInstant();
        Instant modifiedAtInstant = dto.modifiedAt() != null
                ? dto.modifiedAt().atZone(ZoneId.of("Asia/Seoul")).toInstant()
                : null;

        try {
            if ("DELETE".equalsIgnoreCase(eventType)) {
                documentRepository.deleteById(id);
                log.info("[DLT][DELETE] 문서 삭제 완료: id={}", id);

            } else {
                ReviewDocument doc = ReviewDocument.builder()
                                                   .reviewId(id)
                                                   .productId(dto.productId().toString())
                                                   .userId(dto.userId().toString())
                                                   .title(dto.title())
                                                   .rating(dto.rating())
                                                   .comment(dto.comment())
                                                   .createdAt(createdAtInstant)
                                                   .modifiedAt(modifiedAtInstant)
                                                   .build();

                documentRepository.save(doc);
                log.info("[DLT][UPSERT][{}] 처리 완료: id={}", eventType, id);
            }

        } catch (Exception ex) {
            log.error("[DLT][PROCESS_ERROR] 재처리 중 예외 발생: eventType={}, id={}", eventType, id, ex);
            recordFailure(message, 1, ex.getMessage());
        } finally {
            ack.acknowledge();
            log.info("[DLT] 오프셋 커밋: id={}", id);
        }
    }

    private void recordFailure(String payload, int retryCount, String errorMsg) {
        ReviewFailedEvent event = ReviewFailedEvent.builder()
                                                   .payload(truncate(payload, MAX_PAYLOAD_LENGTH))
                                                   .topic("review-topic.DLT")
                                                   .errorMessage(truncate(errorMsg, 500))
                                                   .retryCount(retryCount)
                                                   .build();
        failedEventRepository.save(event);
    }

    private String truncate(String msg, int maxLen) {
        if (msg == null) return null;
        return msg.length() <= maxLen ? msg : msg.substring(0, maxLen);
    }
}
