package com.hyuk.rider.entity;

import com.hyuk.common.Snowflake;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Table(name = "rider_outbox")
public class RiderOutbox {
    @Id
    private Long id;

    @Column(nullable = false, unique = true)
    private String riderOutboxId;

    @Column(nullable = false)
    private String eventType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public static RiderOutbox create(Long id, String eventType, String payload) {
        RiderOutbox riderOutbox = new RiderOutbox();
        riderOutbox.id = id;
        riderOutbox.riderOutboxId = Snowflake.prefixedId("riderOutbox", id);
        riderOutbox.eventType = eventType;
        riderOutbox.payload = payload;
        riderOutbox.createdAt = LocalDateTime.now();

        return riderOutbox;
    }
}
