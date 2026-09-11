package com.example.ridehailing.domain;

public enum RideStatus {
    QUOTED,
    SEARCHING,
    DRIVER_ASSIGNED,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED,
    NO_DRIVERS_FOUND;

    public boolean isActive() {
        return this == SEARCHING || this == DRIVER_ASSIGNED || this == IN_PROGRESS;
    }
}
