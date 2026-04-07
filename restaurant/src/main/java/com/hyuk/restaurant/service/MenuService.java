package com.hyuk.restaurant.service;

import com.hyuk.restaurant.dto.ResponseMenu;

import java.util.List;

public interface MenuService {
    List<ResponseMenu> getMenu(Long restaurantId);
}
