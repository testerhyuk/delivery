package com.hyuk.seller.entity;

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
@Table(name = "seller_outbox")
public class SellerOutbox {
    @Id
    private Long id;

    @Column(nullable = false, unique = true)
    private String sellerOutboxId;

    @Column(nullable = false)
    private String eventType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public static SellerOutbox create(Long id, String eventType, String payload) {
        SellerOutbox sellerOutbox = new SellerOutbox();
        sellerOutbox.id = id;
        sellerOutbox.sellerOutboxId = Snowflake.prefixedId("sellerOutbox", id);
        sellerOutbox.eventType = eventType;
        sellerOutbox.payload = payload;
        sellerOutbox.createdAt = LocalDateTime.now();

        return sellerOutbox;
    }
}
