package com.hyuk.member.controller;

import com.hyuk.member.dto.AddressRequest;
import com.hyuk.member.dto.MemberResponse;
import com.hyuk.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user-service")
@RequiredArgsConstructor
public class MemberController {
    private final MemberService memberService;

    @GetMapping("/my")
    public ResponseEntity<String> getMyInfo(@RequestHeader("userId") Long userId) {
        return ResponseEntity.ok("로그인 유저 ID: " + userId);
    }

    @PostMapping("/addresses")
    public ResponseEntity<MemberResponse> addAddresses(@RequestHeader("userId") String userId, @RequestBody AddressRequest request) {
        MemberResponse member = memberService.createAddress(Long.valueOf(userId), request.getAddress(), request.getDetailAddress());

        return ResponseEntity.status(HttpStatus.CREATED).body(member);
    }

    @PatchMapping("/addresses")
    public ResponseEntity<MemberResponse> updateAddresses(@RequestHeader("userId") String userId, @RequestBody AddressRequest request) {
        MemberResponse member = memberService.updateAddress(Long.valueOf(userId), request.getAddress(), request.getDetailAddress());

        return ResponseEntity.status(HttpStatus.OK).body(member);
    }
}
