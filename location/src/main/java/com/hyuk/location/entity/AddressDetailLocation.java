package com.hyuk.location.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;

import java.time.LocalDateTime;

@Entity
@Table(name = "address_detail_location",
        uniqueConstraints = @UniqueConstraint(columnNames = {"mgmt_num", "detail_address"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AddressDetailLocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "mgmt_num", length = 40)
    private String mgmtNum;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mgmt_num", insertable = false, updatable = false)
    private AddressMaster addressMaster;

    @Column(name = "detail_address", length = 100)
    private String detailAddress;

    @Column(name = "location", columnDefinition = "geometry(Point, 4326)")
    private Point location;

    @Column(name = "sample_count")
    private Integer sampleCount;

    @Column(name = "confidence")
    private Double confidence;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Builder
    public AddressDetailLocation(String mgmtNum, String detailAddress, Point location) {
        this.mgmtNum = mgmtNum;
        this.detailAddress = detailAddress;
        this.location = location;
        this.sampleCount = 1;
        this.confidence = 0.1;
        this.updatedAt = LocalDateTime.now();
    }

    public void updateWithNewLocation(Point newLocation) {
        double avgLon = (this.location.getX() * sampleCount + newLocation.getX()) / (sampleCount + 1);
        double avgLat = (this.location.getY() * sampleCount + newLocation.getY()) / (sampleCount + 1);

        GeometryFactory gf = new GeometryFactory(new PrecisionModel(), 4326);
        this.location = gf.createPoint(new Coordinate(avgLon, avgLat));
        this.sampleCount += 1;
        this.confidence = Math.min(1.0, sampleCount / 10.0);
        this.updatedAt = LocalDateTime.now();
    }
}
