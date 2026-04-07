package com.hyuk.pay.client;

import com.hyuk.pay.dto.PayConfirmedRequestDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
        name = "order-service",
        url = "${ORDER_SERVICE_URL:http://localhost:8000/order-service}"
)
public interface OrderServiceClient {
    @PostMapping("/paid")
    void updatePaid(@RequestBody PayConfirmedRequestDto PayConfirmedRequestDto);
}
