package com.example.ridehailing.pricing;

import com.example.ridehailing.discount.DiscountStrategy;
import com.example.ridehailing.domain.CarType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class FareCalculator {

    private final PricingStrategy pricingStrategy;
    private final List<SurgeStrategy> surgeStrategies;
    private final ConditionsProvider conditionsProvider;
    private final BigDecimal minimumFare;
    private final BigDecimal surgeCap;

    public FareCalculator(PricingStrategy pricingStrategy, List<SurgeStrategy> surgeStrategies,
                           ConditionsProvider conditionsProvider, BigDecimal minimumFare, BigDecimal surgeCap) {
        this.pricingStrategy = Objects.requireNonNull(pricingStrategy, "pricingStrategy must not be null");
        this.surgeStrategies = List.copyOf(surgeStrategies);
        this.conditionsProvider = Objects.requireNonNull(conditionsProvider, "conditionsProvider must not be null");
        this.minimumFare = Objects.requireNonNull(minimumFare, "minimumFare must not be null");
        this.surgeCap = Objects.requireNonNull(surgeCap, "surgeCap must not be null");
    }

    public FareBreakdown quote(BigDecimal distanceKm, CarType carType, Optional<DiscountStrategy> discountStrategy) {
        if (distanceKm == null || distanceKm.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("distanceKm must be positive, was " + distanceKm);
        }
        if (carType == null) {
            throw new IllegalArgumentException("carType must not be null");
        }

        BigDecimal baseFare = pricingStrategy.baseFare(distanceKm);
        BigDecimal carMultiplier = carType.rateMultiplier();
        BigDecimal preMinimum = baseFare.multiply(carMultiplier);

        boolean minimumFareApplied = preMinimum.compareTo(minimumFare) < 0;
        BigDecimal subtotal = preMinimum.max(minimumFare);

        SurgeContext ctx = new SurgeContext(null, conditionsProvider.current());
        BigDecimal surgeMultiplier = BigDecimal.ONE;
        for (SurgeStrategy strategy : surgeStrategies) {
            surgeMultiplier = surgeMultiplier.multiply(strategy.multiplier(ctx).max(BigDecimal.ONE));
        }
        surgeMultiplier = surgeMultiplier.min(surgeCap);

        BigDecimal surged = subtotal.multiply(surgeMultiplier);
        BigDecimal discount = discountStrategy
                .map(strategy -> strategy.discountFor(surged))
                .orElse(BigDecimal.ZERO);
        BigDecimal total = surged.subtract(discount).max(BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_UP);

        return new FareBreakdown(distanceKm, carType, baseFare, carMultiplier, subtotal,
                minimumFareApplied, surgeMultiplier, discount, total);
    }
}
