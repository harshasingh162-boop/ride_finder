package com.example.ridehailing.matching;

import com.example.ridehailing.domain.Driver;
import com.example.ridehailing.domain.Location;

import java.util.Comparator;
import java.util.List;

public class HighestRatedDriverStrategy implements DriverMatchingStrategy {

    @Override
    public List<Driver> rank(List<Driver> candidates, Location pickup) {
        Comparator<Driver> byRatingDescending = Comparator.comparingDouble(Driver::rating).reversed();
        Comparator<Driver> byDistance =
                Comparator.comparingDouble(driver -> pickup.distanceKmTo(driver.currentLocation()));
        return candidates.stream()
                .sorted(byRatingDescending.thenComparing(byDistance))
                .toList();
    }
}
