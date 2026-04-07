package com.hyuk.order.dto;

import com.hyuk.order.entity.enums.OrderStatus;
import lombok.Data;

@Data
public class OrderCompleteResponseDto {
    private Long id;
    private OrderStatus orderStatus;
}
