package com.hyuk.rider.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hyuk.rider.client.RestaurantServiceClient;
import com.hyuk.rider.config.WebSocketHandler;
import com.hyuk.rider.dto.RequestOrder;
import com.hyuk.rider.dto.RestaurantResponse;
import com.hyuk.rider.service.RiderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaConsumer {
    private final ObjectMapper objectMapper;
    private final RiderService riderService;
    private final StringRedisTemplate stringRedisTemplate;
    private final RestaurantServiceClient restaurantServiceClient;
    private final WebSocketHandler riderWebSocketHandler;

    @KafkaListener(topics = "seller-events.public.seller_outbox", groupId = "rider-service-group")
    public void onSellerEvent(String message) {
        try {
            JsonNode payload = objectMapper.readTree(message).get("payload").get("after");

            if (payload == null || payload.isNull()) {
                throw new RuntimeException("seller-events.public.seller_outbox is null");
            }

            String jsonPayload = payload.get("payload").asText();
            JsonNode data = objectMapper.readTree(jsonPayload);
            String status = data.get("status").asText();

            if (status.equals("COOKING")) {
                JsonNode orderDetail = data.get("responseData");
                RequestOrder requestOrder = objectMapper.treeToValue(orderDetail, RequestOrder.class);

                String orderJson = objectMapper.writeValueAsString(requestOrder);

                stringRedisTemplate.opsForValue().set(
                        "order:cooking:" + requestOrder.getId(),
                        orderJson,
                        10, TimeUnit.MINUTES
                );

                RestaurantResponse restaurant = restaurantServiceClient.getRestaurant(requestOrder.getRestaurantId());
                double restaurantLat = restaurant.getLatitude().doubleValue();
                double restaurantLng = restaurant.getLongitude().doubleValue();

                Map<String, Object> notification = Map.of(
                        "type", "NEW_DELIVERY",
                        "orderId", String.valueOf(requestOrder.getId()),
                        "restaurantId", requestOrder.getRestaurantId(),
                        "deliveryAddress", requestOrder.getDeliveryAddress(),
                        "totalPrice", requestOrder.getTotalPrice(),
                        "orderItems", requestOrder.getOrderItems()
                );

                riderWebSocketHandler.broadcastToNearbyRiders(restaurantLat, restaurantLng, notification);
            }
        } catch (JsonProcessingException e) {
            log.error("[Seller Consumer] 음식점 이벤트 처리 오류 발생 : {}", e.getMessage());
        }
    }
}
