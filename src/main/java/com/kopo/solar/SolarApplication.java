package com.kopo.solar;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class SolarApplication {

    public static void main(String[] args) {
        SpringApplication.run(SolarApplication.class, args);
    }
}
