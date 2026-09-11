package com.example.ridehailing.api;

import com.example.ridehailing.domain.Location;

public record LocationResponse(double lat, double lng) {

    public static LocationResponse from(Location location) {
        return new LocationResponse(location.lat(), location.lng());
    }
}
