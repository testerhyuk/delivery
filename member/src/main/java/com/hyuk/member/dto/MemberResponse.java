package com.hyuk.member.dto;

import com.hyuk.member.entity.enums.Role;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@NoArgsConstructor
public class MemberResponse {
    private Long id;
    private String email;
    private String name;
    private Set<Role> roles;
}
