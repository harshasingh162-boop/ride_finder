package com.example.ridehailing.pricing;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class TrafficSurgeStrategy implements SurgeStrategy {

    @Override
    public BigDecimal multiplier(SurgeContext ctx) {
        return switch (ctx.conditions().trafficLevel()) {
            case LOW -> BigDecimal.ONE;
            case MEDIUM -> new BigDecimal("1.2");
            case HIGH -> new BigDecimal("1.5");
        };
    }
}
