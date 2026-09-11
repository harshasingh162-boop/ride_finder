package com.example.ridehailing.pricing;

import java.math.BigDecimal;
import java.util.List;

public class TieredPricingStrategy implements PricingStrategy {

    private final List<PriceTier> tiers;

    public TieredPricingStrategy(List<PriceTier> tiers) {
        if (tiers == null || tiers.isEmpty()) {
            throw new IllegalArgumentException("tiers must not be empty");
        }
        BigDecimal previousBoundary = BigDecimal.ZERO;
        for (int i = 0; i < tiers.size(); i++) {
            PriceTier tier = tiers.get(i);
            boolean isLast = i == tiers.size() - 1;
            if (tier.ratePerKm() == null || tier.ratePerKm().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("tier rate must be positive: " + tier);
            }
            if (isLast) {
                if (tier.upToKm() != null) {
                    throw new IllegalArgumentException("the last tier must be open-ended (upToKm == null)");
                }
            } else {
                if (tier.upToKm() == null) {
                    throw new IllegalArgumentException("only the last tier may be open-ended: " + tier);
                }
                if (tier.upToKm().compareTo(previousBoundary) <= 0) {
                    throw new IllegalArgumentException("tiers must be sorted and non-overlapping: " + tier);
                }
                previousBoundary = tier.upToKm();
            }
        }
        this.tiers = List.copyOf(tiers);
    }

    @Override
    public BigDecimal baseFare(BigDecimal distanceKm) {
        BigDecimal previousBoundary = BigDecimal.ZERO;
        BigDecimal total = BigDecimal.ZERO;
        for (PriceTier tier : tiers) {
            BigDecimal tierUpper = tier.upToKm() == null ? distanceKm : tier.upToKm().min(distanceKm);
            BigDecimal span = tierUpper.subtract(previousBoundary);
            if (span.compareTo(BigDecimal.ZERO) > 0) {
                total = total.add(span.multiply(tier.ratePerKm()));
            }
            if (tier.upToKm() == null || tier.upToKm().compareTo(distanceKm) >= 0) {
                break;
            }
            previousBoundary = tier.upToKm();
        }
        return total;
    }
}
