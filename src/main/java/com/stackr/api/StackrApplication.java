package com.stackr.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class StackrApplication {
    public static void main(String[] args) {
        SpringApplication.run(StackrApplication.class, args);
    }
}
