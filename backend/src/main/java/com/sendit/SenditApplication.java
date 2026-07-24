package com.sendit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SenditApplication {

    public static void main(String[] args) {
        SpringApplication.run(SenditApplication.class, args);
    }
}
