package com.example.ElasticCommerce.domain.review.repository;

import com.example.ElasticCommerce.domain.review.entity.ReviewDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface ReviewDocumentRepository extends ElasticsearchRepository<ReviewDocument, String> {
}
