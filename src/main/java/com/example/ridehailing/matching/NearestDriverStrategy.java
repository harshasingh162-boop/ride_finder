package com.example.ridehailing.matching;

import com.example.ridehailing.domain.Driver;
import com.example.ridehailing.domain.Location;

import java.util.Comparator;
import java.util.List;

public class NearestDriverStrategy implements DriverMatchingStrategy {

    @Override
    public List<Driver> rank(List<Driver> candidates, Location pickup) {
        return candidates.stream()
                .sorted(Comparator.comparingDouble(driver -> pickup.distanceKmTo(driver.currentLocation())))
                .toList();
    }
}
