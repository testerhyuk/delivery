package com.hyuk.order.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "order_outbox")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderOutbox {
    @Id
    private Long id;

    @Column(nullable = false)
    private String eventType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public static OrderOutbox create(Long id, String eventType, String payload) {
        OrderOutbox orderOutbox = new OrderOutbox();
        orderOutbox.id = id;
        orderOutbox.eventType = eventType;
        orderOutbox.payload = payload;
        orderOutbox.createdAt = LocalDateTime.now();

        return orderOutbox;
    }
}
