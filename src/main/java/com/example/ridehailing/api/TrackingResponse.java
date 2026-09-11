package com.example.ridehailing.api;

import com.example.ridehailing.domain.RideStatus;
import com.example.ridehailing.service.RideTracking;

import java.time.Instant;

public record TrackingResponse(
        String rideId,
        RideStatus status,
        LocationResponse driverLocation,
        Instant locationUpdatedAt,
        Double distanceToPickupKm,
        Double distanceToDropKm,
        Long etaMinutes
) {

    public static TrackingResponse from(RideTracking tracking) {
        return new TrackingResponse(
                tracking.rideId(),
                tracking.status(),
                tracking.driverLocation() == null ? null : LocationResponse.from(tracking.driverLocation()),
                tracking.locationUpdatedAt(),
                tracking.distanceToPickupKm(),
                tracking.distanceToDropKm(),
                tracking.etaMinutes());
    }
}
