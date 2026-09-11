package com.example.ridehailing.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "ride-hailing.matching")
public record MatchingProperties(double radiusKm, Duration locationStaleness, StrategyName strategy) {

    /**
     * Typed as an enum rather than a String so Spring rejects an unknown value while
     * binding, and so the selection switch in MatchingConfig stays exhaustive.
     */
    public enum StrategyName {
        NEAREST,
        HIGHEST_RATED
    }
}
