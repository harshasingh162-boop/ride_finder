package com.example.ridehailing.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;
import java.util.List;

@ConfigurationProperties(prefix = "ride-hailing.pricing")
public record PricingProperties(BigDecimal minimumFare, BigDecimal surgeCap, List<Tier> tiers) {

    public record Tier(BigDecimal upToKm, BigDecimal rate) {
    }
}
