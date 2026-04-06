package com.hyuk.member.secret;

import com.hyuk.member.dto.OAuth2Attributes;
import com.hyuk.member.entity.MemberEntity;
import com.hyuk.member.repository.MemberRepository;
import com.hyuk.member.service.MemberService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final MemberService memberService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
        try {
            OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

            String registrationId = ((OAuth2AuthenticationToken) authentication).getAuthorizedClientRegistrationId();
            String userNameAttributeName = registrationId.equals("naver") ? "response" : "sub";
            OAuth2Attributes attributes = OAuth2Attributes.of(registrationId, userNameAttributeName, oAuth2User.getAttributes());

            var member = memberService.joinUser(
                    attributes.getEmail(),
                    attributes.getName(),
                    attributes.getProvider(),
                    attributes.getProviderId()
            );

            String token = jwtTokenProvider.createToken(member.getId(), member.getRoles());

            response.addHeader("Set-Cookie","accessToken=" + token + "; HttpOnly; Path=/; Max-Age=3600");
            response.sendRedirect("http://localhost:8000/auth/success");

        } catch (Exception e) {
            log.error(e.getMessage());
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "인증 후 처리 실패");
        }
    }
}
