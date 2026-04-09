package com.hyuk.pay.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hyuk.common.Snowflake;
import com.hyuk.pay.dto.PayConfirmedRequestDto;
import com.hyuk.pay.entity.PayOutbox;
import com.hyuk.pay.repository.PayOutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class PayOutboxService {
    private final ObjectMapper objectMapper;
    private final PayOutboxRepository payOutboxRepository;
    private final Snowflake snowflake;

    @Transactional(propagation = Propagation.REQUIRED)
    public void saveToOrderUpdatePaid(PayConfirmedRequestDto requestDto) {
        try {
            Map<String, Object> data = Map.of(
                    "orderId", requestDto.getOrderId(),
                    "status", requestDto.getPayStatus()
            );

            String payload = objectMapper.writeValueAsString(data);

            PayOutbox entity = PayOutbox.create(
                    snowflake.nextId(),
                    requestDto.getPayStatus(),
                    payload
            );

            payOutboxRepository.save(entity);
        } catch (JsonProcessingException e) {
            log.error("toOrder update to paid failed : {}", String.valueOf(e));
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveToOrderCancel(String orderId, String status) {
        try {
            Map<String, String> data = Map.of(
                    "orderId", orderId,
                    "status", status
            );

            String payload = objectMapper.writeValueAsString(data);

            PayOutbox entity = PayOutbox.create(
                    snowflake.nextId(),
                    status,
                    payload
            );

            payOutboxRepository.save(entity);
        } catch (JsonProcessingException e) {
            log.error("toOrder cancel failed : {}", String.valueOf(e));
        }
    }
}
