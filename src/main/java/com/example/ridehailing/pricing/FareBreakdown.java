package com.example.ridehailing.pricing;

import com.example.ridehailing.domain.CarType;

import java.math.BigDecimal;

public record FareBreakdown(
        BigDecimal distanceKm,
        CarType carType,
        BigDecimal baseFare,
        BigDecimal carMultiplier,
        BigDecimal subtotal,
        boolean minimumFareApplied,
        BigDecimal surgeMultiplier,
        BigDecimal discount,
        BigDecimal total
) {
}
