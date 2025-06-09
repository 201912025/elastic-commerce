package com.example.ElasticCommerce.domain.review.controller;

import com.example.ElasticCommerce.domain.review.dto.response.ReviewResponse;
import com.example.ElasticCommerce.domain.review.dto.request.CreateReviewRequest;
import com.example.ElasticCommerce.domain.review.dto.request.UpdateReviewRequest;
import com.example.ElasticCommerce.domain.review.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {
    private final ReviewService service;

    @PostMapping
    public ResponseEntity<ReviewResponse> createReview(@RequestBody CreateReviewRequest req) {
        ReviewResponse created = service.createReview(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReviewResponse> getReviewById(@PathVariable Long id) {
        ReviewResponse found = service.getReview(id);
        return ResponseEntity.ok(found);
    }

    @GetMapping
    public ResponseEntity<List<ReviewResponse>> getReviewsByProduct(@RequestParam Long productId) {
        List<ReviewResponse> list = service.getReviewsByProduct(productId);
        return ResponseEntity.ok(list);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ReviewResponse> updateReview(@PathVariable Long id, @RequestBody UpdateReviewRequest req) {
        ReviewResponse updated = service.updateReview(id, req);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReview(@PathVariable Long id) {
        service.deleteReview(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/search")
    public ResponseEntity<List<ReviewResponse>> searchReview(
            @RequestParam String searchQuery,
            @RequestParam(defaultValue = "5.0") double rating,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        List<ReviewResponse> results = service.searchReviews(searchQuery, rating ,page, size);
        return ResponseEntity.ok(results);
    }
}
