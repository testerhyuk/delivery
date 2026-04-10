package com.hyuk.order.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hyuk.order.dto.PayConfirmedRequestDto;
import com.hyuk.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaConsumer {
    private final OrderService orderService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "pay-events.public.pay_outbox", groupId = "order-service-group")
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
            } else if (status.equals("CANCEL")) {
                orderService.cancelOrder(orderId);
            }
        } catch (JsonProcessingException | RuntimeException e) {
            log.error("[Order Consumer] 결제 이벤트 처리 중 오류 발생: {}", e.getMessage());
        }
    }

    @KafkaListener(topics = "seller-events.public.seller_outbox", groupId = "order-service-group")
    public void onSellerEvent(String message) {
        try {
            JsonNode payload = objectMapper.readTree(message).path("payload").path("after");

            if (payload.isMissingNode() || payload.isNull()){
                throw new IllegalArgumentException("seller-events.public.seller_outbox.payload is null");
            }

            String status = payload.path("event_type").asText("");
            JsonNode dataNode = objectMapper.readTree(payload.path("payload").asText("{}"));
            long orderId = dataNode.path("orderId").asLong(-1L);
            if (orderId < 0) {
                throw new IllegalArgumentException("seller event orderId is invalid");
            }

            if (status.equals("COOKING")) {
                orderService.updateToCooking(orderId);
            } else if (status.equals("DELIVERING")) {
                orderService.updateToDelivering(orderId);
            }
        } catch (JsonProcessingException | RuntimeException e) {
            log.error("[Seller Consumer] 음식점 이벤트 처리 오류 발생 : {}", e.getMessage());
        }
    }

    @KafkaListener(topics = "rider-events.public.rider_outbox", groupId = "order-service-group")
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
            }
        } catch (JsonProcessingException | RuntimeException e) {
            log.error("[Rider Consumer] 배달 완료 이벤트 처리 오류 발생 : {}", e.getMessage());
        }
    }
}
