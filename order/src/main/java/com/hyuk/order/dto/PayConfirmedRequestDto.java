package com.hyuk.order.dto;

import lombok.Data;

@Data
public class PayConfirmedRequestDto {
    private String orderId;
    private String payStatus;
}
