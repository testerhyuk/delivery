package com.hyuk.seller.entity;

import com.hyuk.common.Snowflake;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "seller_menu")
public class Menu {
    @Id
    private Long id;

    @Column(nullable = false, unique = true)
    private String menuId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id")
    private SellerEntity seller;
    private String menuName;
    private Integer price;
    private Integer quantity;

    public static Menu create(Long id, String menuName, Integer price, Integer quantity) {
        Menu menu = new Menu();
        menu.id = id;
        menu.menuId = Snowflake.prefixedId("sellerMenu", id);
        menu.menuName = menuName;
        menu.price = price;
        menu.quantity = quantity;

        return menu;
    }

    protected void confirmSellerMenu(SellerEntity seller) {
        this.seller = seller;
    }
}
