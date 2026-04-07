package com.hyuk.order.dto;

import lombok.Data;

import java.util.List;

@Data
public class PayRequestDto {
    private String paymentKey;
    private String orderId;
    private String userId;
    private Long restaurantId;
    private Long amount;
    private List<RequestOrderItems> orderItems;

    @Data
    public static class RequestOrderItems {
        private Long menuId;
        private String menuName;
        private Long price;
        private Integer quantity;
    }
}
