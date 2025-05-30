package com.example.ElasticCommerce.domain.product.repository;

import com.example.ElasticCommerce.domain.product.entity.ProductDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface ProductDocumentRepository extends ElasticsearchRepository<ProductDocument, String> {
}
