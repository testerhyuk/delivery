package com.hyuk.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class OrderRequestDto {
    @NotNull
    private String restaurantId;
    @NotBlank(message = "주소 정보는 필수입니다")
    private String deliveryAddress;
    @NotNull
    private BigDecimal userLatitude;
    @NotNull
    private BigDecimal userLongitude;
    @NotEmpty
    @Valid
    private List<OrderItemsRequestDto> orderItems;

    @Data
    @Builder
    public static class OrderItemsRequestDto {
        @NotNull
        private String menuId;
        @NotBlank
        private String menuName;
        @NotNull
        private Integer price;
        @NotNull
        @Min(1)
        private Integer quantity;
    }
}
