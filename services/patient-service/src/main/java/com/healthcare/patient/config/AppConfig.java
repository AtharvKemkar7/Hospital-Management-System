package com.healthcare.patient.config;

import com.healthcare.patient.security.JwtProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneOffset;

@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class AppConfig {

    @Bean
    public Clock clock() {
        return Clock.system(ZoneOffset.UTC);
    }
}
