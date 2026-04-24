package com.hyuk.location.client;

import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class NominatimClient {
    private final RestTemplate restTemplate = new RestTemplate();
    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

    @Value("${nominatim.url}")
    private String nominatimUrl;

    public Point search(String address) {
        try {
            String url = nominatimUrl + "/search?q={q}&format=json&limit=1";
            List<Map> result = restTemplate.getForObject(url, List.class, address);

            if (result == null || result.isEmpty()) {
                log.warn("Nominatim 결과 없음: {}", address);
                return null;
            }

            double lat = Double.parseDouble((String) result.get(0).get("lat"));
            double lon = Double.parseDouble((String) result.get(0).get("lon"));

            return geometryFactory.createPoint(new Coordinate(lon, lat));

        } catch (Exception e) {
            log.error("Nominatim 호출 실패: {}", e.getMessage());
            return null;
        }
    }
}
