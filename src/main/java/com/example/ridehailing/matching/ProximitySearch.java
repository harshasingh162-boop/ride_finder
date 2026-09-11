package com.example.ridehailing.matching;

import com.example.ridehailing.domain.CarType;
import com.example.ridehailing.domain.Driver;
import com.example.ridehailing.domain.Location;

import java.util.List;
import java.util.Set;

/**
 * Finds drivers who could plausibly take a ride from {@code pickup} right now.
 * The result is unordered; ranking is the job of a {@link DriverMatchingStrategy}.
 */
public interface ProximitySearch {

    List<Driver> findNearby(Location pickup, Set<CarType> carTypes);
}
