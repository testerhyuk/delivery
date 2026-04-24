package com.hyuk.restaurant.entity;

import com.hyuk.common.Snowflake;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "restaurant")
public class RestaurantEntity {
    @Id
    @Column(nullable = false, unique = true)
    private Long id;
    @Column(nullable = false, unique = true, name = "restaurant_id")
    private String restaurantId;
    @Column(nullable = false)
    private String name;
    @Column(nullable = false)
    private String address;
    @Column(nullable = false)
    private String category;
    @Column(nullable = false, precision = 18, scale = 14)
    private BigDecimal longitude;
    @Column(nullable = false, precision = 18, scale = 14)
    private BigDecimal latitude;
    @OneToMany(mappedBy = "restaurant", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MenuEntity> menus = new ArrayList<>();

    @PrePersist
    public void assignRestaurantId() {
        if (restaurantId == null && id != null) {
            restaurantId = Snowflake.prefixedId("restaurant", id);
        }
    }
}
