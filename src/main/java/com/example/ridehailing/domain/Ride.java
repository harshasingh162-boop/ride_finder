package com.example.ridehailing.domain;

import com.example.ridehailing.pricing.FareBreakdown;

import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public class Ride {

    private final String id;
    private final String userId;
    private final Location pickup;
    private final Location drop;
    private final CarType requestedCarType;
    /** Locked at request time, so surge and coupon cannot move under the rider afterwards. */
    private final FareBreakdown quotedFare;
    private final Instant requestedAt;
    private final AtomicReference<RideStatus> status;

    private volatile CarType assignedCarType;
    private volatile String driverId;
    private volatile boolean upgraded;
    private volatile Instant assignedAt;

    public Ride(String id, String userId, Location pickup, Location drop, CarType requestedCarType,
                FareBreakdown quotedFare, Instant requestedAt) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.userId = Objects.requireNonNull(userId, "userId must not be null");
        this.pickup = Objects.requireNonNull(pickup, "pickup must not be null");
        this.drop = Objects.requireNonNull(drop, "drop must not be null");
        this.requestedCarType = Objects.requireNonNull(requestedCarType, "requestedCarType must not be null");
        this.quotedFare = Objects.requireNonNull(quotedFare, "quotedFare must not be null");
        this.requestedAt = Objects.requireNonNull(requestedAt, "requestedAt must not be null");
        this.status = new AtomicReference<>(RideStatus.QUOTED);
    }

    public boolean transition(RideStatus from, RideStatus to) {
        return status.compareAndSet(from, to);
    }

    /** Records who took the ride. Only ever called by the thread that won the status CAS. */
    public void assignTo(Driver driver, Instant at) {
        this.driverId = driver.id();
        this.assignedCarType = driver.car().type();
        this.upgraded = driver.car().type() != requestedCarType;
        this.assignedAt = at;
    }

    public String id() {
        return id;
    }

    public String userId() {
        return userId;
    }

    public Location pickup() {
        return pickup;
    }

    public Location drop() {
        return drop;
    }

    public CarType requestedCarType() {
        return requestedCarType;
    }

    public FareBreakdown quotedFare() {
        return quotedFare;
    }

    public Instant requestedAt() {
        return requestedAt;
    }

    public RideStatus status() {
        return status.get();
    }

    public CarType assignedCarType() {
        return assignedCarType;
    }

    public String driverId() {
        return driverId;
    }

    public boolean upgraded() {
        return upgraded;
    }

    public Instant assignedAt() {
        return assignedAt;
    }
}
