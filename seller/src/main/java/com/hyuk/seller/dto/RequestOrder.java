package com.hyuk.seller.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class RequestOrder {
    private Long id;
    private String orderId;
    private String restaurantId;
    private String userId;
    private Integer totalPrice;
    private String deliveryAddress;
    private LocalDateTime orderAt;
    private List<RequestOrderItems> orderItems;

    @Data
    @Builder
    public static class RequestOrderItems {
        private String menuId;
        private String menuName;
        private Integer price;
        private Integer quantity;
    }
}
