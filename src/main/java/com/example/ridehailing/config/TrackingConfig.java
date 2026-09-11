package com.example.ridehailing.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(TrackingProperties.class)
public class TrackingConfig {
}
