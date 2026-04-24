package com.hyuk.order.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class OrderResponseDto {
    private Long id;
    private String orderId;
    private String userId;
    private String restaurantId;
    private String orderStatus;
    private Integer totalPrice;
    private String deliveryAddress;
    private String detailAddress;
    private BigDecimal userLatitude;
    private BigDecimal userLongitude;
    private LocalDateTime orderAt;
    private List<OrderItemsResponseDto> orderItems;

    private ResponsePayReady paymentInfo;

    @Data
    public static class OrderItemsResponseDto {
        private String menuName;
        private Integer price;
        private Integer quantity;
    }
}
