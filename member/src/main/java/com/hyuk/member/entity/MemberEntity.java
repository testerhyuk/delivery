package com.hyuk.member.entity;

import com.hyuk.member.entity.enums.Role;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

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
    private String email;

    private String name;

    private String provider;

    @Column(unique = true)
    private String providerId;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "member_roles", joinColumns = @JoinColumn(name = "member_id"))
    @Enumerated(EnumType.STRING)
    private Set<Role> roles = new HashSet<>();

    // 일반 사용자 가입 전용 팩토리
    public static MemberEntity createUserMember(Long id, String email, String name, String provider, String providerId) {
        MemberEntity memberEntity = new MemberEntity();
        memberEntity.id = id;
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
        memberEntity.email = email;
        memberEntity.name = name;
        memberEntity.provider = provider;
        memberEntity.providerId = providerId;
        memberEntity.addRole(Role.RIDER);

        return memberEntity;
    }

    public void addRole(Role role) {
        this.roles.add(role);
    }
}
