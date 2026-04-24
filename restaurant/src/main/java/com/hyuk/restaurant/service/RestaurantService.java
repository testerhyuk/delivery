package com.hyuk.restaurant.service;

import com.hyuk.restaurant.dto.ResponseRestaurant;

import java.util.List;

public interface RestaurantService {
    List<ResponseRestaurant> getRestaurantByName(String name, double latitude, double longitude, int page, int size);
    List<ResponseRestaurant> getRestaurantByCategory(String category, double latitude, double longitude, int page, int size);
    ResponseRestaurant getRestaurantById(String restaurantId);
}
