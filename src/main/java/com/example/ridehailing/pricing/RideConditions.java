package com.example.ridehailing.pricing;

import java.util.Objects;

public record RideConditions(boolean raining, TrafficLevel trafficLevel) {

    public RideConditions {
        Objects.requireNonNull(trafficLevel, "trafficLevel must not be null");
    }
}
