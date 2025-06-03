package com.example.ElasticCommerce.domain.product.service;

import co.elastic.clients.elasticsearch._types.query_dsl.*;
import com.example.ElasticCommerce.domain.product.dto.request.CreateProductRequestDTO;
import com.example.ElasticCommerce.domain.product.dto.request.ProductElasticDTO;
import com.example.ElasticCommerce.domain.product.dto.request.UpdateProductRequestDTO;
import com.example.ElasticCommerce.domain.product.dto.response.ProductResponse;
import com.example.ElasticCommerce.domain.product.entity.Product;
import com.example.ElasticCommerce.domain.product.entity.ProductDocument;
import com.example.ElasticCommerce.domain.product.exception.ProductExceptionType;
import com.example.ElasticCommerce.domain.product.repository.ProductRepository;
import com.example.ElasticCommerce.domain.product.service.kafka.KafkaProducerService;
import com.example.ElasticCommerce.global.exception.type.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.HighlightQuery;
import org.springframework.data.elasticsearch.core.query.highlight.Highlight;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightField;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final ElasticsearchOperations elasticsearchOperations;
    private final KafkaProducerService kafkaProducerService;

    public List<ProductResponse> getProducts(int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size);
        List<Product> products = productRepository.findAll(pageable).getContent();
        return products.stream()
                .map(ProductResponse::from)
                .collect(Collectors.toList());
    }

    public List<String> getSuggestions(String query) {
        Query multiMatchQuery = MultiMatchQuery.of(m -> m
                .query(query)
                .type(TextQueryType.BoolPrefix)
                .fields("name.auto_complete", "name.auto_complete._index_prefix","name.auto_complete._2gram", "name.auto_complete._3gram")
        )._toQuery();

        NativeQuery nativeQuery = NativeQuery.builder()
                                             .withQuery(multiMatchQuery)
                                             .withPageable(PageRequest.of(0, 5))
                                             .build();

        SearchHits<ProductDocument> searchHits = this.elasticsearchOperations.search(nativeQuery, ProductDocument.class);

        return searchHits.getSearchHits().stream()
                         .map(hit -> {
                             ProductDocument productDocument = hit.getContent();
                             System.out.println(productDocument);
                             return productDocument.getName();
                         })
                         .toList();
    }

    public List<ProductResponse> searchProducts(
            String query,
            String category,
            double minPrice,
            double maxPrice,
            int page,
            int size
    ) {
        Query multiMatchQuery = MultiMatchQuery.of(m -> m
                .query(query)
                .fields("name^3", "description^1", "category^2")
                .fuzziness("AUTO")
        )._toQuery();

        List<Query> filters = new ArrayList<>();
        // term filter 쿼리 : 카테고리가 정확히 일치하는 것만 필터링
        if (category != null && !category.isEmpty()) {
            Query categoryFilter = TermQuery.of(t -> t
                    .field("category.raw")
                    .value(category)
            )._toQuery();
            filters.add(categoryFilter);
        }

        // range filter: 가격 범위 필터
        Query priceRangeFilter = NumberRangeQuery.of(r -> r
                .field("price")
                .gte(minPrice)
                .lte(maxPrice)
        )._toRangeQuery()._toQuery();
        filters.add(priceRangeFilter);

        Query ratingShould = NumberRangeQuery.of(r -> r
                .field("rating")
                .gt(4.0)
        )._toRangeQuery()._toQuery();

        Query boolQuery = BoolQuery.of(b -> b
                .must(multiMatchQuery)
                .filter(filters)
                .should(ratingShould)
        )._toQuery();

        HighlightParameters highlightParameters = HighlightParameters.builder()
                .withPreTags("<b>")
                .withPostTags("</b>")
                .build();
        Highlight highlight = new Highlight(highlightParameters, List.of(new HighlightField("name")));
        HighlightQuery highlightQuery = new HighlightQuery(highlight, ProductDocument.class);

        NativeQuery nativeQuery = NativeQuery.builder()
                .withQuery(boolQuery)
                .withHighlightQuery(highlightQuery)
                .withPageable(PageRequest.of(page - 1, size))
                .build();

        SearchHits<ProductDocument> searchHits = this.elasticsearchOperations.search(nativeQuery, ProductDocument.class);
        List<ProductDocument> productDocuments = searchHits.getSearchHits().stream()
                                                           .map(hit -> {
                                                               ProductDocument doc = hit.getContent();

                                                               // 1) 전체 하이라이트 맵을 꺼내고
                                                               Map<String, List<String>> highlightFields = hit.getHighlightFields();

                                                               // 2) "name" 키가 있고, 리스트가 비어있지 않은지 확인
                                                               if (highlightFields.containsKey("name")
                                                                       && highlightFields.get("name") != null
                                                                       && !highlightFields.get("name").isEmpty()) {

                                                                   // 3) 첫 번째 하이라이트 스트링으로 세팅
                                                                   String highlightedName = highlightFields.get("name").get(0);
                                                                   doc.highlighting(highlightedName);
                                                               }
                                                               // 없으면 그냥 원본 name 유지
                                                               return doc;
                                                           })
                                                           .toList();

        return productDocuments.stream()
                       .map(ProductResponse::from)
                       .collect(Collectors.toList());
    }

    @Transactional
    public ProductResponse createProduct(CreateProductRequestDTO createProductRequestDTO) {
        String rawUuid = UUID.randomUUID().toString().replace("-", "");
        String productCode = rawUuid.substring(0, 8).toUpperCase();

        Product product = Product.builder()
                                         .productCode(productCode)
                                         .name(createProductRequestDTO.name())
                                         .category(createProductRequestDTO.category())
                                         .stockQuantity(createProductRequestDTO.stockQuantity())
                                         .brand(createProductRequestDTO.brand())
                                         .imageUrl(createProductRequestDTO.imageUrl())
                                         .description(createProductRequestDTO.description())
                                         .price(createProductRequestDTO.price())
                                         .build();
        productRepository.save(product);

        ProductResponse productResponse = ProductResponse.from(product);
        kafkaProducerService.sendProduct("product-topic", ProductElasticDTO.from(productResponse));

        return productResponse;
    }

    public ProductResponse getProductById(Long productId) {
        Product product = productRepository.findById(productId)
                                           .orElseThrow(() -> new NotFoundException(ProductExceptionType.PRODUCT_NOT_FOUND));
        return ProductResponse.from(product);
    }

    @Transactional
    public ProductResponse updateProduct(Long productId, UpdateProductRequestDTO updateProductRequestDTO) {
        Product product = productRepository.findById(productId)
                                           .orElseThrow(() -> new NotFoundException(ProductExceptionType.PRODUCT_NOT_FOUND));

        product.update(updateProductRequestDTO);

        return ProductResponse.from(product);
    }

}
