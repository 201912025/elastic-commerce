package com.example.ElasticCommerce.domain.notification;

import com.example.ElasticCommerce.domain.notification.dto.NotificationRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationClient {

    @Value("${notification.service.base-url}")
    private String baseUrl;

    private final WebClient.Builder webClientBuilder;

    public Mono<Void> sendOrderNotification(Long orderId, String type, String userEmail, Long totalPrice) {
        String url = baseUrl + "/api/send";
        NotificationRequest payload = new NotificationRequest(orderId, type, userEmail, totalPrice);
        return webClientBuilder.build()
                               .post()
                               .uri(url)
                               .bodyValue(payload)
                               .retrieve()
                               .bodyToMono(Void.class)
                               .doOnError(e -> log.error("Notification failed for order {}: {}", orderId, e.getMessage()));
    }
}
