package com.hyuk.order.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hyuk.common.Snowflake;
import com.hyuk.order.dto.SellerResponseDto;
import com.hyuk.order.entity.OrderOutbox;
import com.hyuk.order.repository.OrderOutboxRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class OrderOutboxService {
    private final ObjectMapper objectMapper;
    private final OrderOutboxRepository orderOutboxRepository;
    private final Snowflake snowflake;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveCancelEvent(String orderId, String status) {
        try {
            Map<String, String> data = Map.of(
                    "orderId", orderId,
                    "status", status,
                    "reason", "ORDER_UPDATE_TO_PAID_FAILED"
            );

            String payload = objectMapper.writeValueAsString(data);

            OrderOutbox orderOutbox = OrderOutbox.create(
                    snowflake.nextId(),
                    status,
                    payload
            );

            orderOutboxRepository.save(orderOutbox);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("toPay Cancel 이벤트 직렬화 실패 : " + e);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveOrderCompleteEvent(String orderId, String status) {
        try {
            Map<String, String> data = Map.of(
                    "orderId", orderId,
                    "status", status
            );

            String payload = objectMapper.writeValueAsString(data);

            OrderOutbox orderOutbox = OrderOutbox.create(
                    snowflake.nextId(),
                    status,
                    payload
            );

            orderOutboxRepository.save(orderOutbox);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("toSeller Completed 이벤트 직렬화 실패 : " + e);
        }
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void saveSendToSellerEvent(String orderId, String status, SellerResponseDto response) {
        try {
            Map<String, Object> data = Map.of(
                    "orderId", orderId,
                    "status", status,
                    "sellerResponse", response
            );

            String payload = objectMapper.writeValueAsString(data);

            OrderOutbox orderOutbox = OrderOutbox.create(
                    snowflake.nextId(),
                    status,
                    payload
            );

            orderOutboxRepository.save(orderOutbox);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("toSeller Send Data 이벤트 직렬화 실패 : " + e);
        }
    }
}
