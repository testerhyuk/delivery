package com.hyuk.restaurant.service;

import com.hyuk.restaurant.dto.ResponseMenu;
import com.hyuk.restaurant.entity.MenuEntity;
import com.hyuk.restaurant.repository.MenuRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MenuServiceImpl implements MenuService {
    private final MenuRepository menuRepository;

    @Override
    public List<ResponseMenu> getMenu(String restaurantId) {
        List<MenuEntity> entities = menuRepository.findByRestaurant_RestaurantId(restaurantId);

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
