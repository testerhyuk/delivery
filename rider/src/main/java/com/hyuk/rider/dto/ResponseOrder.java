package com.hyuk.rider.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class ResponseOrder {
    private Long id;
    private String orderId;
    private String restaurantId;
    private Integer totalPrice;
    private String deliveryAddress;
    private String detailAddress;
    private BigDecimal userLatitude;
    private BigDecimal userLongitude;
    private List<ResponseOrderItems> orderItems;
    private BigDecimal latitude;
    private BigDecimal longitude;

    @Data
    @Builder
    public static class ResponseOrderItems {
        private Long menuId;
        private String menuName;
        private Integer price;
        private Integer quantity;
    }
}
