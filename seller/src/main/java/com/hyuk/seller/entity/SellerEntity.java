package com.hyuk.seller.entity;

import com.hyuk.common.Snowflake;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "seller")
public class SellerEntity {
    @Id
    private Long id;
    @Column(nullable = false, unique = true)
    private String sellerId;
    private String orderId;
    private String restaurantId;
    private String deliveryAddress;
    private Integer price;
    @OneToMany(mappedBy = "seller", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Menu> menu = new ArrayList<>();
    private LocalDateTime orderAt;
    private LocalDateTime deliveredAt;

    public static SellerEntity create(Long id, String orderId, String restaurantId, String deliveryAddress, Integer price, List<Menu> menu,
                                      LocalDateTime orderAt, LocalDateTime deliveredAt) {
        SellerEntity sellerEntity = new SellerEntity();
        sellerEntity.id = id;
        sellerEntity.sellerId = Snowflake.prefixedId("seller", id);
        sellerEntity.orderId = orderId;
        sellerEntity.restaurantId = restaurantId;
        sellerEntity.deliveryAddress = deliveryAddress;
        sellerEntity.price = price;

        for (Menu m : menu) {
            sellerEntity.addMenu(m);
        }

        sellerEntity.orderAt = orderAt;
        sellerEntity.deliveredAt = deliveredAt;

        return sellerEntity;
    }

    public void addMenu(Menu menuData) {
        this.menu.add(menuData);
    }
}
