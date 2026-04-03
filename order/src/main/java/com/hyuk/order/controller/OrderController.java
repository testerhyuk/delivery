package com.hyuk.order.controller;

import com.hyuk.order.dto.OrderRequestDto;
import com.hyuk.order.dto.OrderResponseDto;
import com.hyuk.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/order-service")
public class OrderController {
    private final OrderService orderService;

    @PostMapping("/order")
    public ResponseEntity<OrderResponseDto> createOrder(@Valid @RequestBody OrderRequestDto orderRequestDto,
                                                        @RequestHeader("X-User-Id") String userId) {
        OrderResponseDto dto = orderService.createOrder(orderRequestDto, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }
}
