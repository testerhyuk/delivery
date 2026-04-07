package com.hyuk.order.service;

import com.hyuk.order.dto.OrderCompleteResponseDto;
import com.hyuk.order.dto.OrderRequestDto;
import com.hyuk.order.dto.OrderResponseDto;
import com.hyuk.order.dto.PayConfirmedRequestDto;

public interface OrderService {
    OrderResponseDto createOrder(OrderRequestDto orderRequestDto, String userId);
    void moneyPaid(PayConfirmedRequestDto payConfirmedRequestDto);
    void cancelOrder(Long orderId);
    void updateToCooking(Long orderId);
    void updateToDelivering(Long orderId);
    OrderCompleteResponseDto completeOrder(Long orderId);

}
