package com.hyuk.seller.controller;

import com.hyuk.seller.service.SellerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/seller-service")
public class SellerController {
    private final SellerService sellerService;

    @PostMapping("/delivery-start/{orderId}")
    public ResponseEntity<Void> deliveryStarted(@PathVariable("orderId") String orderId) {
        sellerService.deliveryStarted(orderId);

        return ResponseEntity.ok().build();
    }
}
