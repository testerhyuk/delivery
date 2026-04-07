package com.hyuk.order.dto;

import com.hyuk.order.entity.enums.OrderStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class SellerResponseDto {
    private Long id;
    private String userId;
    private Long restaurantId;
    private OrderStatus orderStatus;
    private Integer totalPrice;
    private String deliveryAddress;
    private LocalDateTime orderAt;
    private List<ResponseOrderItems> orderItems;

    @Data
    public static class ResponseOrderItems {
        private Long menuId;
        private String menuName;
        private Integer price;
        private Integer quantity;
    }
}
