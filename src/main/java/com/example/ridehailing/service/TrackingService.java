package com.example.ridehailing.service;

import com.example.ridehailing.api.exception.InvalidStateException;
import com.example.ridehailing.api.exception.NotFoundException;
import com.example.ridehailing.config.TrackingProperties;
import com.example.ridehailing.domain.Driver;
import com.example.ridehailing.domain.Location;
import com.example.ridehailing.domain.Ride;
import com.example.ridehailing.repository.DriverRepository;
import com.example.ridehailing.repository.RideRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class TrackingService {

    private final RideRepository rideRepository;
    private final DriverRepository driverRepository;
    private final double avgSpeedKmph;

    public TrackingService(RideRepository rideRepository, DriverRepository driverRepository,
                           TrackingProperties properties) {
        this.rideRepository = rideRepository;
        this.driverRepository = driverRepository;
        this.avgSpeedKmph = properties.avgSpeedKmph();
    }

    /**
     * Pull model: the rider's app polls this while the ride is live. Exhaustive over RideStatus
     * so a new status has to declare whether it is trackable rather than defaulting to one.
     */
    public RideTracking track(String rideId) {
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new NotFoundException("ride not found: " + rideId));

        return switch (ride.status()) {
            case SEARCHING -> new RideTracking(ride.id(), ride.status(), null, null, null, null, null);
            case DRIVER_ASSIGNED -> {
                DriverPing ping = pingOf(ride);
                Double toPickup = distanceKm(ping.location(), ride.pickup());
                yield new RideTracking(ride.id(), ride.status(), ping.location(), ping.at(),
                        toPickup, null, etaMinutes(toPickup));
            }
            case IN_PROGRESS -> {
                DriverPing ping = pingOf(ride);
                Double toDrop = distanceKm(ping.location(), ride.drop());
                yield new RideTracking(ride.id(), ride.status(), ping.location(), ping.at(),
                        null, toDrop, etaMinutes(toDrop));
            }
            case QUOTED, COMPLETED, CANCELLED_BY_USER, NO_DRIVER_FOUND ->
                    throw new InvalidStateException(
                            "ride " + rideId + " cannot be tracked while it is " + ride.status());
        };
    }

    private DriverPing pingOf(Ride ride) {
        Driver driver = driverRepository.findById(ride.driverId())
                .orElseThrow(() -> new NotFoundException("driver not found: " + ride.driverId()));
        // Read both halves of the ping together; they are separate volatile fields.
        return new DriverPing(driver.currentLocation(), driver.lastLocationAt());
    }

    private Double distanceKm(Location driverLocation, Location target) {
        return driverLocation == null ? null : driverLocation.distanceKmTo(target);
    }

    /** Whole minutes: an ETA quoted to more precision than that would be false confidence. */
    private Long etaMinutes(Double distanceKm) {
        return distanceKm == null ? null : Math.round(distanceKm / avgSpeedKmph * 60);
    }

    private record DriverPing(Location location, Instant at) {
    }
}
