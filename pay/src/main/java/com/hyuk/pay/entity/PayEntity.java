package com.hyuk.pay.entity;

import com.hyuk.pay.entity.enums.PayStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Table(name = "pay")
public class PayEntity {
    @Id
    private Long id;

    @Column(nullable = false, unique = true)
    private String orderId;
    @Column(nullable = false)
    private Long amount;
    @Column(unique = true)
    private String paymentKey;
    @Enumerated(EnumType.STRING)
    private PayStatus status;
    private String method;
    private String cardNumber;
    private String failReason;
    private Long vat;
    private LocalDateTime requestedAt;
    private LocalDateTime approvedAt;

    public static PayEntity processPay(Long id, String orderId, Long amount) {
        PayEntity entity = new PayEntity();
        entity.id = id;
        entity.orderId = orderId;
        entity.amount = amount;
        entity.status = PayStatus.READY;
        entity.requestedAt = LocalDateTime.now();

        return entity;
    }

    public void updateStatus(PayStatus status) {
        this.status = status;
    }

    public void completedPayment(String paymentKey, String method, String cardNumber,
                                 Long vat, LocalDateTime approvedAt) {
        this.paymentKey = paymentKey;
        this.method = method;
        this.cardNumber = cardNumber;
        this.vat = vat;
        this.status = PayStatus.DONE;
        this.approvedAt = approvedAt;
    }

    public void failPayment(String failReason) {
        this.status = PayStatus.FAILED;
        this.failReason = failReason;
    }

    public void updateAmount(Long amount) {
        this.amount = amount;
    }
}
