package com.hyuk.gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Slf4j
public class AuthenticationFilter extends AbstractGatewayFilterFactory<AuthenticationFilter.Config> {

    @Value("${TOKEN_SECRET}")
    private String secret;

    public AuthenticationFilter() {
        super(Config.class);
    }

    public static class Config {}

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String path = exchange.getRequest().getURI().getPath();

            if (path.contains("/v3/api-docs") ||
                    path.contains("/login") ||
                    path.contains("/oauth2") ||
                    path.contains("/auth/success")
            ) {
                log.info("Whitelist path detected, skipping filter: {}", path);
                return chain.filter(exchange);
            }

            if (secret == null) {
                log.error("Config Missing: TOKEN_SECRET is null");
                return onError(exchange, "Internal Server Error", HttpStatus.INTERNAL_SERVER_ERROR);
            }

            ServerHttpRequest request = exchange.getRequest();

            if (!request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
                return onError(exchange, "No Authorization header", HttpStatus.UNAUTHORIZED);
            }

            String authorizationHeader = request.getHeaders().get(HttpHeaders.AUTHORIZATION).get(0);
            String jwt = authorizationHeader.replace("Bearer ", "");

            Claims claims = getClaimsIfValid(jwt);
            if (claims == null) {
                return onError(exchange, "Invalid JWT token", HttpStatus.UNAUTHORIZED);
            }

            String userId = claims.getSubject();
            Object rolesObj = claims.get("roles");
            String rolesStr = "";
            if (rolesObj instanceof List<?>) {
                rolesStr = ((List<?>) rolesObj).stream()
                        .map(String::valueOf)
                        .collect(Collectors.joining(","));
            }

            log.info("인증 통과 - ID: {}, Roles: {}", userId, rolesStr);

            ServerHttpRequest newRequest = request.mutate()
                    .header("userId", userId)
                    .header("userRoles", rolesStr)
                    .build();

            return chain.filter(exchange.mutate().request(newRequest).build());
        };
    }

    private Mono<Void> onError(ServerWebExchange exchange, String err, HttpStatus status) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        log.error("인증 실패: {} (Path: {})", err, exchange.getRequest().getURI().getPath());

        byte[] bytes = "The requested token is invalid or missing".getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Flux.just(buffer));
    }

    private Claims getClaimsIfValid(String jwt) {
        try {
            byte[] secretKeyBytes = secret.getBytes(StandardCharsets.UTF_8);
            SecretKey signingKey = Keys.hmacShaKeyFor(secretKeyBytes);
            JwtParser parser = Jwts.parser().verifyWith(signingKey).build();
            return parser.parseSignedClaims(jwt).getPayload();
        } catch (Exception e) {
            log.error("JWT 검증 에러: {}", e.getMessage());
            return null;
        }
    }
}
