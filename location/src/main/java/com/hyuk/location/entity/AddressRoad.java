package com.hyuk.location.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "address_road")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AddressRoad {

    @Id
    @Column(name = "mgmt_num", length = 40)
    private String mgmtNum;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mgmt_num", insertable = false, updatable = false)
    private AddressMaster addressMaster;

    @Column(name = "sido", length = 20)
    private String sido;

    @Column(name = "sigungu", length = 20)
    private String sigungu;

    @Column(name = "b_dong_name", length = 20)
    private String bDongName;

    @Column(name = "road_name", length = 50)
    private String roadName;

    @Column(name = "build_main")
    private Integer buildMain;

    @Column(name = "build_sub")
    private Integer buildSub;

    @Column(name = "zip_code", length = 5)
    private String zipCode;

    @Column(name = "build_nm_official", length = 100)
    private String buildNmOfficial;

    @Column(name = "build_nm_sgg", length = 100)
    private String buildNmSgg;
}
