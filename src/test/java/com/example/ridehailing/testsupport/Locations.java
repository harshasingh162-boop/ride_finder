package com.example.ridehailing.testsupport;

import com.example.ridehailing.domain.Location;

public final class Locations {

    public static final Location BENGALURU = new Location(12.9716, 77.5946);

    /**
     * Derived from the production haversine rather than hard-coded, so the helper cannot
     * drift from the implementation it is used to test. Along a meridian haversine reduces
     * to R * dLat, so a north-south offset is exact at any latitude.
     */
    private static final double KM_PER_DEGREE_LATITUDE =
            new Location(0, 0).distanceKmTo(new Location(1, 0));

    private Locations() {
    }

    /** A point exactly {@code km} north of {@code origin}. */
    public static Location offsetNorth(Location origin, double km) {
        return new Location(origin.lat() + km / KM_PER_DEGREE_LATITUDE, origin.lng());
    }
}
