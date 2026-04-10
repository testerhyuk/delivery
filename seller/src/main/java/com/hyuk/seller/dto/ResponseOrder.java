package com.hyuk.seller.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ResponseOrder {
    private Long id;
    private Long restaurantId;
    private Integer totalPrice;
    private String deliveryAddress;
    private LocalDateTime orderAt;
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
