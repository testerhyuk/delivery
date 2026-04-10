package com.hyuk.order.dto;

import lombok.Data;

@Data
public class ResponsePayReady {
    private Long id;
    private String payId;
    private String orderId;
    private Long amount;
    private String status;
    private String paymentKey;
    private String checkoutUrl;
}
