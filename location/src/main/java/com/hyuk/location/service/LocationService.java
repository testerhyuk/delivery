package com.hyuk.location.service;

import com.hyuk.location.client.NominatimClient;
import com.hyuk.location.dto.LocationResponse;
import com.hyuk.location.entity.AddressDetailLocation;
import com.hyuk.location.entity.AddressLocation;
import com.hyuk.location.entity.AddressRoad;
import com.hyuk.location.repository.AddressDetailLocationRepository;
import com.hyuk.location.repository.AddressLocationRepository;
import com.hyuk.location.repository.AddressRoadRepository;
import com.hyuk.location.util.ParsedAddress;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class LocationService {

    private final AddressRoadRepository addressRoadRepository;
    private final AddressLocationRepository addressLocationRepository;
    private final AddressDetailLocationRepository addressDetailLocationRepository;
    private final NominatimClient nominatimClient;
    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

    // 주소 → 좌표 반환 (메인 진입점)
    @Transactional
    public LocationResponse resolveLocation(String address, String detailAddress) {

        ParsedAddress parsed = ParsedAddress.from(address);

        // 1. address_road에서 mgmt_num 조회
        Optional<AddressRoad> roadOpt = addressRoadRepository
                .findByRoadNameAndBuildMain(parsed.getRoadName(), parsed.getBuildMain());

        if (roadOpt.isEmpty()) {
            // DB에 없으면 Nominatim 직접 호출
            log.warn("address_road 미존재, Nominatim 직접 호출: {}", address);
            Point point = nominatimClient.search(address);
            return toResponse(point, 1, null);
        }

        String mgmtNum = roadOpt.get().getMgmtNum();

        // 2. 상세주소 학습 좌표 있으면 우선 사용
        if (detailAddress != null && !detailAddress.isBlank()) {
            Optional<AddressDetailLocation> detailOpt = addressDetailLocationRepository
                    .findByMgmtNumAndDetailAddress(mgmtNum, detailAddress);

            if (detailOpt.isPresent() && detailOpt.get().getConfidence() >= 0.3) {
                log.info("상세주소 학습 좌표 사용: {} {}", address, detailAddress);
                return toResponse(detailOpt.get().getLocation(), 3, mgmtNum);
            }
        }

        // 3. 건물 대표 좌표 있으면 사용
        Optional<AddressLocation> locationOpt = addressLocationRepository.findById(mgmtNum);
        if (locationOpt.isPresent()) {
            log.info("건물 대표 좌표 사용: {}", address);
            return toResponse(locationOpt.get().getLocation(), 1, mgmtNum);
        }

        // 4. Nominatim 호출 → 저장
        Point point = nominatimClient.search(address);
        if (point != null) {
            addressLocationRepository.save(AddressLocation.builder()
                    .mgmtNum(mgmtNum)
                    .location(point)
                    .accuracyLevel((short) 1)
                    .source("nominatim")
                    .build());
            log.info("Nominatim 좌표 저장: {}", address);
        }

        return toResponse(point, 1, mgmtNum);
    }

    // 배달 완료 시 좌표 학습
    @Transactional
    public void learnDeliveryLocation(String mgmtNum, String detailAddress,
                                      double lat, double lon) {
        Point point = geometryFactory.createPoint(new Coordinate(lon, lat));

        addressDetailLocationRepository
                .findByMgmtNumAndDetailAddress(mgmtNum, detailAddress)
                .ifPresentOrElse(
                        existing -> existing.updateWithNewLocation(point),
                        () -> addressDetailLocationRepository.save(
                                AddressDetailLocation.builder()
                                        .mgmtNum(mgmtNum)
                                        .detailAddress(detailAddress)
                                        .location(point)
                                        .build()
                        )
                );

        log.info("배달 완료 좌표 학습: {} {} ({}, {})", mgmtNum, detailAddress, lat, lon);
    }

    // ETA 계산 (미터 거리 → 분)
    public int calculateEta(Point from, Point to) {
        // 위도/경도 차이로 대략적 거리 계산 (Haversine)
        double lat1 = from.getY(), lon1 = from.getX();
        double lat2 = to.getY(), lon2 = to.getX();

        double R = 6371000; // 지구 반지름 (미터)
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double distance = R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        // 평균 20km/h 기준 → 분
        return (int) (distance / 333.0 / 60);
    }

    private LocationResponse toResponse(Point point, int accuracyLevel, String mgmtNum) {
        if (point == null) return LocationResponse.empty();
        return LocationResponse.builder()
                .lat(point.getY())
                .lon(point.getX())
                .accuracyLevel(accuracyLevel)
                .mgmtNum(mgmtNum)
                .build();
    }
}
