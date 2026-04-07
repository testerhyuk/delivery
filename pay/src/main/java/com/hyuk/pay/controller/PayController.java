package com.hyuk.pay.controller;

import com.hyuk.pay.dto.RequestOrder;
import com.hyuk.pay.dto.ResponseOrder;
import com.hyuk.pay.dto.ResponsePayReady;
import com.hyuk.pay.service.PayService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/pay-service")
@RequiredArgsConstructor
public class PayController {
    private final PayService payService;

    @PostMapping("/pay/ready")
    public ResponseEntity<ResponsePayReady> readyPayment(@RequestBody RequestOrder requestOrder) {
        ResponsePayReady response = payService.readyPayment(requestOrder);

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PostMapping("/pay/confirm")
    public ResponseEntity<ResponseOrder> confirmPayment(@RequestBody RequestOrder requestOrder) {
        ResponseOrder responseOrder = payService.confirmPayment(requestOrder);

        return ResponseEntity.status(HttpStatus.OK).body(responseOrder);
    }
}
