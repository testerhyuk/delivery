package com.hyuk.location.util;

import lombok.Getter;

@Getter
public class ParsedAddress {
    private final String roadName;
    private final Integer buildMain;
    private final String fullAddress;

    private ParsedAddress(String roadName, Integer buildMain, String fullAddress) {
        this.roadName = roadName;
        this.buildMain = buildMain;
        this.fullAddress = fullAddress;
    }

    public static ParsedAddress from(String address) {
        String[] parts = address.trim().split("\\s+");

        try {
            int buildMain = Integer.parseInt(parts[parts.length - 1]);
            String roadName = parts[parts.length - 2];
            return new ParsedAddress(roadName, buildMain, address);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("주소 파싱 실패: " + address);
        }
    }
}
