package com.hyuk.restaurant.repository;

import com.hyuk.restaurant.dto.ResponseRestaurant;
import com.hyuk.restaurant.entity.RestaurantEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.ResponseEntity;

import java.util.List;

public interface RestaurantRepository extends JpaRepository<RestaurantEntity, Long> {
    List<RestaurantEntity> findByNameContaining(String name);
    List<RestaurantEntity> findByCategory(String category);
}
