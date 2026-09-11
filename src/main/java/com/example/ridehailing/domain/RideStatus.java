package com.example.ridehailing.domain;

public enum RideStatus {
    QUOTED,
    SEARCHING,
    DRIVER_ASSIGNED,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED_BY_USER,
    NO_DRIVER_FOUND;

    /** True while the ride still occupies the rider, blocking them from booking another. */
    public boolean isActive() {
        return this == SEARCHING || this == DRIVER_ASSIGNED || this == IN_PROGRESS;
    }
}
