package com.hyuk.member.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user-service")
@RequiredArgsConstructor
public class MemberController {

    @GetMapping("/my")
    public ResponseEntity<String> getMyInfo(@RequestHeader("userId") String userId) {
        return ResponseEntity.ok("로그인 유저 ID: " + userId);
    }
}
