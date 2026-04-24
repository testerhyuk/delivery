package com.hyuk.order.util;

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
public class OrderWebSocketHandler extends TextWebSocketHandler {
    private final ObjectMapper objectMapper;
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String userId = getParam(session, "userId");
        if (userId != null) {
            sessions.put(userId, session);
            log.info("Order WebSocket 연결: userId={}", userId);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.entrySet().removeIf(e -> e.getValue().getId().equals(session.getId()));
        log.info("Order WebSocket 종료");
    }

    public void sendToUser(String userId, Object data) {
        WebSocketSession session = sessions.get(userId);
        if (session != null && session.isOpen()) {
            try {
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(data)));
            } catch (Exception e) {
                log.error("Order WebSocket 전송 실패: userId={}", userId);
            }
        } else {
            log.warn("주문자 오프라인: userId={}", userId);
        }
    }

    private String getParam(WebSocketSession session, String key) {
        return UriComponentsBuilder.fromUri(session.getUri())
                .build().getQueryParams().getFirst(key);
    }
}
