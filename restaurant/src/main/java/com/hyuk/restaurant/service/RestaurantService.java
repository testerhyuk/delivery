package com.hyuk.restaurant.service;

import com.hyuk.restaurant.dto.ResponseRestaurant;

import java.util.List;

public interface RestaurantService {
    List<ResponseRestaurant> getRestaurantByName(String name, double latitude, double longitude);
    List<ResponseRestaurant> getRestaurantByCategory(String category, double latitude, double longitude);
    ResponseRestaurant getRestaurantById(String restaurantId);
}
