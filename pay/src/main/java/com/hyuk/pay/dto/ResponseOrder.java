package com.hyuk.pay.dto;

import com.hyuk.pay.entity.enums.PayStatus;
import lombok.Data;

@Data
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
    public static class ResponseToss {
        private String number;
    }
}
