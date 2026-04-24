package com.hyuk.location.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hyuk.location.repository.AddressRoadRepository;
import com.hyuk.location.service.LocationService;
import com.hyuk.location.util.ParsedAddress;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaConsumer {

    private final ObjectMapper objectMapper;
    private final LocationService locationService;
    private final AddressRoadRepository addressRoadRepository;

    @KafkaListener(topics = "rider-events.public.rider_outbox", groupId = "location-service-rider-group")
    public void onRiderEvent(String message) {
        try {
            JsonNode payload = objectMapper.readTree(message)
                    .path("payload").path("after");

            if (payload.isMissingNode() || payload.isNull()) return;

            String eventType = payload.path("event_type").asText("");
            if (!eventType.equals("COMPLETED")) return;

            JsonNode dataNode = objectMapper.readTree(payload.path("payload").asText("{}"));
            JsonNode responseData = dataNode.path("responseData");

            String deliveryAddress = responseData.path("deliveryAddress").asText("");
            String detailAddress = responseData.path("detailAddress").asText("");
            double lat = responseData.path("userLatitude").asDouble(0);
            double lon = responseData.path("userLongitude").asDouble(0);

            if (deliveryAddress.isBlank() || lat == 0 || lon == 0) {
                log.warn("위치 학습 스킵 - 주소 또는 좌표 없음");
                return;
            }

            ParsedAddress parsed = ParsedAddress.from(deliveryAddress);
            addressRoadRepository
                    .findByRoadNameAndBuildMain(parsed.getRoadName(), parsed.getBuildMain())
                    .ifPresentOrElse(
                            road -> {
                                locationService.learnDeliveryLocation(
                                        road.getMgmtNum(), detailAddress, lat, lon
                                );
                                log.info("배달 완료 위치 학습: {} {} ({}, {})",
                                        deliveryAddress, detailAddress, lat, lon);
                            },
                            () -> log.warn("mgmt_num 미존재, 학습 스킵: {}", deliveryAddress)
                    );

        } catch (Exception e) {
            log.error("[Location Consumer] 처리 오류: {}", e.getMessage());
        }
    }
}
