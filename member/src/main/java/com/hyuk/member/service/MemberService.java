package com.hyuk.member.service;

import com.hyuk.member.dto.AddressResponse;
import com.hyuk.member.dto.MemberResponse;

public interface MemberService {
    MemberResponse joinUser(String email, String name, String provider, String providerId);
    MemberResponse joinRider(String email, String name, String provider, String providerId);
    MemberResponse createAddress(Long id, String address, String detailAddress);
    MemberResponse updateAddress(Long id, String address, String detailAddress);
    AddressResponse getAddress(Long id);
    boolean hasAddress(Long id);
}
