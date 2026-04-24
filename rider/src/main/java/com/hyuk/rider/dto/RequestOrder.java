package com.hyuk.rider.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class RequestOrder {
    private Long id;
    private String restaurantId;
    private String userId;
    private Integer totalPrice;
    private String deliveryAddress;
    private String detailAddress;
    private BigDecimal userLatitude;
    private BigDecimal userLongitude;
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
