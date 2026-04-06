package com.hyuk.member.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {
    @GetMapping("/success")
    public String loginSuccess(@AuthenticationPrincipal OAuth2User principal) {
        // 여기서 구글이 준 정보를 확인하거나, 생성된 JWT를 화면에 뿌려버리세요.
        return "로그인 성공! 유저 정보: " + principal.getAttributes();
    }
}
