package com.hyuk.order.controller;

import com.hyuk.order.dto.OrderCompleteResponseDto;
import com.hyuk.order.dto.OrderRequestDto;
import com.hyuk.order.dto.OrderResponseDto;
import com.hyuk.order.dto.PayConfirmedRequestDto;
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
                                                        @RequestHeader("userId") String userId) {
        OrderResponseDto dto = orderService.createOrder(orderRequestDto, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @PostMapping("/paid")
    public ResponseEntity<Void> updatePaid(@RequestBody PayConfirmedRequestDto payConfirmedRequestDto) {
        orderService.moneyPaid(payConfirmedRequestDto);

        return ResponseEntity.ok().build();
    }

    @PostMapping("/cancel/{orderId}")
    public ResponseEntity<Void> cancelOrder(@PathVariable("orderId") Long orderId) {
        orderService.cancelOrder(orderId);

        return ResponseEntity.ok().build();
    }

    @PostMapping("/cooking/{orderId}")
    public ResponseEntity<Void> updateCooking(@PathVariable("orderId") Long orderId) {
        orderService.updateToCooking(orderId);

        return ResponseEntity.ok().build();
    }

    @PostMapping("/delivering/{orderId}")
    public ResponseEntity<Void> updateDelivering(@PathVariable("orderId") Long orderId) {
        orderService.updateToDelivering(orderId);

        return ResponseEntity.ok().build();
    }

    @PostMapping("/completed/{orderId}")
    public ResponseEntity<OrderCompleteResponseDto> completeOrder(@PathVariable("orderId") Long orderId) {
        OrderCompleteResponseDto response = orderService.completeOrder(orderId);

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
