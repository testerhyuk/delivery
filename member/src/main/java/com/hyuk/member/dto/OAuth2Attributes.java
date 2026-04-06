package com.hyuk.member.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
@Builder
public class OAuth2Attributes {
    private Map<String, Object> attributes;
    private String nameAttributeKey;
    private String name;
    private String email;
    private String provider;
    private String providerId;

    public static OAuth2Attributes of(String registrationId, String userNameAttributeName, Map<String, Object> attributes) {
        if ("naver".equals(registrationId)) {
            return ofNaver("id", attributes);
        }
        return ofGoogle(userNameAttributeName, attributes);
    }

    private static OAuth2Attributes ofGoogle(String userNameAttributeName, Map<String, Object> attributes) {
        return OAuth2Attributes.builder()
                .name((String) attributes.get("name"))
                .email((String) attributes.get("email"))
                .provider("google")
                .providerId((String) attributes.get(userNameAttributeName))
                .attributes(attributes)
                .nameAttributeKey(userNameAttributeName)
                .build();
    }

    private static OAuth2Attributes ofNaver(String userNameAttributeName, Map<String, Object> attributes) {
        Object responseObj = attributes.get("response");

        if (responseObj instanceof Map<?, ?> response) {
            return OAuth2Attributes.builder()
                    .name(String.valueOf(response.get("name")))
                    .email(String.valueOf(response.get("email")))
                    .provider("naver")
                    .providerId(String.valueOf(response.get("id")))
                    .attributes(attributes) // 원본 유지
                    .nameAttributeKey(userNameAttributeName)
                    .build();
        }

        throw new IllegalArgumentException("Naver 응답 형식이 올바르지 않습니다.");
    }
}
