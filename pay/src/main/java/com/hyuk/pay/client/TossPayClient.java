package com.hyuk.pay.client;

import com.hyuk.pay.config.TossFeignConfig;
import com.hyuk.pay.dto.RequestOrder;
import com.hyuk.pay.dto.ResponseOrder;
import com.hyuk.pay.dto.TossCancelResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient(
        name = "tossClient",
        url = "https://api.tosspayments.com/v1/payments",
        configuration = TossFeignConfig.class
)
public interface TossPayClient {
    @PostMapping("/confirm")
    public ResponseOrder confirm(@RequestBody RequestOrder requestOrder);

    @PostMapping("/{paymentKey}/cancel")
    TossCancelResponse cancel(
            @PathVariable("paymentKey") String paymentKey,
            @RequestBody Map<String, String> cancelReason
    );
}
