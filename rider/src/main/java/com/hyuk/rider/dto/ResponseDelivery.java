package com.hyuk.rider.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class ResponseDelivery {
    private Long id;
    private String riderId;
    private String orderId;
    private String restaurantId;
    private Integer totalPrice;
    private String deliveryAddress;
    private String detailAddress;
    private BigDecimal userLatitude;
    private BigDecimal userLongitude;
    private List<ResponseDelivery.ResponseDeliveryOrderItems> orderItems;

    @Data
    @Builder
    public static class ResponseDeliveryOrderItems {
        private String menuId;
        private String menuName;
        private Integer price;
        private Integer quantity;
    }
}
