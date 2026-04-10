package com.hyuk.restaurant.service;

import com.hyuk.restaurant.calculator.DistanceCalculator;
import com.hyuk.restaurant.dto.ResponseRestaurant;
import com.hyuk.restaurant.repository.RestaurantRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class RestaurantServiceImpl implements RestaurantService {
    private final RestaurantRepository restaurantRepository;
    private final DistanceCalculator distanceCalculator;
    private final ModelMapper modelMapper;

    private final double RADIUS_LIMIT = 5.0; // 반경 5km

    @Override
    public List<ResponseRestaurant> getRestaurantByName(String name, double latitude, double longitude) {
        return restaurantRepository.findByNameContaining(name).stream()
                .map(r -> {
                    double dist = distanceCalculator.calculateDistance(
                            latitude, longitude, r.getLatitude().doubleValue(), r.getLongitude().doubleValue());
                    return new Object[] { r, dist }; // [식당객체, 거리값] 묶음
                })
                .filter(arr -> (double) arr[1] <= RADIUS_LIMIT)
                .sorted(Comparator.comparingDouble(arr -> (double) arr[1]))
                .map(arr -> modelMapper.map(arr[0], ResponseRestaurant.class))
                .toList();
    }

    @Override
    public List<ResponseRestaurant> getRestaurantByCategory(String category, double latitude, double longitude) {
        return restaurantRepository.findByCategory(category).stream()
                .map(entity -> {
                    double distance = distanceCalculator.calculateDistance(
                            latitude, longitude,
                            entity.getLatitude().doubleValue(),
                            entity.getLongitude().doubleValue());
                    return Map.entry(entity, distance);
                })
                .filter(entry -> entry.getValue() <= RADIUS_LIMIT)
                .sorted(Map.Entry.comparingByValue())
                .map(entry -> {
                    return modelMapper.map(entry.getKey(), ResponseRestaurant.class);
                })
                .toList();
    }
}
