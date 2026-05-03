package com.example.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

@Configuration
public class AppConfig {

    // Singleton scope is Spring's default — only one instance exists for the entire app lifecycle
    @Bean
    @Scope("singleton")
    public TaskStatisticsCache taskStatisticsCache() {
        return new TaskStatisticsCache();
    }
}
