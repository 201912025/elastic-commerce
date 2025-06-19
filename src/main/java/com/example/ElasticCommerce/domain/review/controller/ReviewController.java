package com.example.ElasticCommerce.domain.review.controller;

import com.example.ElasticCommerce.domain.review.dto.response.ReviewDetailResponse;
import com.example.ElasticCommerce.domain.review.dto.response.ReviewResponse;
import com.example.ElasticCommerce.domain.review.dto.request.CreateReviewRequest;
import com.example.ElasticCommerce.domain.review.dto.request.UpdateReviewRequest;
import com.example.ElasticCommerce.domain.review.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {
    private final ReviewService reviewService;

    @PostMapping
    public ResponseEntity<ReviewResponse> createReview(@Valid @RequestBody CreateReviewRequest request) {
        ReviewResponse reviewResponse = reviewService.createReview(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(reviewResponse);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReviewDetailResponse> getReviewById(@PathVariable Long id) {
        ReviewDetailResponse reviewDetailResponse = reviewService.getReview(id);
        return ResponseEntity.ok(reviewDetailResponse);
    }

    @GetMapping
    public ResponseEntity<List<ReviewDetailResponse>> getReviewsByProduct(@RequestParam Long productId) {
        List<ReviewDetailResponse> reviewDetailResponses = reviewService.getReviewsByProduct(productId);
        return ResponseEntity.ok(reviewDetailResponses);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ReviewResponse> updateReview(@PathVariable Long id, @Valid @RequestBody UpdateReviewRequest request) {
        ReviewResponse updateReview = reviewService.updateReview(id, request);
        return ResponseEntity.ok(updateReview);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReview(@PathVariable Long id) {
        reviewService.deleteReview(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/search")
    public ResponseEntity<List<ReviewResponse>> searchReview(
            @RequestParam String searchQuery,
            @RequestParam(defaultValue = "5.0") double rating,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        List<ReviewResponse> reviewResponses = reviewService.searchReviews(searchQuery, rating ,page, size);
        return ResponseEntity.ok(reviewResponses);
    }
}
