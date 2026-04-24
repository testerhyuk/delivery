package com.hyuk.order.entity;

import com.hyuk.common.Snowflake;
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

    @Column(nullable = false, unique = true)
    private String orderId;

    @Column(nullable = false)
    private String userId;
    @Column(nullable = false)
    private String restaurantId;
    @Enumerated(EnumType.STRING)
    private OrderStatus orderStatus;
    @Column(nullable = false)
    private Integer totalPrice;
    @Column(nullable = false, length = 500)
    private String deliveryAddress;
    @Column(nullable = false, length = 100)
    private String detailAddress;
    @Column(nullable = false, precision = 18, scale = 14)
    private BigDecimal restaurantLatitude;
    @Column(nullable = false, precision = 18, scale = 14)
    private BigDecimal restaurantLongitude;
    @Column(nullable = false, precision = 18, scale = 14)
    private BigDecimal userLatitude;
    @Column(nullable = false, precision = 18, scale = 14)
    private BigDecimal userLongitude;
    @Column(nullable = false)
    private LocalDateTime orderAt;
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItems> orderItems = new ArrayList<>();

    public static OrderEntity create(Long id, String userId, String restaurantId, Integer totalPrice, String deliveryAddress,
                                     String detailAddress, BigDecimal restaurantLatitude, BigDecimal restaurantLongitude,
                                     BigDecimal userLatitude, BigDecimal userLongitude, List<OrderItems> orderItems) {
        OrderEntity orderEntity = new OrderEntity();
        orderEntity.id = id;
        orderEntity.orderId = Snowflake.prefixedId("order", id);
        orderEntity.userId = userId;
        orderEntity.restaurantId = restaurantId;
        orderEntity.orderStatus = OrderStatus.PENDING;
        orderEntity.totalPrice = totalPrice;
        orderEntity.deliveryAddress = deliveryAddress;
        orderEntity.detailAddress = detailAddress;
        orderEntity.restaurantLatitude = restaurantLatitude;
        orderEntity.restaurantLongitude = restaurantLongitude;
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

    public void updateToDeliveryStart() {
        this.orderStatus = OrderStatus.DELIVERY_START;
    }

    public void updateToCompleted() {
        this.orderStatus = OrderStatus.COMPLETED;
    }

    public void updateToCancelled() {
        this.orderStatus = OrderStatus.CANCELED;
    }
}
