package com.example.ElasticCommerce.global.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthCheckController {
    @GetMapping("/")
    public String healthCheck() {
        return "the service is up and running ...";
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Service is healthy");
    }

    @GetMapping("/api/health")
    public ResponseEntity<String> checkApiHealth() {
        return ResponseEntity.ok("server is running test");
    }
}
