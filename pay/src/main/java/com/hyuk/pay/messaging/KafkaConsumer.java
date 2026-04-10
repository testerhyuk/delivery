package com.hyuk.pay.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hyuk.pay.service.PayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaConsumer {
    private final ObjectMapper objectMapper;
    private final PayService payService;

    @KafkaListener(topics = "order-events.public.order_outbox", groupId = "pay-service-group")
    public void onOrderEvent(String message) {
        try {
            JsonNode payload = objectMapper.readTree(message).path("payload").path("after");

            if (payload.isMissingNode() || payload.isNull()) {
                throw new RuntimeException("order-events.public.order_outbox.payload is null");
            }

            String eventType = payload.path("event_type").asText("");
            if (!"CANCEL".equals(eventType)) {
                return;
            }

            String innerPayload = payload.path("payload").asText("");
            if (innerPayload.isBlank()) {
                throw new IllegalArgumentException("order outbox payload is empty");
            }

            JsonNode dataNode = objectMapper.readTree(innerPayload);
            String orderId = dataNode.path("orderId").asText("");
            if (orderId.isBlank()) {
                throw new IllegalArgumentException("orderId is empty");
            }
            String reason = dataNode.path("reason").asText("ORDER_TO_PAID_FAILED");
            payService.cancelByOrderId(orderId, reason, false);
        } catch (JsonProcessingException | RuntimeException e) {
            log.error("[Pay Consumer] 주문 취소 이벤트 처리 중 오류 발생: {}", e.getMessage());
        }
    }
}
