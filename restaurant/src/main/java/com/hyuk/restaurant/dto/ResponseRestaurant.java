package com.hyuk.restaurant.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ResponseRestaurant {
    private Long id;
    private String restaurantId;
    private String name;
    private String address;
    private String category;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private String eumCard;
}
