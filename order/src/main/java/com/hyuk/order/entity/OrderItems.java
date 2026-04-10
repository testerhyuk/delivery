package com.hyuk.order.entity;

import com.hyuk.common.Snowflake;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "order_items")
@Getter
public class OrderItems {
    @Id
    private Long id;

    @Column(nullable = false, unique = true)
    private String orderItemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private OrderEntity order;
    @Column(nullable = false)
    private String menuId;
    @Column(nullable = false)
    private String menuName;
    @Column(nullable = false)
    private Integer price;
    @Column(nullable = false)
    private Integer quantity;

    public static OrderItems create(Long id, String menuId, String menuName, Integer price, Integer quantity) {
        OrderItems orderItems = new OrderItems();
        orderItems.id = id;
        orderItems.orderItemId = Snowflake.prefixedId("orderItem", id);
        orderItems.menuId = menuId;
        orderItems.menuName = menuName;
        orderItems.price = price;
        orderItems.quantity = quantity;

        return orderItems;
    }

    protected void confirmOrder(OrderEntity order) {
        this.order = order;
    }
}
