package com.example.ridehailing.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public class Driver {

    public static final double DEFAULT_RATING = 5.0;

    private final String id;
    private final String name;
    private final String phone;
    private final Car car;
    private final AtomicReference<DriverStatus> status;

    private volatile double rating;
    private volatile Location currentLocation;
    private volatile Instant lastLocationAt;

    public Driver(String id, String name, String phone, Car car) {
        this(id, name, phone, car, DEFAULT_RATING);
    }

    public Driver(String id, String name, String phone, Car car, double rating) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.phone = Objects.requireNonNull(phone, "phone must not be null");
        this.car = Objects.requireNonNull(car, "car must not be null");
        this.status = new AtomicReference<>(DriverStatus.OFFLINE);
        this.rating = rating;
    }

    public boolean transition(DriverStatus from, DriverStatus to) {
        return status.compareAndSet(from, to);
    }

    public void updateLocation(Location location, Instant at) {
        this.currentLocation = Objects.requireNonNull(location, "location must not be null");
        this.lastLocationAt = Objects.requireNonNull(at, "at must not be null");
    }

    public String id() {
        return id;
    }

    public String name() {
        return name;
    }

    public String phone() {
        return phone;
    }

    public Car car() {
        return car;
    }

    public DriverStatus status() {
        return status.get();
    }

    public double rating() {
        return rating;
    }

    public Location currentLocation() {
        return currentLocation;
    }

    public Instant lastLocationAt() {
        return lastLocationAt;
    }
}
