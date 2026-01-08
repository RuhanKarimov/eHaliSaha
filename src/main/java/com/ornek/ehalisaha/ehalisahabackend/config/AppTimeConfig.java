package com.ornek.ehalisaha.ehalisahabackend.config;

import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

import java.util.TimeZone;

/**
 * JVM default timezone = Europe/Istanbul
 * Not: DB tarafı için yine de UTC saklama tavsiye edilir (hibernate.jdbc.time_zone=UTC).
 */
@Configuration
public class AppTimeConfig {

    @PostConstruct
    public void init() {
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Istanbul"));
    }
}
