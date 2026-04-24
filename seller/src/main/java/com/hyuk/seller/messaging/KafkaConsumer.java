package com.hyuk.seller.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hyuk.seller.config.SellerWebSocketHandler;
import com.hyuk.seller.dto.RequestOrder;
import com.hyuk.seller.service.SellerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaConsumer {
    private final ObjectMapper objectMapper;
    private final SellerService sellerService;
    private final SellerWebSocketHandler sellerWebSocketHandler;

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
                    .id(sellerRes.path("id").asLong())
                    .orderId(sellerRes.path("orderId").asText(""))
                    .restaurantId(sellerRes.path("restaurantId").asText(""))
                    .userId(sellerRes.path("userId").asText(""))
                    .totalPrice(sellerRes.path("totalPrice").asInt())
                    .deliveryAddress(sellerRes.path("deliveryAddress").asText(""))
                    .orderAt(LocalDateTime.parse(sellerRes.path("orderAt").asText()))
                    .orderItems(objectMapper.convertValue(
                            sellerRes.path("orderItems"),
                            new TypeReference<List<RequestOrder.RequestOrderItems>>() {}
                    ))
                    .build();

            sellerService.confirmOrder(order);

            Map<String, Object> notification = Map.of(
                    "type", "NEW_ORDER",
                    "orderId", order.getOrderId(),
                    "restaurantId", order.getRestaurantId(),
                    "totalPrice", order.getTotalPrice(),
                    "deliveryAddress", order.getDeliveryAddress(),
                    "orderItems", order.getOrderItems()
            );
            log.info("WebSocket 알림 전송 시도: restaurantId={}", order.getRestaurantId());
            sellerWebSocketHandler.sendToSeller(
                    order.getRestaurantId(),
                    notification
            );
            log.info("WebSocket 알림 전송 완료");
        } catch (JsonProcessingException | RuntimeException e) {
            log.error("Order에서 이벤트를 수신하지 못했습니다 : {}", e.getMessage());
        }
    }
}
