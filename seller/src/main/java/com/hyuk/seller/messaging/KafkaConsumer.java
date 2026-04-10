package com.hyuk.seller.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hyuk.seller.dto.RequestOrder;
import com.hyuk.seller.service.SellerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaConsumer {
    private final ObjectMapper objectMapper;
    private final SellerService sellerService;

    @KafkaListener(topics = "order-events.public.order_outbox", groupId = "seller-service-group")
    public void onOrderEvent(String message) {
        try {
            JsonNode root = objectMapper.readTree(message);
            JsonNode after = root.path("payload").path("after");

            if (after.isMissingNode() || after.isNull()) {
                log.warn("[Seller Consumer] Debezium after payload가 비어 있습니다.");
                return;
            }

            String eventType = after.path("event_type").asText("");
            if (!"ORDER".equals(eventType)) {
                return;
            }

            String payloadString = after.path("payload").asText();
            if (payloadString.isBlank()) {
                throw new IllegalArgumentException("order outbox payload is empty");
            }

            JsonNode data = objectMapper.readTree(payloadString);
            JsonNode sellerRes = data.get("sellerResponse");
            if (sellerRes == null || sellerRes.isNull()) {
                throw new IllegalArgumentException("sellerResponse is missing");
            }

            RequestOrder order = RequestOrder.builder()
                    .id(sellerRes.get("id").asLong())
                    .restaurantId(sellerRes.get("restaurantId").asLong())
                    .userId(sellerRes.get("userId").asText())
                    .totalPrice(sellerRes.get("totalPrice").asInt())
                    .deliveryAddress(sellerRes.get("deliveryAddress").asText())
                    .orderAt(LocalDateTime.parse(sellerRes.get("orderAt").asText()))
                    .orderItems(objectMapper.convertValue(
                            sellerRes.get("orderItems"),
                            new TypeReference<List<RequestOrder.RequestOrderItems>>() {}
                    ))
                    .build();

            sellerService.confirmOrder(order);
        } catch (JsonProcessingException | RuntimeException e) {
            log.error("Order에서 이벤트를 수신하지 못했습니다 : {}", e.getMessage());
        }
    }
}
