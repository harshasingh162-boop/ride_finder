package com.example.ridehailing.pricing;

import java.math.BigDecimal;

public interface PricingStrategy {

    BigDecimal baseFare(BigDecimal distanceKm);
}
