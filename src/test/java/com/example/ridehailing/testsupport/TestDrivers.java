package com.example.ridehailing.testsupport;

import com.example.ridehailing.domain.Car;
import com.example.ridehailing.domain.CarType;
import com.example.ridehailing.domain.Driver;
import com.example.ridehailing.domain.DriverStatus;
import com.example.ridehailing.domain.Location;

import java.time.Instant;

public final class TestDrivers {

    private TestDrivers() {
    }

    /** A driver who has pinged their location and gone online: the normal matchable state. */
    public static Driver available(String id, CarType carType, double rating, Location at, Instant pingedAt) {
        Driver driver = offline(id, carType, rating, at, pingedAt);
        driver.transition(DriverStatus.OFFLINE, DriverStatus.AVAILABLE);
        return driver;
    }

    public static Driver offline(String id, CarType carType, double rating, Location at, Instant pingedAt) {
        Driver driver = new Driver(id, "Driver " + id, "+91-" + id,
                new Car("PLATE-" + id, "Model " + id, carType), rating);
        driver.updateLocation(at, pingedAt);
        return driver;
    }

    public static Driver onTrip(String id, CarType carType, double rating, Location at, Instant pingedAt) {
        Driver driver = available(id, carType, rating, at, pingedAt);
        driver.transition(DriverStatus.AVAILABLE, DriverStatus.ON_TRIP);
        return driver;
    }
}
