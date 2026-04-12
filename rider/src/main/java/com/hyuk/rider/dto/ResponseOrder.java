package com.hyuk.rider.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ResponseOrder {
    private Long id;
    private String orderId;
    private String restaurantId;
    private Integer totalPrice;
    private String deliveryAddress;
    private List<ResponseOrderItems> orderItems;

    @Data
    @Builder
    public static class ResponseOrderItems {
        private Long menuId;
        private String menuName;
        private Integer price;
        private Integer quantity;
    }
}
