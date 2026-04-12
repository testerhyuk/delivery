package com.hyuk.restaurant.repository;

import com.hyuk.restaurant.entity.RestaurantEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RestaurantRepository extends JpaRepository<RestaurantEntity, Long> {
    List<RestaurantEntity> findByNameContaining(String name);
    List<RestaurantEntity> findByCategory(String category);
    RestaurantEntity findByRestaurantId(String restaurantId);
}
