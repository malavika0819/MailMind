package com.example.minder;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling; // ADD THIS IMPORT

@SpringBootApplication
@EnableScheduling
public class MinderApplication {

    public static void main(String[] args) {
        SpringApplication.run(MinderApplication.class, args);
    }
}
