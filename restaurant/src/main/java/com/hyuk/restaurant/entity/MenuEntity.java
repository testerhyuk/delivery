package com.hyuk.restaurant.entity;

import com.hyuk.common.Snowflake;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Table(name = "restaurant_menus")
public class MenuEntity {
    @Id
    private Long id;

    @Column(nullable = false, unique = true)
    private String menuId;

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
        menu.menuId = Snowflake.prefixedId("menu", id);
        menu.name = name;
        menu.price = price;
        menu.restaurant = restaurant;
        return menu;
    }
}
