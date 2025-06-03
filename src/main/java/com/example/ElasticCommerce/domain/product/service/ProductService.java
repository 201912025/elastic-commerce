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
import com.example.ElasticCommerce.global.exception.type.BadRequestException;
import com.example.ElasticCommerce.global.exception.type.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final ElasticsearchOperations elasticsearchOperations;
    private final KafkaProducerService kafkaProducerService;

    public List<ProductResponse> getProducts(int page, int size) {
        log.info("[Service][GET_PRODUCTS] page={}, size={}", page, size);
        Pageable pageable = PageRequest.of(page - 1, size);
        List<Product> products = productRepository.findAll(pageable).getContent();
        return products.stream()
                       .map(ProductResponse::from)
                       .collect(Collectors.toList());
    }

    public List<String> getSuggestions(String query) {
        log.info("[Service][GET_SUGGESTIONS] query={}", query);
        Query multiMatchQuery = MultiMatchQuery.of(m -> m
                .query(query)
                .type(TextQueryType.BoolPrefix)
                .fields("name.auto_complete", "name.auto_complete._index_prefix", "name.auto_complete._2gram", "name.auto_complete._3gram")
        )._toQuery();

        NativeQuery nativeQuery = NativeQuery.builder()
                                             .withQuery(multiMatchQuery)
                                             .withPageable(PageRequest.of(0, 5))
                                             .build();

        SearchHits<ProductDocument> searchHits = this.elasticsearchOperations.search(nativeQuery, ProductDocument.class);

        return searchHits.getSearchHits().stream()
                         .map(hit -> hit.getContent().getName())
                         .collect(Collectors.toList());
    }

    public List<ProductResponse> searchProducts(
            String query,
            String category,
            double minPrice,
            double maxPrice,
            int page,
            int size
    ) {
        log.info("[Service][SEARCH_PRODUCTS] query={}, category={}, minPrice={}, maxPrice={}, page={}, size={}",
                query, category, minPrice, maxPrice, page, size);

        Query multiMatchQuery = MultiMatchQuery.of(m -> m
                .query(query)
                .fields("name^3", "description^1", "category^2")
                .fuzziness("AUTO")
        )._toQuery();

        List<Query> filters = new ArrayList<>();
        if (category != null && !category.isEmpty()) {
            Query categoryFilter = TermQuery.of(t -> t
                    .field("category.raw")
                    .value(category)
            )._toQuery();
            filters.add(categoryFilter);
        }

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
                                                               Map<String, List<String>> highlightFields = hit.getHighlightFields();
                                                               if (highlightFields.containsKey("name")
                                                                       && highlightFields.get("name") != null
                                                                       && !highlightFields.get("name").isEmpty()) {
                                                                   String highlightedName = highlightFields.get("name").get(0);
                                                                   doc.highlighting(highlightedName);
                                                               }
                                                               return doc;
                                                           })
                                                           .collect(Collectors.toList());

        return productDocuments.stream()
                               .map(ProductResponse::from)
                               .collect(Collectors.toList());
    }

    @Transactional
    public ProductResponse createProduct(CreateProductRequestDTO createProductRequestDTO) {
        log.info("[Service][CREATE_PRODUCT] name={}, category={}, stockQuantity={}, price={}",
                createProductRequestDTO.name(),
                createProductRequestDTO.category(),
                createProductRequestDTO.stockQuantity(),
                createProductRequestDTO.price());

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
        kafkaProducerService.sendProduct("product-topic", ProductElasticDTO.from(product, "CREATE"));
        log.info("[Service][CREATE_PRODUCT] Kafka 이벤트 전송 완료: eventType=CREATE, id={}", product.getId());

        return productResponse;
    }

    public ProductResponse getProductById(Long productId) {
        log.info("[Service][GET_PRODUCT_BY_ID] productId={}", productId);
        Product product = productRepository.findById(productId)
                                           .orElseThrow(() -> new NotFoundException(ProductExceptionType.PRODUCT_NOT_FOUND));
        return ProductResponse.from(product);
    }

    @Transactional
    public ProductResponse updateProduct(Long productId, UpdateProductRequestDTO updateProductRequestDTO) {
        log.info("[Service][UPDATE_PRODUCT] productId={}, fields={}", productId, updateProductRequestDTO);
        Product product = productRepository.findById(productId)
                                           .orElseThrow(() -> new NotFoundException(ProductExceptionType.PRODUCT_NOT_FOUND));
        product.update(updateProductRequestDTO);

        kafkaProducerService.sendProduct("product-topic", ProductElasticDTO.from(product, "UPDATE"));
        log.info("[Service][UPDATE_PRODUCT] Kafka 이벤트 전송 완료: eventType=UPDATE, id={}", productId);

        return ProductResponse.from(product);
    }

    @Transactional
    public void deleteProduct(Long productId) {
        log.info("[Service][DELETE_PRODUCT] productId={}", productId);
        Product product = productRepository.findById(productId)
                                           .orElseThrow(() -> new NotFoundException(ProductExceptionType.PRODUCT_NOT_FOUND));

        kafkaProducerService.sendProduct("product-topic", ProductElasticDTO.from(product, "DELETE"));
        log.info("[Service][DELETE_PRODUCT] Kafka 이벤트 전송 완료: eventType=DELETE, id={}", productId);

        productRepository.deleteById(productId);
        log.info("[Service][DELETE_PRODUCT] DB 삭제 완료: id={}", productId);
    }

    @Transactional
    public ProductResponse updateStock(Long productId, Integer newStockQuantity) {
        log.info("[Service][UPDATE_STOCK] productId={}, newStockQuantity={}", productId, newStockQuantity);
        Product product = productRepository.findById(productId)
                                           .orElseThrow(() -> new NotFoundException(ProductExceptionType.PRODUCT_NOT_FOUND));

        if (newStockQuantity < 0) {
            log.error("[Service][UPDATE_STOCK] 재고는 0보다 작을 수 없습니다: {}", newStockQuantity);
            throw new BadRequestException(ProductExceptionType.INVALID_STOCK_QUANTITY);
        }
        product.updateStockQuantity(newStockQuantity);
        if (newStockQuantity == 0) {
            product.closeProduct();
            log.info("[Service][UPDATE_STOCK] 재고 초과로 상품 품절: id={}", productId);
        }

        kafkaProducerService.sendProduct("product-topic", ProductElasticDTO.from(product, "UPDATE_STOCK"));
        log.info("[Service][UPDATE_STOCK] Kafka 이벤트 전송 완료: eventType=UPDATE_STOCK, id={}", productId);

        return ProductResponse.from(product);
    }

    @Transactional
    public ProductResponse openProduct(Long productId) {
        log.info("[Service][OPEN_PRODUCT] productId={}", productId);
        Product product = productRepository.findById(productId)
                                           .orElseThrow(() -> new NotFoundException(ProductExceptionType.PRODUCT_NOT_FOUND));

        product.openProduct();
        kafkaProducerService.sendProduct("product-topic", ProductElasticDTO.from(product, "OPEN"));
        log.info("[Service][OPEN_PRODUCT] Kafka 이벤트 전송 완료: eventType=OPEN, id={}", productId);

        return ProductResponse.from(product);
    }

    @Transactional
    public ProductResponse closeProduct(Long productId) {
        log.info("[Service][CLOSE_PRODUCT] productId={}", productId);
        Product product = productRepository.findById(productId)
                                           .orElseThrow(() -> new NotFoundException(ProductExceptionType.PRODUCT_NOT_FOUND));

        product.closeProduct();
        kafkaProducerService.sendProduct("product-topic", ProductElasticDTO.from(product, "CLOSE"));
        log.info("[Service][CLOSE_PRODUCT] Kafka 이벤트 전송 완료: eventType=CLOSE, id={}", productId);

        return ProductResponse.from(product);
    }

    @Transactional
    public void updateProductRating(Long productId, double newAvgRating) {
        log.info("[Service][UPDATE_PRODUCT_RATING] productId={}, newAvgRating={}", productId, newAvgRating);
        Product product = productRepository.findById(productId)
                                           .orElseThrow(() -> new NotFoundException(ProductExceptionType.PRODUCT_NOT_FOUND));

        product.updateRating(newAvgRating);
        kafkaProducerService.sendProduct("product-topic", ProductElasticDTO.from(product, "UPDATE_RATING"));
        log.info("[Service][UPDATE_PRODUCT_RATING] Kafka 이벤트 전송 완료: eventType=UPDATE_RATING, id={}", productId);
    }
}
