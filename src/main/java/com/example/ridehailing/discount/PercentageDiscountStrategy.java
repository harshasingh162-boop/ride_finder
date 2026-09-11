package com.example.ridehailing.discount;

import java.math.BigDecimal;
import java.util.Objects;

public class PercentageDiscountStrategy implements DiscountStrategy {

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private final BigDecimal percent;
    private final BigDecimal maxDiscount;

    public PercentageDiscountStrategy(BigDecimal percent, BigDecimal maxDiscount) {
        this.percent = Objects.requireNonNull(percent, "percent must not be null");
        this.maxDiscount = Objects.requireNonNull(maxDiscount, "maxDiscount must not be null");
    }

    @Override
    public BigDecimal discountFor(BigDecimal fare) {
        // Exact division is safe here: dividing by 100 always terminates, so the only
        // rounding in the whole pipeline stays the final HALF_UP in FareCalculator.
        return fare.multiply(percent).divide(HUNDRED).min(maxDiscount);
    }
}
