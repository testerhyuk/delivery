package com.hyuk.rider.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class RestaurantResponse {
    private Long id;
    private String restaurantId;
    private String name;
    private String address;
    private BigDecimal latitude;
    private BigDecimal longitude;
}