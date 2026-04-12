package com.hyuk.rider.entity;

import com.hyuk.common.Snowflake;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Table(name = "rider")
public class RiderEntity {
    @Id
    private Long id;

    @Column(nullable = false, unique = true)
    private String riderId;

    private String orderId;
    private String restaurantId;
    private String deliveryAddress;
    private Integer price;
    @OneToMany(mappedBy = "rider", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Menu> menuList = new ArrayList<>();
    private LocalDateTime deliveryStartedAt;
    private LocalDateTime deliveryFinishedAt;

    public static RiderEntity create(Long id, String orderId, String restaurantId, String deliveryAddress, Integer price, List<Menu> menuList) {
        RiderEntity rider = new RiderEntity();
        rider.id = id;
        rider.riderId = Snowflake.prefixedId("rider", id);
        rider.orderId = orderId;
        rider.restaurantId = restaurantId;
        rider.deliveryAddress = deliveryAddress;
        rider.price = price;

        for (Menu menu : menuList) {
            rider.addMenu(menu);
        }

        rider.deliveryStartedAt = LocalDateTime.now();

        return rider;
    }

    public void addMenu(Menu menu) {
        menuList.add(menu);
        menu.confirmRiderMenu(this);
    }

    public void finishedDelivery() {
        deliveryFinishedAt = LocalDateTime.now();
    }
}
