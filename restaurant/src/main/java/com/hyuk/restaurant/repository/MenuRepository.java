package com.hyuk.restaurant.repository;

import com.hyuk.restaurant.entity.MenuEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MenuRepository extends JpaRepository<MenuEntity, Long> {
    boolean existsByRestaurantId(Long restaurantId);
}
