package com.hyuk.pay.config;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Base64;
import java.util.concurrent.TimeUnit;

@Configuration
public class TossFeignConfig {
    @Value("${TOSS_SECRET_KEY}")
    private String widgetSecretKey;

    @Bean
    public RequestInterceptor requestInterceptor() {
        return restTemplate -> {
            String auth = "Basic " + Base64.getEncoder().encodeToString((widgetSecretKey + ":").getBytes());
            restTemplate.header("Authorization", auth);
        };
    }

    @Bean
    public feign.Request.Options options() {
        return new feign.Request.Options(
                5, TimeUnit.SECONDS,
                10, TimeUnit.SECONDS,
                true
        );
    }
}
