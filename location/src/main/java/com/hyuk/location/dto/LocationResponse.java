package com.hyuk.location.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LocationResponse {
    private Double lat;
    private Double lon;
    private Integer accuracyLevel;
    private String mgmtNum;

    public static LocationResponse empty() {
        return LocationResponse.builder().build();
    }

    public boolean hasLocation() {
        return lat != null && lon != null;
    }
}
