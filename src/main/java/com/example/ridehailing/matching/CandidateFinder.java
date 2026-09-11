package com.example.ridehailing.matching;

import com.example.ridehailing.domain.CarType;
import com.example.ridehailing.domain.Driver;
import com.example.ridehailing.domain.Location;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Component
public class CandidateFinder {

    private final ProximitySearch proximitySearch;
    private final DriverMatchingStrategy matchingStrategy;

    public CandidateFinder(ProximitySearch proximitySearch, DriverMatchingStrategy matchingStrategy) {
        this.proximitySearch = proximitySearch;
        this.matchingStrategy = matchingStrategy;
    }

    /**
     * Looks for the requested car type first, then walks the free-upgrade chain. The walk
     * only ever moves up, so a rider never silently gets a cheaper car than they asked for.
     */
    public MatchResult find(Location pickup, CarType requestedCarType) {
        CarType carType = requestedCarType;
        while (true) {
            List<Driver> found = proximitySearch.findNearby(pickup, Set.of(carType));
            if (!found.isEmpty()) {
                return new MatchResult(matchingStrategy.rank(found, pickup), carType, carType != requestedCarType);
            }
            Optional<CarType> upgrade = carType.freeUpgrade();
            if (upgrade.isEmpty()) {
                return new MatchResult(List.of(), requestedCarType, false);
            }
            carType = upgrade.get();
        }
    }
}
