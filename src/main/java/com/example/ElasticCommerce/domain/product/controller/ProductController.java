package com.example.ElasticCommerce.domain.product.controller;

import com.example.ElasticCommerce.domain.product.dto.request.CreateProductRequestDTO;
import com.example.ElasticCommerce.domain.product.dto.request.StockUpdateRequestDTO;
import com.example.ElasticCommerce.domain.product.dto.request.UpdateProductRequestDTO;
import com.example.ElasticCommerce.domain.product.dto.response.ProductResponse;
import com.example.ElasticCommerce.domain.product.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public ResponseEntity<List<ProductResponse>> getProducts(@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(productService.getProducts(page, size));
    }

    @GetMapping("/search")
    public ResponseEntity<List<ProductResponse>> searchProducts(
            @RequestParam String query,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0") double minPrice,
            @RequestParam(defaultValue = "1000000000") double maxPrice,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        List<ProductResponse> products = productService.searchProducts(query, category, minPrice, maxPrice, page, size);
        return ResponseEntity.ok(products);
    }

    @GetMapping("/suggestions")
    public ResponseEntity<List<String>> getSuggestions(@RequestParam String query) {
        List<String> suggestions = productService.getSuggestions(query);
        return ResponseEntity.ok(suggestions);
    }

    @PostMapping
    public ResponseEntity<ProductResponse> createProduct(@RequestBody @Valid CreateProductRequestDTO createProductRequestDTO) {
        ProductResponse productResponse = productService.createProduct(createProductRequestDTO);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(productResponse.id())
                .toUri();

        return ResponseEntity.created(location).body(productResponse);
    }

    @GetMapping("/{productId}")
    public ResponseEntity<ProductResponse> getProductById(@PathVariable Long productId) {
        ProductResponse response = productService.getProductById(productId);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{productId}")
    public ResponseEntity<ProductResponse> updateProduct(
            @PathVariable Long productId,
            @RequestBody @Valid UpdateProductRequestDTO updateRequest
    ) {
        ProductResponse updated = productService.updateProduct(productId, updateRequest);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long productId) {
        productService.deleteProduct(productId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{productId}/stock")
    public ResponseEntity<ProductResponse> updateStock(
            @PathVariable Long productId,
            @RequestBody @Valid StockUpdateRequestDTO stockUpdate
    ) {
        ProductResponse updated = productService.updateStock(productId, stockUpdate.stockQuantity());
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/{productId}/open")
    public ResponseEntity<ProductResponse> openProduct(@PathVariable Long productId) {
        ProductResponse response = productService.openProduct(productId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{productId}/close")
    public ResponseEntity<ProductResponse> closeProduct(@PathVariable Long productId) {
        ProductResponse response = productService.closeProduct(productId);
        return ResponseEntity.ok(response);
    }
}
