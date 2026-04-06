package com.hyuk.restaurant.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class RequestLatAndLong {
    private BigDecimal latitude;
    private BigDecimal longitude;
}
