package com.hyuk.pay.dto;

import lombok.Data;

import java.util.List;

@Data
public class RequestOrder {
    private String paymentKey;
    private String orderId;
    private String userId;
    private String restaurantId;
    private Long amount;
    private List<RequestOrderItems> orderItems;

    @Data
    public static class RequestOrderItems {
        private String menuId;
        private String menuName;
        private Long price;
        private Integer quantity;
    }
}
