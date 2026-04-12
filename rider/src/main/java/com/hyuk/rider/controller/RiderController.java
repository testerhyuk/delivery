package com.hyuk.rider.controller;

import com.hyuk.rider.dto.RequestOrder;
import com.hyuk.rider.service.RiderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/rider-service")
public class RiderController {
    private final RiderService riderService;

    @PostMapping("/complete")
    public ResponseEntity<Void> completeDelivery(@RequestBody RequestOrder order) {
        riderService.completeDelivery(order);

        return ResponseEntity.ok().build();
    }

    @PostMapping("/accept/{orderId}")
    public ResponseEntity<Void> acceptDelivery(
            @PathVariable String orderId,
            @RequestHeader("userId") String userId) {
        riderService.acceptDelivery(orderId, userId);
        return ResponseEntity.ok().build();
    }
}
