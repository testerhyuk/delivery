package com.hyuk.member.entity.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Role {
    USER("ROLE_USER", "일반 사용자"),
    RIDER("ROLE_RIDER", "배달 기사"),
    ADMIN("ROLE_ADMIN", "관리자");

    private final String key;
    private final String title;
}
