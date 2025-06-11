package com.example.ElasticCommerce.domain.review.repository;

import com.example.ElasticCommerce.domain.review.entity.ReviewFailedEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReviewFailedEventRepository extends JpaRepository<ReviewFailedEvent, Long> {
}
