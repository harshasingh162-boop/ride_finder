package com.example.ridehailing.api;

import com.example.ridehailing.domain.CarType;
import com.example.ridehailing.domain.Ride;
import com.example.ridehailing.domain.RideStatus;
import com.example.ridehailing.pricing.FareBreakdown;

import java.time.Instant;

public record RideResponse(
        String id,
        String userId,
        RideStatus status,
        LocationResponse pickup,
        LocationResponse drop,
        CarType requestedCarType,
        CarType assignedCarType,
        boolean upgraded,
        String driverId,
        FareBreakdown quotedFare,
        Instant requestedAt,
        Instant assignedAt
) {

    public static RideResponse from(Ride ride) {
        return new RideResponse(ride.id(), ride.userId(), ride.status(),
                LocationResponse.from(ride.pickup()), LocationResponse.from(ride.drop()),
                ride.requestedCarType(), ride.assignedCarType(), ride.upgraded(), ride.driverId(),
                ride.quotedFare(), ride.requestedAt(), ride.assignedAt());
    }
}
