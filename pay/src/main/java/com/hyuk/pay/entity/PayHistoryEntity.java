package com.hyuk.pay.entity;

import com.hyuk.pay.entity.enums.PayStatus;
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
@Table(name = "pay_history")
public class PayHistoryEntity {
    @Id
    private Long id;
    @Column(nullable = false)
    private Long payId;
    private PayStatus payStatus;
    private String failReason;
    private LocalDateTime failedTime;

    public static PayHistoryEntity failPayment(Long id, Long payId, String failReason, LocalDateTime failedTime) {
        PayHistoryEntity payHistoryEntity = new PayHistoryEntity();
        payHistoryEntity.id = id;
        payHistoryEntity.payId = payId;
        payHistoryEntity.payStatus = PayStatus.FAILED;
        payHistoryEntity.failReason = failReason;
        payHistoryEntity.failedTime = failedTime;

        return payHistoryEntity;
    }
}
