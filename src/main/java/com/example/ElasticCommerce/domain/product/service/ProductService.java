package com.example.ElasticCommerce.domain.product.service;

import com.example.ElasticCommerce.domain.product.dto.response.ProductResponse;
import com.example.ElasticCommerce.domain.product.entity.Product;
import com.example.ElasticCommerce.domain.product.repository.ProductDocumentRepository;
import com.example.ElasticCommerce.domain.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductDocumentRepository productDocumentRepository;
    private final ElasticsearchOperations elasticsearchOperations;


    public List<ProductResponse> getProducts(int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size);
        List<Product> products = productRepository.findAll(pageable).getContent();
        return products.stream()
                .map(ProductResponse::from)
                .collect(Collectors.toList());
    }
}
