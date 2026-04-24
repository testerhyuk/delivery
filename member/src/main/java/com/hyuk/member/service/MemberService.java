package com.hyuk.member.service;

import com.hyuk.member.dto.AddressRequest;
import com.hyuk.member.dto.AddressResponse;
import com.hyuk.member.dto.MemberResponse;

public interface MemberService {
    MemberResponse joinUser(String email, String name, String provider, String providerId);
    MemberResponse joinRider(String email, String name, String provider, String providerId);
    MemberResponse joinSeller(String email, String name, String provider, String providerId);
    MemberResponse createAddress(String memberId, AddressRequest request);
    MemberResponse updateAddress(String memberId, AddressRequest request);
    AddressResponse getAddress(String memberId);
    boolean hasAddress(String memberId);
    MemberResponse getMyInfo(String memberId);
    void createDummyRiders();
}
