package com.ornek.ehalisaha.ehalisahabackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class EHalisahaApplication {

    public static void main(String[] args) {
        SpringApplication.run(EHalisahaApplication.class, args);
    }

}
