package com.example.ElasticCommerce.domain.product.controller;

import com.example.ElasticCommerce.domain.product.dto.response.ProductResponse;
import com.example.ElasticCommerce.domain.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping("/productId")
    public ResponseEntity<List<ProductResponse>> getProducts(@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(productService.getProducts(page, size));
    }
}
