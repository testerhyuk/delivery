package com.hyuk.pay.dto;

import com.hyuk.pay.entity.enums.PayStatus;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ResponseOrder {
    private String paymentKey;
    private String orderId;
    private String userId;
    private String restaurantId;
    private Long totalAmount;
    private PayStatus payStatus;
    private String method;
    private String approvedAt;
    private ResponseToss card;
    private String failure;
    private Long vat;

    @Data
    @Builder
    public static class ResponseToss {
        private String number;
    }
}
