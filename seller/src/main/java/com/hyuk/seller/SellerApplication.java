package com.hyuk.seller;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.hyuk")
public class SellerApplication {
    public static void main(String[] args) {
        SpringApplication.run(SellerApplication.class, args);
    }
}
