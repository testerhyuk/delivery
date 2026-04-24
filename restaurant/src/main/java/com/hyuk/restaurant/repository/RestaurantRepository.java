package com.hyuk.restaurant.repository;

import com.hyuk.restaurant.entity.RestaurantEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RestaurantRepository extends JpaRepository<RestaurantEntity, Long> {
    RestaurantEntity findByRestaurantId(String restaurantId);

    @Query(value = """
    WITH nearby AS MATERIALIZED (
        SELECT id, restaurant_id, name, address, category, longitude, latitude,
               ST_DistanceSphere(location, ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)) as dist
        FROM restaurant
        WHERE category = :category
          AND ST_DWithin(location, ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326), :radius / (111320.0 * cos(radians(37.0))))
    )
    SELECT * FROM nearby WHERE dist <= :radius ORDER BY dist LIMIT :limit OFFSET :offset
    """, nativeQuery = true)
    List<RestaurantEntity> findByCategoryAndLocation(
            @Param("category") String category,
            @Param("latitude") double latitude,
            @Param("longitude") double longitude,
            @Param("radius") double radius,
            @Param("limit") int limit,
            @Param("offset") int offset
    );

    @Query(value = """
    WITH nearby AS MATERIALIZED (
        SELECT id, restaurant_id, name, address, category, longitude, latitude,
            ST_DistanceSphere(location, ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)) as dist
        FROM restaurant
        WHERE ST_DWithin(location, ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326), :radius / (111320.0 * cos(radians(37.0))))
    )
    SELECT * FROM nearby WHERE name LIKE CONCAT('%', :name, '%') AND dist <= :radius ORDER BY dist LIMIT :limit OFFSET :offset
    """, nativeQuery = true)
    List<RestaurantEntity> findByNameContainingAndLocation(
            @Param("name") String name,
            @Param("latitude") double latitude,
            @Param("longitude") double longitude,
            @Param("radius") double radius,
            @Param("limit") int limit,
            @Param("offset") int offset
    );
}
