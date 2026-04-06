package com.hyuk.restaurant.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "restaurant")
public class RestaurantEntity {
    @Id
    @Column(nullable = false, unique = true)
    private Long id;
    @Column(nullable = false)
    private String name;
    @Column(nullable = false)
    private String address;
    @Column(nullable = false)
    private String category;
    @Column(nullable = false)
    private BigDecimal longitude;
    @Column(nullable = false)
    private BigDecimal latitude;
    @Column(nullable = true)
    private String eumCard;


}
