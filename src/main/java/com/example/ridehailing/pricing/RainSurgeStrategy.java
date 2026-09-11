package com.example.ridehailing.pricing;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class RainSurgeStrategy implements SurgeStrategy {

    private static final BigDecimal RAIN_MULTIPLIER = new BigDecimal("1.2");

    @Override
    public BigDecimal multiplier(SurgeContext ctx) {
        return ctx.conditions().raining() ? RAIN_MULTIPLIER : BigDecimal.ONE;
    }
}
