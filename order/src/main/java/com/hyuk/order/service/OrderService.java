package com.hyuk.order.service;

import com.hyuk.order.dto.OrderRequestDto;
import com.hyuk.order.dto.OrderResponseDto;
import org.springframework.web.bind.annotation.RequestHeader;

public interface OrderService {
    OrderResponseDto createOrder(OrderRequestDto orderRequestDto, String userId);
}
