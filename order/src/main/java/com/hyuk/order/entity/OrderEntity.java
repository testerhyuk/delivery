package com.hyuk.order.entity;

import com.hyuk.order.entity.enums.OrderStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "orders")
@Getter
public class OrderEntity {
    @Id
    private Long id;

    @Column(nullable = false)
    private String userId;
    @Column(nullable = false)
    private Long restaurantId;
    @Enumerated(EnumType.STRING)
    private OrderStatus orderStatus;
    @Column(nullable = false)
    private Integer totalPrice;
    @Column(nullable = false, length = 500)
    private String deliveryAddress;
    @Column(nullable = false, precision = 18, scale = 14)
    private BigDecimal userLatitude;
    @Column(nullable = false, precision = 18, scale = 14)
    private BigDecimal userLongitude;
    @Column(nullable = false)
    private LocalDateTime orderAt;
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItems> orderItems = new ArrayList<>();

    public static OrderEntity create(Long id, String userId, Long restaurantId, Integer totalPrice, String deliveryAddress,
                                     BigDecimal userLatitude, BigDecimal userLongitude, List<OrderItems> orderItems) {
        OrderEntity orderEntity = new OrderEntity();
        orderEntity.id = id;
        orderEntity.userId = userId;
        orderEntity.restaurantId = restaurantId;
        orderEntity.orderStatus = OrderStatus.PENDING;
        orderEntity.totalPrice = totalPrice;
        orderEntity.deliveryAddress = deliveryAddress;
        orderEntity.userLatitude = userLatitude;
        orderEntity.userLongitude = userLongitude;
        orderEntity.orderAt = LocalDateTime.now();

        for (OrderItems item : orderItems) {
            orderEntity.addOrderItem(item);
        }

        return orderEntity;
    }

    public void addOrderItem(OrderItems item) {
        this.orderItems.add(item);
        item.confirmOrder(this);
    }

    public void updateToPaid() {
        this.orderStatus = OrderStatus.PAID;
    }

    public void updateToCooking() {
        this.orderStatus = OrderStatus.COOKING;
    }

    public void updateToDelivering() {
        this.orderStatus = OrderStatus.DELIVERING;
    }

    public void updateToCompleted() {
        this.orderStatus = OrderStatus.COMPLETED;
    }

    public void updateToCancelled() {
        this.orderStatus = OrderStatus.CANCELED;
    }
}
