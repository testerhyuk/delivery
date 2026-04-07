package com.hyuk.restaurant.repository;

import com.hyuk.restaurant.entity.MenuEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MenuRepository extends JpaRepository<MenuEntity, Long> {
    boolean existsByRestaurantId(Long restaurantId);

    List<MenuEntity> findByRestaurantId(Long restaurantId);
}
