package com.hyuk.rider.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
@RequiredArgsConstructor
public class WebSocketHandler extends TextWebSocketHandler {
    private final ObjectMapper objectMapper;

    // userId -> {session, lat, lng}
    private final Map<String, RiderSession> sessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String userId = getParam(session, "userId");
        String lat = getParam(session, "latitude");
        String lng = getParam(session, "longitude");

        if (userId != null && lat != null && lng != null) {
            sessions.put(userId, new RiderSession(session, Double.parseDouble(lat), Double.parseDouble(lng)));
            log.info("Rider WebSocket 연결: userId={}, lat={}, lng={}", userId, lat, lng);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            Map<String, Object> payload = objectMapper.readValue(message.getPayload(), Map.class);
            String type = (String) payload.get("type");

            if ("LOCATION_UPDATE".equals(type)) {
                double lat = ((Number) payload.get("latitude")).doubleValue();
                double lng = ((Number) payload.get("longitude")).doubleValue();

                String userId = sessions.entrySet().stream()
                        .filter(e -> e.getValue().session().getId().equals(session.getId()))
                        .map(Map.Entry::getKey)
                        .findFirst()
                        .orElse(null);

                if (userId != null) {
                    sessions.put(userId, new RiderSession(session, lat, lng));
                    log.info("Rider 위치 갱신: userId={}, lat={}, lng={}", userId, lat, lng);
                }
            }
        } catch (Exception e) {
            log.error("Rider WebSocket 메시지 처리 실패: {}", e.getMessage());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.entrySet().removeIf(e -> e.getValue().session().getId().equals(session.getId()));
        log.info("Rider WebSocket 종료");
    }

    public void broadcastToNearbyRiders(double restaurantLat, double restaurantLng, Object data) {
        sessions.forEach((userId, riderSession) -> {
            double distance = haversine(restaurantLat, restaurantLng,
                    riderSession.latitude(), riderSession.longitude());
            if (distance <= 3.0 && riderSession.session().isOpen()) {
                try {
                    riderSession.session().sendMessage(
                            new TextMessage(objectMapper.writeValueAsString(data))
                    );
                } catch (Exception e) {
                    log.error("Rider WebSocket 전송 실패: userId={}", userId);
                }
            }
        });
    }

    private double haversine(double lat1, double lng1, double lat2, double lng2) {
        final int R = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat/2) * Math.sin(dLat/2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng/2) * Math.sin(dLng/2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
    }

    private String getParam(WebSocketSession session, String key) {
        return UriComponentsBuilder.fromUri(session.getUri())
                .build().getQueryParams().getFirst(key);
    }

    public record RiderSession(WebSocketSession session, double latitude, double longitude) {}
}