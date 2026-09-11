package com.example.ridehailing.pricing;

import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Process-wide holder of the current rain/traffic conditions used to price surge.
 * Mutated by the admin endpoint added in a later iteration; defaults to no surge.
 */
@Component
public class ConditionsProvider {

    private final AtomicReference<RideConditions> conditions =
            new AtomicReference<>(new RideConditions(false, TrafficLevel.LOW));

    public RideConditions current() {
        return conditions.get();
    }

    public void update(RideConditions newConditions) {
        conditions.set(Objects.requireNonNull(newConditions, "newConditions must not be null"));
    }
}
