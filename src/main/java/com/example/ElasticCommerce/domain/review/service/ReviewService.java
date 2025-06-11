package com.example.ElasticCommerce.domain.review.service;

import co.elastic.clients.elasticsearch._types.query_dsl.*;
import com.example.ElasticCommerce.domain.product.service.ProductService;
import com.example.ElasticCommerce.domain.review.dto.kafka.ProductRatingKafkaDTO;
import com.example.ElasticCommerce.domain.review.dto.kafka.ReviewElasticDTO;
import com.example.ElasticCommerce.domain.review.dto.response.ReviewResponse;
import com.example.ElasticCommerce.domain.review.dto.request.CreateReviewRequest;
import com.example.ElasticCommerce.domain.review.dto.request.UpdateReviewRequest;
import com.example.ElasticCommerce.domain.review.entity.Review;
import com.example.ElasticCommerce.domain.review.entity.ReviewDocument;
import com.example.ElasticCommerce.domain.review.exception.ReviewExceptionType;
import com.example.ElasticCommerce.domain.review.repository.*;
import com.example.ElasticCommerce.domain.review.service.kafka.ReviewKafkaProducerService;
import com.example.ElasticCommerce.global.exception.type.BadRequestException;
import com.example.ElasticCommerce.global.exception.type.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ReviewService {
    private final ReviewRepository reviewRepository;
    private final ElasticsearchOperations elasticsearchOperations;
    private final ReviewKafkaProducerService reviewKafkaProducerService;
    private final ReviewDocumentRepository reviewDocumentRepository;
    private final ProductService productService;

    public ReviewResponse getReview(Long id) {
        log.info("[리뷰조회] ID={} 조회 시작", id);
        Review review = reviewRepository.findById(id)
                                        .orElseThrow(() -> new NotFoundException(ReviewExceptionType.REVIEW_NOT_FOUND));
        log.info("[리뷰조회] ID={} 조회 완료", id);
        return ReviewResponse.from(review);
    }

    public List<ReviewResponse> getReviewsByProduct(Long productId) {
        log.info("[상품별리뷰조회] 상품ID={} 리뷰 리스트 조회 시작", productId);
        List<ReviewResponse> list = reviewRepository.findByProductId(productId).stream()
                                                    .map(ReviewResponse::from)
                                                    .collect(Collectors.toList());
        log.info("[상품별리뷰조회] 상품ID={} 리뷰 {}건 조회 완료", productId, list.size());
        return list;
    }

    @Transactional
    public ReviewResponse createReview(CreateReviewRequest req) {
        log.info("[리뷰등록] 상품ID={}, 사용자ID={} 리뷰등록 요청", req.productId(), req.userId());
        if (req.rating() < 1 || req.rating() > 5) {
            log.error("[리뷰등록] 평점 유효성 검사 실패: rating={}", req.rating());
            throw new BadRequestException(ReviewExceptionType.INVALID_RATING);
        }

        // 1) DB에 저장
        Review review = Review.builder()
                              .productId(req.productId())
                              .userId(req.userId())
                              .title(req.title())
                              .rating(req.rating())
                              .comment(req.comment())
                              .build();
        reviewRepository.save(review);

        // 2) Elasticsearch 동기 저장
        ReviewDocument doc = ReviewDocument.builder()
                                           .reviewId(review.getId().toString())
                                           .productId(review.getProductId().toString())
                                           .userId(review.getUserId().toString())
                                           .title(review.getTitle())
                                           .rating(review.getRating())
                                           .comment(review.getComment())
                                           .createdAt(review.getCreatedAt().atZone(ZoneId.of("Asia/Seoul")).toInstant())
                                           .build();
        reviewDocumentRepository.save(doc);
        log.info("[ReviewSync] Elasticsearch 동기 반영 완료: id={}", review.getId());

        // 3) 상품평점 바로 갱신
        productService.updateAverageRating(review.getProductId());
        log.info("[ReviewSync] 상품평점 동기 갱신 완료: productId={}", review.getProductId());

        return ReviewResponse.from(review);
    }

    @Transactional
    public ReviewResponse updateReview(Long id, UpdateReviewRequest req) {
        log.info("[리뷰수정] ID={} 수정 요청", id);
        if (req.rating() < 1 || req.rating() > 5) {
            log.error("[리뷰수정] 평점 유효성 검사 실패: rating={}", req.rating());
            throw new BadRequestException(ReviewExceptionType.INVALID_RATING);
        }

        Review review = reviewRepository.findById(id)
                                        .orElseThrow(() -> new NotFoundException(ReviewExceptionType.REVIEW_NOT_FOUND));
        review.update(req.title(), req.rating(), req.comment());
        reviewRepository.save(review);

        reviewKafkaProducerService.sendReview("review-topic", ReviewElasticDTO.from(review, "UPDATE"));

        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronizationAdapter() {
                    @Override
                    public void afterCommit() {
                        reviewKafkaProducerService.sendProductRating(
                                "review-rating-topic",
                                new ProductRatingKafkaDTO(review.getProductId())
                        );
                        log.info("[리뷰수정] 평점갱신 메시지 전송: productId={}", review.getProductId());
                    }
                }
        );

        log.info("[리뷰수정] ID={} 리뷰등록 완료", review.getId());
        return ReviewResponse.from(review);
    }

    @Transactional
    public void deleteReview(Long id) {
        log.info("[리뷰삭제] ID={} 삭제 요청", id);
        Review review = reviewRepository.findById(id)
                                        .orElseThrow(() -> new NotFoundException(ReviewExceptionType.REVIEW_NOT_FOUND));
        reviewRepository.delete(review);

        reviewKafkaProducerService.sendReview("review-topic", ReviewElasticDTO.from(review, "DELETE"));

        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronizationAdapter() {
                    @Override
                    public void afterCommit() {
                        reviewKafkaProducerService.sendProductRating(
                                "review-rating-topic",
                                new ProductRatingKafkaDTO(review.getProductId())
                        );
                        log.info("[리뷰삭제] 평점갱신 메시지 전송: productId={}", review.getProductId());
                    }
                }
        );

        log.info("[리뷰삭제] ID={} 리뷰삭제 완료", id);
    }

    public List<ReviewResponse> searchReviews(String query, double rating, int page, int size) {
        log.info("[리뷰검색] query='{}', 평점={}, page={}, size={} 검색 시작", query, rating, page, size);

        Query multiMatchQuery = MultiMatchQuery.of(m -> m
                .query(query)
                .fields("title", "comment")
                .fuzziness("AUTO")
        )._toQuery();

        Query ratingFilter = TermQuery.of(t -> t
                .field("rating")
                .value(rating)
        )._toQuery();

        Query boolQuery = BoolQuery.of(b -> b
                .must(multiMatchQuery)
                .filter(ratingFilter)
        )._toQuery();

        PageRequest pageRequest = PageRequest.of(
                page - 1,
                size,
                Sort.by(Sort.Order.desc("createdAt"))
        );

        NativeQuery nativeQuery = NativeQuery.builder()
                                             .withQuery(boolQuery)
                                             .withPageable(pageRequest)
                                             .build();

        SearchHits<ReviewDocument> hits = elasticsearchOperations.search(nativeQuery, ReviewDocument.class);

        List<ReviewResponse> results = hits.getSearchHits().stream()
                                           .map(hit -> {
                                               ReviewDocument doc = hit.getContent();
                                               LocalDateTime createdAt = LocalDateTime.ofInstant(doc.getCreatedAt(), ZoneId.of("Asia/Seoul"));
                                               LocalDateTime modifiedAt = doc.getModifiedAt() != null
                                                       ? LocalDateTime.ofInstant(doc.getModifiedAt(), ZoneId.of("Asia/Seoul"))
                                                       : null;
                                               return new ReviewResponse(
                                                       Long.valueOf(doc.getReviewId()),
                                                       Long.valueOf(doc.getProductId()),
                                                       Long.valueOf(doc.getUserId()),
                                                       doc.getTitle(),
                                                       doc.getRating(),
                                                       doc.getComment(),
                                                       createdAt,
                                                       modifiedAt
                                               );
                                           })
                                           .collect(Collectors.toList());

        log.info("[리뷰검색] 검색 결과 {}건 반환", results.size());
        return results;
    }
}
