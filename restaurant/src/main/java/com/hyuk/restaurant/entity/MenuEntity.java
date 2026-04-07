package com.hyuk.restaurant.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Table(name = "menus")
public class MenuEntity {
    @Id
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "restaurant_id")
    private RestaurantEntity restaurant;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Integer price;

    public static MenuEntity create(Long id, String name, Integer price, RestaurantEntity restaurant) {
        MenuEntity menu = new MenuEntity();
        menu.id = id;
        menu.name = name;
        menu.price = price;
        menu.restaurant = restaurant;
        return menu;
    }
}
