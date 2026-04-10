package com.hyuk.rider;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.hyuk")
public class RiderApplication {
    public static void main(String[] args) {
        SpringApplication.run(RiderApplication.class, args);
    }
}
