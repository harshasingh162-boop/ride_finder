package com.example.ridehailing.pricing;

import java.math.BigDecimal;

/**
 * One band of a tiered fare schedule: charged at {@code ratePerKm} for the portion
 * of the trip up to {@code upToKm}. {@code upToKm} is null for the open-ended final tier.
 */
public record PriceTier(BigDecimal upToKm, BigDecimal ratePerKm) {
}
