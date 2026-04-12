package com.hyuk.member.controller;

import com.hyuk.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/dummy")
public class MemberDummyController {
    private final MemberService memberService;

    @PostMapping("/riders")
    public ResponseEntity<Void> createDummyRiders() {
        memberService.createDummyRiders();

        return ResponseEntity.ok().build();
    }
}
