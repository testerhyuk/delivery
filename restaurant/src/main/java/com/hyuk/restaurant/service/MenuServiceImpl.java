package com.hyuk.restaurant.service;

import com.hyuk.restaurant.dto.ResponseMenu;
import com.hyuk.restaurant.entity.MenuEntity;
import com.hyuk.restaurant.repository.MenuRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MenuServiceImpl implements MenuService {
    private final MenuRepository menuRepository;

    @Override
    public List<ResponseMenu> getMenu(String restaurantId) {
        List<MenuEntity> entities;
        try {
            long pk = Long.parseLong(restaurantId);
            entities = menuRepository.findByRestaurant_Id(pk);
        } catch (NumberFormatException e) {
            entities = menuRepository.findByRestaurant_RestaurantId(restaurantId);
        }
        List<ResponseMenu> response = new ArrayList<>();

        entities.forEach(entity -> {
            ResponseMenu responseMenu = new ResponseMenu();
            responseMenu.setId(entity.getId());
            responseMenu.setName(entity.getName());
            responseMenu.setPrice(entity.getPrice());
            responseMenu.setMenuId(entity.getMenuId());

            response.add(responseMenu);
        });

        return response;
    }
}
