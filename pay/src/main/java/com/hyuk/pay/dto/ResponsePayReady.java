package com.hyuk.pay.dto;

import com.hyuk.pay.entity.enums.PayStatus;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ResponsePayReady {
    private Long id;
    private String payId;
    private String orderId;
    private Long amount;
    private PayStatus status;
    private String paymentKey;
    private String checkoutUrl;
}
