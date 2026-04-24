package com.hyuk.member.secret;

import com.hyuk.member.dto.MemberResponse;
import com.hyuk.member.dto.OAuth2Attributes;
import com.hyuk.member.entity.MemberEntity;
import com.hyuk.member.repository.MemberRepository;
import com.hyuk.member.service.MemberService;
import com.hyuk.member.service.RefreshTokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final MemberService memberService;
    private final RefreshTokenService refreshTokenService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
        try {
            OAuth2AuthenticationToken authToken = (OAuth2AuthenticationToken) authentication;
            OAuth2User oAuth2User = authToken.getPrincipal();

            String memberType = (String) oAuth2User.getAttributes().get("member_type");

            if (memberType == null) memberType = "user";

            log.info("추출된 로그인 타입: {}", memberType);

            Map<String, Object> rawAttributes = oAuth2User.getAttributes();
            String registrationId = authToken.getAuthorizedClientRegistrationId();
            String userNameAttributeName = registrationId.equals("naver") ? "response" : "sub";
            OAuth2Attributes oAuth2Attributes = OAuth2Attributes.of(registrationId, userNameAttributeName, rawAttributes);

            MemberResponse member = null;

            if ("rider".equals(memberType)) {
                member = memberService.joinRider(oAuth2Attributes.getEmail(), oAuth2Attributes.getName(), oAuth2Attributes.getProvider(), oAuth2Attributes.getProviderId());
            } else if ("user".equals(memberType)) {
                member = memberService.joinUser(oAuth2Attributes.getEmail(), oAuth2Attributes.getName(), oAuth2Attributes.getProvider(), oAuth2Attributes.getProviderId());
            } else if ("seller".equals(memberType)) {
                member = memberService.joinSeller(oAuth2Attributes.getEmail(), oAuth2Attributes.getName(), oAuth2Attributes.getProvider(), oAuth2Attributes.getProviderId());
            }

            String accessToken = jwtTokenProvider.createToken(member.getMemberId(), member.getRoles());
            String refreshToken = jwtTokenProvider.createRefreshToken(member.getMemberId());

            refreshTokenService.saveRefreshToken(member.getMemberId(), refreshToken, jwtTokenProvider.getExpiration(refreshToken));

            String userId = member.getMemberId();

            response.addHeader("Set-Cookie", "accessToken=" + accessToken + "; HttpOnly; Path=/; Max-Age=3600; SameSite=Lax");
            ResponseCookie loginCookie = ResponseCookie.from("isLoggedIn", "true")
                    .path("/")
                    .maxAge(3600)
                    .domain("localhost")
                    .sameSite("Lax")
                    .build();

            response.addHeader("Set-Cookie", loginCookie.toString());

            boolean isNewUser = memberService.hasAddress(userId);

            if (isNewUser) {
                response.sendRedirect("http://localhost:5173/?need_address=true&user_id=" + userId);
            } else {
                response.sendRedirect("http://localhost:5173/");
            }

        } catch (Exception e) {
            log.error("인증 처리 실패: ", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
