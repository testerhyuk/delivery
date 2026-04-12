package com.hyuk.rider.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hyuk.common.Snowflake;
import com.hyuk.rider.dto.ResponseDelivery;
import com.hyuk.rider.dto.ResponseOrder;
import com.hyuk.rider.entity.RiderOutbox;
import com.hyuk.rider.repository.RiderOutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class RiderOutboxService {
    private final ObjectMapper objectMapper;
    private final Snowflake snowflake;
    private final RiderOutboxRepository riderOutboxRepository;

    @Transactional
    public void completeDeliveryEvent(ResponseOrder response) {
        try {
            Map<String, Object> eventData = Map.of(
                    "orderId", response.getOrderId(),
                    "status", "COMPLETED",
                    "responseData", response
            );

            String payload = objectMapper.writeValueAsString(eventData);

            RiderOutbox outbox = RiderOutbox.create(
                    snowflake.nextId(),
                    "COMPLETED",
                    payload
            );

            riderOutboxRepository.save(outbox);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("배달 완료 이벤트 직렬화 실패", e);
        }
    }

    @Transactional
    public void deliveryStartEvent(ResponseDelivery response) {
        try {
            Map<String, Object> eventData = Map.of(
                    "orderId", response.getOrderId(),
                    "status", "DELIVERING",
                    "responseData", response
            );

            String payload = objectMapper.writeValueAsString(eventData);

            RiderOutbox outbox = RiderOutbox.create(
                    snowflake.nextId(),
                    "DELIVERING",
                    payload
            );

            riderOutboxRepository.save(outbox);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("배달 시작 이벤트 직렬화 실패", e);
        }
    }
}
