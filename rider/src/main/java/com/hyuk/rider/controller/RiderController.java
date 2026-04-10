package com.hyuk.rider.controller;

import com.hyuk.rider.dto.RequestOrder;
import com.hyuk.rider.service.RiderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
