package com.hyuk.order.dto;

import lombok.Data;

@Data
public class ResponsePayReady {
    private Long id;
    private Long orderId;
    private Integer amount;
    private String status;
    private String paymentKey;
    private String checkoutUrl;
}
