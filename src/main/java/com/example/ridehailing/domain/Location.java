package com.example.ridehailing.domain;

public record Location(double lat, double lng) {

    private static final double EARTH_RADIUS_KM = 6371.0;

    public Location {
        if (lat < -90 || lat > 90) {
            throw new IllegalArgumentException("lat must be between -90 and 90, was " + lat);
        }
        if (lng < -180 || lng > 180) {
            throw new IllegalArgumentException("lng must be between -180 and 180, was " + lng);
        }
    }

    public double distanceKmTo(Location other) {
        double dLat = Math.toRadians(other.lat - lat);
        double dLng = Math.toRadians(other.lng - lng);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat)) * Math.cos(Math.toRadians(other.lat))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }
}
