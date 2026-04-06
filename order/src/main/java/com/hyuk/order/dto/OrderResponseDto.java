package com.hyuk.order.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class OrderResponseDto {
    private Long id;
    private String userId;
    private Long restaurantId;
    private String orderStatus;
    private Integer totalPrice;
    private String deliveryAddress;
    private BigDecimal userLatitude;
    private BigDecimal userLongitude;
    private LocalDateTime orderAt;
    private List<OrderItemsResponseDto> orderItems;

    @Data
    public static class OrderItemsResponseDto {
        private String menuName;
        private Integer price;
        private Integer quantity;
    }
}
