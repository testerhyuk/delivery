package com.hyuk.order.dto;

import com.hyuk.order.entity.enums.OrderStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class SellerResponseDto {
    private Long id;
    private String orderId;
    private String userId;
    private String restaurantId;
    private OrderStatus orderStatus;
    private Integer totalPrice;
    private String deliveryAddress;
    private BigDecimal userLatitude;
    private BigDecimal userLongitude;
    private BigDecimal restaurantLatitude;
    private BigDecimal restaurantLongitude;
    private String detailAddress;
    private LocalDateTime orderAt;
    private List<ResponseOrderItems> orderItems;

    @Data
    public static class ResponseOrderItems {
        private String menuId;
        private String menuName;
        private Integer price;
        private Integer quantity;
    }
}
