package com.hyuk.rider.entity;

import com.hyuk.common.Snowflake;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Table(name = "rider_menu")
public class Menu {
    @Id
    private Long id;

    @Column(nullable = false, unique = true)
    private String menuId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rider_id")
    private RiderEntity rider;
    private String menuName;
    private Integer price;
    private Integer quantity;

    public static Menu create(Long id, String menuName, Integer price, Integer quantity) {
        Menu menu = new Menu();
        menu.id = id;
        menu.menuId = Snowflake.prefixedId("riderMenu", id);
        menu.menuName = menuName;
        menu.price = price;
        menu.quantity = quantity;

        return menu;
    }

    protected void confirmRiderMenu(RiderEntity rider) {
        this.rider = rider;
    }
}
