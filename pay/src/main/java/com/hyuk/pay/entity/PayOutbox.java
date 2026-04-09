package com.hyuk.pay.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "pay_outbox")
public class PayOutbox {
    @Id
    private Long id;

    @Column(nullable = false)
    private String eventType;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;
    @Column(nullable = false)
    private LocalDateTime createdAt;

    public static PayOutbox create(Long id, String eventType, String payload) {
        PayOutbox payOutbox = new PayOutbox();
        payOutbox.id = id;
        payOutbox.eventType = eventType;
        payOutbox.payload = payload;
        payOutbox.createdAt = LocalDateTime.now();

        return payOutbox;
    }
}
