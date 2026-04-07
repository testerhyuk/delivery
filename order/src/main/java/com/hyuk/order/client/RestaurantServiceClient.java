package com.hyuk.order.client;

import com.hyuk.order.dto.ResponseMenu;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(
        name = "restaurant-service",
        url = "${RESTAURANT_SERVICE_URL:http://localhost:9092/restaurant-service}"
)
public interface RestaurantServiceClient {
    @GetMapping("/menu/{restaurantId}")
    List<ResponseMenu> getMenu(@PathVariable("restaurantId") Long restaurantId);
}
