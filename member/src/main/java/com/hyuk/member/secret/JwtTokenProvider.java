package com.hyuk.member.secret;

import com.hyuk.member.entity.enums.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Component
public class JwtTokenProvider {

    @Value("${token.secret}")
    private String secretKeyString;

    private SecretKey key;

    @Value("${token.expiration-time}")
    private Long tokenValidityInMilliseconds;

    @Value("${token.refresh-expiration-time}")
    private Long refreshValidityInMilliseconds;

    @PostConstruct
    protected void init() {
        byte[] keyBytes = secretKeyString.getBytes(StandardCharsets.UTF_8);
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    public String createToken(String memberId, Set<Role> roles) {
        return Jwts.builder()
                .subject(memberId)
                .claim("roles", roles.stream().map(Role::getKey).collect(Collectors.toList()))
                .issuedAt(new Date())
                .expiration(new Date(new Date().getTime() + tokenValidityInMilliseconds))
                .signWith(key)
                .compact();
    }

    public String createRefreshToken(String memberId) {
        return Jwts.builder()
                .subject(memberId)
                .issuedAt(new Date())
                .expiration(new Date(new Date().getTime() + refreshValidityInMilliseconds))
                .signWith(key)
                .compact();
    }

    public Long getExpiration(String token) {
        Date expiration = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getExpiration();
        return expiration.getTime() - new Date().getTime();
    }

    public Long getMemberId(String token) {
        return Long.parseLong(
                Jwts.parser()
                        .verifyWith(key)
                        .build()
                        .parseSignedClaims(token)
                        .getPayload()
                        .getSubject()
        );
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public List<String> getRoles(String token) {
        Object roles = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get("roles");

        if (roles instanceof List<?>) {
            return ((List<?>) roles).stream()
                    .map(String::valueOf)
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}