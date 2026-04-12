package com.hyuk.seller.config;

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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
@Slf4j
public class SellerWebSocketHandler extends TextWebSocketHandler {
    private final Map<String, Set<WebSocketSession>> sessions = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String restaurantId = getRestaurantId(session);

        if (restaurantId != null) {
            sessions.computeIfAbsent(restaurantId, k -> ConcurrentHashMap.newKeySet()).add(session);
            log.info("Seller WebSocket 연결: restaurantId={}", restaurantId);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String restaurantId = getRestaurantId(session);

        if (restaurantId != null) {
            Set<WebSocketSession> set = sessions.get(restaurantId);
            if (set != null) {
                set.remove(session);
                if (set.isEmpty()) sessions.remove(restaurantId);
            }
            log.info("Seller WebSocket 종료: restaurantId={}", restaurantId);
        }
    }

    public void sendToSeller(String restaurantId, Object data) {
        Set<WebSocketSession> set = sessions.get(restaurantId);

        if (set != null && !set.isEmpty()) {
            set.forEach(session -> {
                if (session.isOpen()) {
                    try {
                        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(data)));
                    } catch (Exception e) {
                        log.error("WebSocket 전송 실패: restaurantId={}, error={}", restaurantId, e.getMessage());
                        set.remove(session);
                    }
                }
            });
        } else {
            log.warn("Seller 오프라인 상태: restaurantId={}", restaurantId);
        }
    }

    private String getRestaurantId(WebSocketSession session) {
        return UriComponentsBuilder.fromUri(session.getUri())
                .build().getQueryParams().getFirst("restaurantId");
    }
}
