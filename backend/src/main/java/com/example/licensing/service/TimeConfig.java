package com.example.licensing.service;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class TimeConfig {

    /** 统一使用 UTC 时钟 */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
