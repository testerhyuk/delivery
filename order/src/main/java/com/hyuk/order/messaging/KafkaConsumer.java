package com.hyuk.order.messaging;

import com.hyuk.order.entity.OrderEntity;
import com.hyuk.order.repository.OrderRepository;
import com.hyuk.order.util.OrderWebSocketHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hyuk.order.dto.PayConfirmedRequestDto;
import com.hyuk.order.service.OrderService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaConsumer {
    private final OrderService orderService;
    private final ObjectMapper objectMapper;
    private final OrderWebSocketHandler orderWebSocketHandler;
    private final OrderRepository orderRepository;

    @KafkaListener(topics = "pay-events.public.pay_outbox", groupId = "order-service-pay-group")
    public void onPayEvent(String message) {
        try {
            JsonNode payload = objectMapper.readTree(message).path("payload").path("after");

            if (payload.isMissingNode() || payload.isNull()) {
                throw new IllegalArgumentException("pay-events.public.pay_outbox.payload is null");
            }

            String status = payload.path("event_type").asText("");
            JsonNode dataNode = objectMapper.readTree(payload.path("payload").asText("{}"));
            long orderId = dataNode.path("orderId").asLong(-1L);
            if (orderId < 0) {
                throw new IllegalArgumentException("pay event orderId is invalid");
            }

            if (status.equals("DONE")) {
                PayConfirmedRequestDto payConfirmedRequestDto = new PayConfirmedRequestDto();
                payConfirmedRequestDto.setOrderId(String.valueOf(orderId));
                payConfirmedRequestDto.setPayStatus(status);

                orderService.moneyPaid(payConfirmedRequestDto);

                log.info("[Order Consumer] 결제 완료 처리 성공 orderId={}", orderId);
            } else if (status.equals("CANCEL")) {
                orderService.cancelOrder(orderId);
            }
        } catch (JsonProcessingException | RuntimeException e) {
            log.error("[Order Consumer] 결제 이벤트 처리 중 오류 발생: {}", e.getMessage());
        }
    }

    @KafkaListener(topics = "seller-events.public.seller_outbox", groupId = "order-service-seller-group")
    public void onSellerEvent(String message) {
        try {
            JsonNode payload = objectMapper.readTree(message).path("payload").path("after");

            if (payload.isMissingNode() || payload.isNull()){
                throw new IllegalArgumentException("seller-events.public.seller_outbox.payload is null");
            }

            String status = payload.path("event_type").asText("");
            JsonNode dataNode = objectMapper.readTree(payload.path("payload").asText("{}"));
            long orderId = resolveOrderPkFromSellerPayload(dataNode);
            if (orderId < 0) {
                throw new IllegalArgumentException("seller event orderId is invalid");
            }

            if (status.equals("COOKING")) {
                orderService.updateToCooking(orderId);
            }
        } catch (JsonProcessingException | RuntimeException e) {
            log.error("[Seller Consumer] 음식점 이벤트 처리 오류 발생 : {}", e.getMessage());
        }
    }

    @KafkaListener(topics = "rider-events.public.rider_outbox", groupId = "order-service-rider-group")
    public void onRiderEvent(String message) {
        try {
            JsonNode payload = objectMapper.readTree(message).path("payload").path("after");

            if (payload.isMissingNode() || payload.isNull()) {
                throw new RuntimeException("rider-events.public.rider_outbox.payload is null");
            }

            String status = payload.path("event_type").asText("");
            JsonNode dataNode = objectMapper.readTree(payload.path("payload").asText("{}"));
            long orderId = dataNode.path("orderId").asLong(-1L);
            if (orderId < 0) {
                throw new IllegalArgumentException("rider event orderId is invalid");
            }

            if (status.equals("COMPLETED")) {
                orderService.completeOrder(orderId);

                String userId = orderService.findUserIdByOrderId(orderId);
                orderWebSocketHandler.sendToUser(userId, Map.of(
                        "type", "COMPLETED",
                        "orderId", String.valueOf(orderId),
                        "message", "배달이 완료되었습니다! 맛있게 드세요 😊"
                ));
            } else if (status.equals("DELIVERING")) {
                orderService.updateToDelivering(orderId);
            } else if (status.equals("DELIVERY_START")) {
                orderService.updateToDeliveryStart(orderId);

                String userId = orderService.findUserIdByOrderId(orderId);

                OrderEntity order = orderRepository.findById(orderId).orElseThrow(
                        () -> new RuntimeException("주문 정보를 찾을 수 없습니다.")
                );

                int etaMinutes = calculateEta(
                        order.getRestaurantLatitude().doubleValue(),
                        order.getRestaurantLongitude().doubleValue(),
                        order.getUserLatitude().doubleValue(),
                        order.getUserLongitude().doubleValue()
                );

                orderWebSocketHandler.sendToUser(userId, Map.of(
                        "type", "DELIVERY_STARTED",
                        "orderId", String.valueOf(orderId),
                        "message", "배달이 시작되었습니다! 약 " + etaMinutes + "분 후 도착 예정입니다.",
                        "etaMinutes", etaMinutes
                ));
            }
        } catch (JsonProcessingException | RuntimeException e) {
            log.error("[Rider Consumer] 배달 완료 이벤트 처리 오류 발생 : {}", e.getMessage());
        }
    }

    private int calculateEta(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371000;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat/2) * Math.sin(dLat/2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon/2) * Math.sin(dLon/2);
        double distance = R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        return Math.max(1, (int)(distance / 333.0 / 60));
    }

    private static long resolveOrderPkFromSellerPayload(JsonNode dataNode) {
        long fromBody = dataNode.path("responseData").path("id").asLong(-1L);
        if (fromBody >= 0) {
            return fromBody;
        }
        String raw = dataNode.path("orderId").asText("");
        if (raw.isBlank()) {
            return -1L;
        }
        int u = raw.lastIndexOf('_');
        String tail = u >= 0 ? raw.substring(u + 1) : raw;
        try {
            return Long.parseLong(tail);
        } catch (NumberFormatException e) {
            return -1L;
        }
    }
}
