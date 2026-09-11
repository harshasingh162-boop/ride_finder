package com.example.ridehailing.matching;

import com.example.ridehailing.domain.CarType;
import com.example.ridehailing.domain.Driver;
import com.example.ridehailing.domain.DriverStatus;
import com.example.ridehailing.domain.Location;
import com.example.ridehailing.repository.DriverRepository;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;

/**
 * Scans every driver on each call. That is deliberate at this scale: it keeps the
 * matching rules in one readable place, and the {@link ProximitySearch} interface is
 * the seam where a geohash or grid index would slot in once the fleet outgrows it.
 */
public class LinearScanProximitySearch implements ProximitySearch {

    private final DriverRepository driverRepository;
    private final Clock clock;
    private final double radiusKm;
    private final Duration locationStaleness;

    public LinearScanProximitySearch(DriverRepository driverRepository, Clock clock,
                                     double radiusKm, Duration locationStaleness) {
        this.driverRepository = driverRepository;
        this.clock = clock;
        this.radiusKm = radiusKm;
        this.locationStaleness = locationStaleness;
    }

    @Override
    public List<Driver> findNearby(Location pickup, Set<CarType> carTypes) {
        Instant oldestAcceptablePing = clock.instant().minus(locationStaleness);
        return driverRepository.findAll().stream()
                .filter(driver -> driver.status() == DriverStatus.AVAILABLE)
                .filter(driver -> carTypes.contains(driver.car().type()))
                .filter(driver -> isWithinReach(driver, pickup, oldestAcceptablePing))
                .toList();
    }

    private boolean isWithinReach(Driver driver, Location pickup, Instant oldestAcceptablePing) {
        // Read both halves of the last ping once. They are separate volatile fields, so a
        // concurrent push could otherwise be seen half-applied.
        Location driverLocation = driver.currentLocation();
        Instant pingedAt = driver.lastLocationAt();
        if (driverLocation == null || pingedAt == null || pingedAt.isBefore(oldestAcceptablePing)) {
            return false;
        }
        // Inclusive bound. Note that landing exactly on the radius is not reachable in
        // practice: haversine over double coordinates carries ~1e-13 km of error, which is
        // far below any distance that matters here.
        return pickup.distanceKmTo(driverLocation) <= radiusKm;
    }
}
