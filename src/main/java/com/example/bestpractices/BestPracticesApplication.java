package com.example.bestpractices;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;

// @EnableCaching and @EnableAsync are placed on the main class to keep
// configuration centralised; the actual beans live in their own @Configuration classes.
@SpringBootApplication
@EnableCaching
@EnableAsync
public class BestPracticesApplication {

    public static void main(String[] args) {
        SpringApplication.run(BestPracticesApplication.class, args);
    }
}
