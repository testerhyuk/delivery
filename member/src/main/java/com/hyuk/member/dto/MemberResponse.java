package com.hyuk.member.dto;

import com.hyuk.member.entity.enums.Role;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@NoArgsConstructor
public class MemberResponse {
    private Long id;
    private String memberId;
    private String restaurantId;
    private String email;
    private String name;
    private String address;
    private String detailAddress;
    private Set<Role> roles;
}
