package com.hyuk.rider.client;

import com.hyuk.rider.dto.RestaurantResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "restaurant-service", url = "http://localhost:9101")
public interface RestaurantServiceClient {
    @GetMapping("/restaurant-service/restaurant/{restaurantId}")
    RestaurantResponse getRestaurant(@PathVariable("restaurantId") String restaurantId);
}
