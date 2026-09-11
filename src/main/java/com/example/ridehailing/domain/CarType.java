package com.example.ridehailing.domain;

import java.math.BigDecimal;
import java.util.Optional;

public enum CarType {
    HATCHBACK(new BigDecimal("1.0")),
    SEDAN(new BigDecimal("1.5"));

    private final BigDecimal rateMultiplier;

    CarType(BigDecimal rateMultiplier) {
        this.rateMultiplier = rateMultiplier;
    }

    public BigDecimal rateMultiplier() {
        return rateMultiplier;
    }

    /**
     * The car type a rider is bumped to, free of charge, when no driver of their
     * requested type is available. A switch expression with no default so that
     * adding a new CarType forces a decision here at compile time.
     */
    public Optional<CarType> freeUpgrade() {
        return switch (this) {
            case HATCHBACK -> Optional.of(SEDAN);
            case SEDAN -> Optional.empty();
        };
    }
}
