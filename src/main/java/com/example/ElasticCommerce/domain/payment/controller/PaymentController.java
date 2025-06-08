package com.example.ElasticCommerce.domain.payment.controller;

import com.example.ElasticCommerce.domain.payment.dto.request.PaymentRequest;
import com.example.ElasticCommerce.domain.payment.dto.response.PaymentDto;
import com.example.ElasticCommerce.domain.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users/{userId}/orders/{orderId}/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<PaymentDto> createPayment(
            @PathVariable Long userId,
            @PathVariable Long orderId,
            @RequestBody PaymentRequest paymentRequest
    ) {
        PaymentDto dto = paymentService.createPayment(userId, orderId, paymentRequest);
        return ResponseEntity.ok(dto);
    }

    @GetMapping
    public ResponseEntity<PaymentDto> getPayment(
            @PathVariable Long userId,
            @PathVariable Long orderId
    ) {
        PaymentDto dto = paymentService.getPayment(userId, orderId);
        return ResponseEntity.ok(dto);
    }
}
