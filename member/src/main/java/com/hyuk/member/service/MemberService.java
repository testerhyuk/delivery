package com.hyuk.member.service;

import com.hyuk.member.dto.MemberResponse;

public interface MemberService {
    MemberResponse joinUser(String email, String name, String provider, String providerId);
    MemberResponse joinRider(String email, String name, String provider, String providerId);
}
