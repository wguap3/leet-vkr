package com.stackr.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class StackrApplication {
    public static void main(String[] args) {
        // Docker service records disappear while a container is stopped. Do not retain
        // a fallback DNS answer after the service comes back.
        java.security.Security.setProperty("networkaddress.cache.ttl", "1");
        java.security.Security.setProperty("networkaddress.cache.negative.ttl", "1");
        java.security.Security.setProperty("networkaddress.cache.stale.ttl", "0");
        java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("UTC"));
        SpringApplication.run(StackrApplication.class, args);
    }
}
