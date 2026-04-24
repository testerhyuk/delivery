package com.hyuk.location.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.locationtech.jts.geom.Point;

import java.time.LocalDateTime;

@Entity
@Table(name = "address_master")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AddressMaster {

    @Id
    @Column(name = "mgmt_num", length = 40)
    private String mgmtNum;

    @Column(name = "sido", length = 20)
    private String sido;

    @Column(name = "sigungu", length = 20)
    private String sigungu;

    @Column(name = "zip_code", length = 5)
    private String zipCode;

    @Column(name = "location", columnDefinition = "geometry(Point, 4326)")
    private Point location;

    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;
}
