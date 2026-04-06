package com.hyuk.order.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(
        name = "restaurant-service",
        url = "${RESTAURANT_SERVICE_URL:http://localhost:9092}"
)
public interface RestaurantServiceClient {
    @GetMapping("/restaurant-service/")
}
