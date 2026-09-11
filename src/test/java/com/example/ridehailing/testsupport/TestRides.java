package com.example.ridehailing.testsupport;

import com.example.ridehailing.domain.CarType;
import com.example.ridehailing.domain.Location;
import com.example.ridehailing.domain.Ride;
import com.example.ridehailing.pricing.FareBreakdown;

import java.math.BigDecimal;
import java.time.Instant;

public final class TestRides {

    /** A stand-in quote for tests that care about ride state rather than pricing. */
    public static final FareBreakdown ANY_FARE = new FareBreakdown(
            new BigDecimal("10"), CarType.HATCHBACK, new BigDecimal("69"), new BigDecimal("1.0"),
            new BigDecimal("69"), false, BigDecimal.ONE, BigDecimal.ZERO, new BigDecimal("69.00"));

    private TestRides() {
    }

    public static Ride ride(String id, String userId, Location pickup, Location drop,
                            CarType carType, Instant requestedAt) {
        return new Ride(id, userId, pickup, drop, carType, ANY_FARE, requestedAt);
    }
}
