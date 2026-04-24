package com.hyuk.restaurant.service;

import com.hyuk.restaurant.dto.ResponseRestaurant;
import com.hyuk.restaurant.entity.RestaurantEntity;
import com.hyuk.restaurant.repository.RestaurantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class RestaurantServiceImpl implements RestaurantService {
    private final RestaurantRepository restaurantRepository;
    private final ModelMapper modelMapper;

    private final double RADIUS_LIMIT = 5000; // 반경 5km

    @Override
    public List<ResponseRestaurant> getRestaurantByName(String name, double latitude, double longitude, int page, int size) {
        return restaurantRepository.findByNameContainingAndLocation(name, latitude, longitude, RADIUS_LIMIT, size, page * size)
                .stream()
                .map(entity -> modelMapper.map(entity, ResponseRestaurant.class))
                .toList();
    }

    @Override
    @Cacheable(value = "categories", key = "#category + ':' + #page")
    public List<ResponseRestaurant> getRestaurantByCategory(String category, double latitude, double longitude, int page, int size) {
        return restaurantRepository.findByCategoryAndLocation(category, latitude, longitude, RADIUS_LIMIT, size, page * size)
                .stream()
                .map(entity -> modelMapper.map(entity, ResponseRestaurant.class))
                .toList();
    }

    @Override
    public ResponseRestaurant getRestaurantById(String restaurantId) {
        RestaurantEntity entity = restaurantRepository.findByRestaurantId(restaurantId);

        if (entity == null) {
            throw new RuntimeException("Restaurant not found");
        }

        return modelMapper.map(entity, ResponseRestaurant.class);
    }
}
