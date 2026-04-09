package com.hyuk.pay.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hyuk.pay.client.TossPayClient;
import com.hyuk.pay.dto.TossCancelResponse;
import com.hyuk.pay.service.PayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaConsumer {
    private final ObjectMapper objectMapper;
    private final PayService payService;
    private final TossPayClient tossPayClient;

    @KafkaListener(topics = "order-events.public.order_outbox", groupId = "pay-service-group")
    public void onOrderEvent(String message) {
        try {
            JsonNode payload = objectMapper.readTree(message).get("payload").get("after");

            if (payload == null || payload.isNull()) {
                throw new RuntimeException("order-events.public.order_outbox.payload is null");
            }

            String status = payload.get("status").asText();
            String reason = payload.get("reason").asText();

            if (status.equals("CANCEL")) {
                String innerPayload = payload.get("payload").asText();
                JsonNode dataNode = objectMapper.readTree(innerPayload);

                JsonNode requestNode = dataNode.get("payConfirmRequest");

                String paymentKey = requestNode.get("paymentKey").asText();

                Map<String, String> cancelRequest = Map.of("cancelReason", reason);

                TossCancelResponse response = tossPayClient.cancel(paymentKey, cancelRequest);

                payService.cancelPayment(response, false);
            }
        } catch (JsonProcessingException e) {
            log.error("[Pay Consumer] 주문 취소 이벤트 처리 중 오류 발생: {}", e.getMessage());
        }
    }
}
