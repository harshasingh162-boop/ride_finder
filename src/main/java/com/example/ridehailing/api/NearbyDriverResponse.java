package com.example.ridehailing.api;

import com.example.ridehailing.domain.CarType;
import com.example.ridehailing.domain.Driver;
import com.example.ridehailing.domain.Location;

public record NearbyDriverResponse(String id, double distanceKm, CarType carType, double rating) {

    public static NearbyDriverResponse from(Driver driver, Location pickup) {
        return new NearbyDriverResponse(driver.id(), pickup.distanceKmTo(driver.currentLocation()),
                driver.car().type(), driver.rating());
    }
}
