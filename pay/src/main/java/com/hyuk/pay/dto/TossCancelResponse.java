package com.hyuk.pay.dto;

import lombok.Data;

import java.util.List;

@Data
public class TossCancelResponse {
    private String paymentKey;
    private String orderId;
    private String status;
    private String approvedAt;
    private String lastTransactionKey;

    private List<Cancel> cancels;

    @Data
    public static class Cancel {
        private Long cancelAmount;
        private String cancelReason;
        private String canceledAt;
        private String transactionKey;
    }
}
