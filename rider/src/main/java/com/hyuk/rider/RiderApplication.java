package com.hyuk.rider;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication(scanBasePackages = "com.hyuk")
@EnableFeignClients
public class RiderApplication {
    public static void main(String[] args) {
        SpringApplication.run(RiderApplication.class, args);
    }
}
