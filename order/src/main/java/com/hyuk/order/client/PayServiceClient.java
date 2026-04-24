package com.hyuk.order.client;

import com.hyuk.order.dto.PayRequestDto;
import com.hyuk.order.dto.ResponsePayReady;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
        name = "pay-service",
//        url = "${PAY_SERVICE_URL:http://localhost:8000/pay-service}"
        url = "http://localhost:9094/pay-service"
)
public interface PayServiceClient {
    @PostMapping("/pay/ready")
    ResponsePayReady readyPayment(@RequestBody PayRequestDto payRequestDto);
}
