package com.example.ridehailing.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public class Ride {

    private final String id;
    private final String userId;
    private final Location pickup;
    private final Location drop;
    private final CarType requestedCarType;
    private final Instant requestedAt;
    private final AtomicReference<RideStatus> status;

    private volatile CarType assignedCarType;
    private volatile String driverId;
    private volatile boolean upgraded;
    private volatile String couponCode;

    public Ride(String id, String userId, Location pickup, Location drop, CarType requestedCarType, Instant requestedAt) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.userId = Objects.requireNonNull(userId, "userId must not be null");
        this.pickup = Objects.requireNonNull(pickup, "pickup must not be null");
        this.drop = Objects.requireNonNull(drop, "drop must not be null");
        this.requestedCarType = Objects.requireNonNull(requestedCarType, "requestedCarType must not be null");
        this.requestedAt = Objects.requireNonNull(requestedAt, "requestedAt must not be null");
        this.status = new AtomicReference<>(RideStatus.QUOTED);
    }

    public boolean transition(RideStatus from, RideStatus to) {
        return status.compareAndSet(from, to);
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

    public Instant requestedAt() {
        return requestedAt;
    }

    public RideStatus status() {
        return status.get();
    }

    public CarType assignedCarType() {
        return assignedCarType;
    }

    public void setAssignedCarType(CarType assignedCarType) {
        this.assignedCarType = assignedCarType;
    }

    public String driverId() {
        return driverId;
    }

    public void setDriverId(String driverId) {
        this.driverId = driverId;
    }

    public boolean upgraded() {
        return upgraded;
    }

    public void setUpgraded(boolean upgraded) {
        this.upgraded = upgraded;
    }

    public String couponCode() {
        return couponCode;
    }

    public void setCouponCode(String couponCode) {
        this.couponCode = couponCode;
    }
}
