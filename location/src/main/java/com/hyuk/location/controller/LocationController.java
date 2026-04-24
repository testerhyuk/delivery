package com.hyuk.location.controller;

import com.hyuk.location.dto.LocationRequest;
import com.hyuk.location.dto.LocationResponse;
import com.hyuk.location.service.LocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/location-service")
@RequiredArgsConstructor
public class LocationController {

    private final LocationService locationService;

    @PostMapping("/resolve")
    public ResponseEntity<LocationResponse> resolve(@RequestBody LocationRequest request) {
        LocationResponse response = locationService.resolveLocation(
                request.getAddress(), request.getDetailAddress()
        );

        return ResponseEntity.ok(response);
    }
}
