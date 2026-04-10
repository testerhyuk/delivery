package com.hyuk.seller.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hyuk.common.Snowflake;
import com.hyuk.seller.dto.ResponseOrder;
import com.hyuk.seller.entity.SellerOutbox;
import com.hyuk.seller.repository.SellerOutboxRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class SellerOutboxService {
    private final ObjectMapper objectMapper;
    private final SellerOutboxRepository sellerOutboxRepository;
    private final Snowflake snowflake;

    @Transactional
    public void saveCookingOrDeliveringEvent(ResponseOrder response, String status) {
        try {
            Map<String, Object> data = Map.of(
                    "orderId", response.getId(),
                    "status", status,
                    "responseData", response
            );

            String payload = objectMapper.writeValueAsString(data);

            SellerOutbox sellerOutbox = SellerOutbox.create(
                    snowflake.nextId(),
                    status,
                    payload
            );

            sellerOutboxRepository.save(sellerOutbox);
        } catch (Exception e) {
            throw new RuntimeException("toOrder Cooking/Delivering 이벤트 직렬화 실패 : " + e);
        }
    }
}
