package com.hyuk.member.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AddressRequest {
    private String address;
    private String detailAddress;
    private BigDecimal latitude;
    private BigDecimal longitude;
}
