package com.hyuk.location.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.locationtech.jts.geom.Point;

import java.time.LocalDateTime;

@Entity
@Table(name = "address_location")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AddressLocation {

    @Id
    @Column(name = "mgmt_num", length = 40)
    private String mgmtNum;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mgmt_num", insertable = false, updatable = false)
    private AddressMaster addressMaster;

    @Column(name = "location", columnDefinition = "geometry(Point, 4326)")
    private Point location;

    @Column(name = "accuracy_level")
    private Short accuracyLevel;

    @Column(name = "source", length = 20)
    private String source;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Builder
    public AddressLocation(String mgmtNum, Point location, Short accuracyLevel, String source) {
        this.mgmtNum = mgmtNum;
        this.location = location;
        this.accuracyLevel = accuracyLevel;
        this.source = source;
        this.updatedAt = LocalDateTime.now();
    }

    public void updateLocation(Point location, Short accuracyLevel, String source) {
        this.location = location;
        this.accuracyLevel = accuracyLevel;
        this.source = source;
        this.updatedAt = LocalDateTime.now();
    }
}
