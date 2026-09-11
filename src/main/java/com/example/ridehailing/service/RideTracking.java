package com.example.ridehailing.service;

import com.example.ridehailing.domain.Location;
import com.example.ridehailing.domain.RideStatus;

import java.time.Instant;

/**
 * A point-in-time view of where the driver is. Every field after {@code status} is null while
 * the ride is still SEARCHING, because no driver has been assigned to report on yet.
 *
 * @param distanceToPickupKm set while heading to the rider, null once the trip is under way
 * @param distanceToDropKm   set once the trip is under way, null before that
 */
public record RideTracking(
        String rideId,
        RideStatus status,
        Location driverLocation,
        Instant locationUpdatedAt,
        Double distanceToPickupKm,
        Double distanceToDropKm,
        Long etaMinutes
) {
}
