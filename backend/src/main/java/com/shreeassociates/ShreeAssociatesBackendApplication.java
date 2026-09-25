package com.shreeassociates;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class ShreeAssociatesBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(ShreeAssociatesBackendApplication.class, args);
    }
}
