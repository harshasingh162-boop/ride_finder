package com.example.ridehailing.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ride-hailing.tracking")
public record TrackingProperties(double avgSpeedKmph) {
}
