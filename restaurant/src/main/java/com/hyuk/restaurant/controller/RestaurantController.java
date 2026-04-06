package com.hyuk.restaurant.controller;

import com.hyuk.restaurant.dto.RequestLatAndLong;
import com.hyuk.restaurant.dto.ResponseRestaurant;
import com.hyuk.restaurant.service.RestaurantService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/restaurant-service")
public class RestaurantController {
    private final RestaurantService restaurantService;

    @PostMapping("/search/name")
    public ResponseEntity<List<ResponseRestaurant>> searchByName(@RequestParam("name") String name, @RequestBody RequestLatAndLong request){
        List<ResponseRestaurant> response = restaurantService.getRestaurantByName(
                name, request.getLatitude().doubleValue(), request.getLongitude().doubleValue()
        );

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PostMapping("/search/category")
    public ResponseEntity<List<ResponseRestaurant>> searchByCategory(@RequestParam("category") String category, @RequestBody RequestLatAndLong request){
        List<ResponseRestaurant> response = restaurantService.getRestaurantByCategory(
                category, request.getLatitude().doubleValue(), request.getLongitude().doubleValue()
        );

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
