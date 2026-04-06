package com.hyuk.member.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RedisTemplate<String, Object> redisTemplate;

    public void saveRefreshToken(Long memberId, String refreshToken, Long expirationTime) {
        redisTemplate.opsForValue().set(
                "RT:" + memberId,
                refreshToken,
                expirationTime,
                TimeUnit.MILLISECONDS
        );
    }

    public String getRefreshToken(Long memberId) {
        return (String) redisTemplate.opsForValue().get("RT:" + memberId);
    }

    public void deleteRefreshToken(Long memberId) {
        redisTemplate.delete("RT:" + memberId);
    }
}
