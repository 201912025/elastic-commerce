package com.example.ElasticCommerce.domain.order.controller;

import com.example.ElasticCommerce.domain.order.dto.request.CreateOrderRequest;
import com.example.ElasticCommerce.domain.order.dto.request.UpdateOrderStatusRequest;
import com.example.ElasticCommerce.domain.order.dto.response.OrderDto;
import com.example.ElasticCommerce.domain.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/users/{userId}/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderDto> createOrder(
            @PathVariable Long userId,
            @Valid @RequestBody CreateOrderRequest req,
            UriComponentsBuilder uriBuilder
    ) {
        OrderDto dto = orderService.createOrder(userId, req);
        URI location = uriBuilder
                .path("/users/{userId}/orders/{orderId}")
                .buildAndExpand(userId, dto.orderId())
                .toUri();
        return ResponseEntity.created(location).body(dto);
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderDto> getOrder(
            @PathVariable Long userId,
            @PathVariable Long orderId
    ) {
        OrderDto dto = orderService.getOrder(userId, orderId);
        return ResponseEntity.ok(dto);
    }

    @GetMapping
    public ResponseEntity<Page<OrderDto>> listOrders(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Page<OrderDto> result = orderService.listOrders(userId, page, size);
        return ResponseEntity.ok(result);
    }

    @PutMapping("/{orderId}/status")
    public ResponseEntity<OrderDto> updateStatus(
            @PathVariable Long userId,
            @PathVariable Long orderId,
            @Valid @RequestBody UpdateOrderStatusRequest req
    ) {
        OrderDto dto = orderService.updateOrderStatus(userId, orderId, req);
        return ResponseEntity.ok(dto);
    }

    @DeleteMapping("/{orderId}")
    public ResponseEntity<OrderDto> cancelOrder(
            @PathVariable Long userId,
            @PathVariable Long orderId
    ) {
        OrderDto dto = orderService.cancelOrder(userId, orderId);
        return ResponseEntity.ok(dto);
    }
}
