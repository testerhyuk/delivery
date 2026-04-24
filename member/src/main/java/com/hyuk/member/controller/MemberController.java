package com.hyuk.member.controller;

import com.hyuk.member.dto.AddressRequest;
import com.hyuk.member.dto.AddressResponse;
import com.hyuk.member.dto.MemberResponse;
import com.hyuk.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user-service")
@RequiredArgsConstructor
public class MemberController {
    private final MemberService memberService;

    @GetMapping("/my")
    public ResponseEntity<MemberResponse> getMyInfo(@RequestHeader("userId") String userId) {
        MemberResponse member = memberService.getMyInfo(userId);

        return ResponseEntity.status(HttpStatus.OK).body(member);
    }

    @PostMapping("/addresses")
    public ResponseEntity<MemberResponse> addAddresses(@RequestHeader("userId") String userId, @RequestBody AddressRequest request) {
        MemberResponse member = memberService.createAddress(userId, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(member);
    }

    @PatchMapping("/addresses")
    public ResponseEntity<MemberResponse> updateAddresses(@RequestHeader("userId") String userId, @RequestBody AddressRequest request) {
        MemberResponse member = memberService.updateAddress(userId, request);

        return ResponseEntity.status(HttpStatus.OK).body(member);
    }

    @GetMapping("/addresses")
    public ResponseEntity<AddressResponse> getAddresses(@RequestHeader("userId") String userId) {
        AddressResponse address = memberService.getAddress(userId);

        return ResponseEntity.status(HttpStatus.OK).body(address);
    }
}
