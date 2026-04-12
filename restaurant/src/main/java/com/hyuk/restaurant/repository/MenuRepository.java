package com.hyuk.restaurant.repository;

import com.hyuk.restaurant.entity.MenuEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MenuRepository extends JpaRepository<MenuEntity, Long> {
    List<MenuEntity> findByRestaurant_Id(Long restaurantId);

    List<MenuEntity> findByRestaurant_RestaurantId(String restaurantId);
}
