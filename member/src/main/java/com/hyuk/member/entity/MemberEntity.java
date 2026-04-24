package com.hyuk.member.entity;

import com.hyuk.common.Snowflake;
import com.hyuk.member.entity.enums.Role;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Table(name = "member")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberEntity {
    @Id
    private Long id;

    @Column(nullable = false, unique = true)
    private String memberId;

    @Column(nullable = false, unique = true)
    private String email;

    private String name;

    private String provider;

    @Column(unique = true)
    private String providerId;

    private String address;
    private String detailAddress;
    @Column(nullable = false, precision = 18, scale = 14)
    private BigDecimal latitude;
    @Column(nullable = false, precision = 18, scale = 14)
    private BigDecimal longitude;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "member_roles", joinColumns = @JoinColumn(name = "member_id"))
    @Enumerated(EnumType.STRING)
    private Set<Role> roles = new HashSet<>();

    private String restaurantId;

    // 일반 사용자 가입 전용 팩토리
    public static MemberEntity createUserMember(Long id, String email, String name, String provider, String providerId) {
        MemberEntity memberEntity = new MemberEntity();
        memberEntity.id = id;
        memberEntity.memberId = Snowflake.prefixedId("member", id);
        memberEntity.email = email;
        memberEntity.name = name;
        memberEntity.provider = provider;
        memberEntity.providerId = providerId;
        memberEntity.addRole(Role.USER);

        return memberEntity;
    }

    // 배달 기사 가입 전용 팩토리
    public static MemberEntity createRiderMember(Long id, String email, String name, String provider, String providerId) {
        MemberEntity memberEntity = new MemberEntity();
        memberEntity.id = id;
        memberEntity.memberId = Snowflake.prefixedId("member", id);
        memberEntity.email = email;
        memberEntity.name = name;
        memberEntity.provider = provider;
        memberEntity.providerId = providerId;
        memberEntity.addRole(Role.RIDER);

        return memberEntity;
    }

    // 판매점 가입 전용 팩토리
    public static MemberEntity createSellerMember(Long id, String email, String name, String provider, String providerId) {
        MemberEntity memberEntity = new MemberEntity();
        memberEntity.id = id;
        memberEntity.memberId = Snowflake.prefixedId("member", id);
        memberEntity.email = email;
        memberEntity.name = name;
        memberEntity.provider = provider;
        memberEntity.providerId = providerId;
        memberEntity.addRole(Role.SELLER);

        return memberEntity;
    }

    public void addRole(Role role) {
        this.roles.add(role);
    }

    public void addAddress(String address, String detailAddress, BigDecimal latitude, BigDecimal longitude) {
        this.address = address;
        this.detailAddress = detailAddress;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public void updateAddress(String address, String detailAddress, BigDecimal latitude, BigDecimal longitude) {
        this.address = address;
        this.detailAddress = detailAddress;
        this.latitude = latitude;
        this.longitude = longitude;
    }
}
