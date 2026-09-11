package com.example.ridehailing.domain;

public enum OfferStatus {
    PENDING,
    ACCEPTED,
    REJECTED,
    EXPIRED,
    /** Settled by the system rather than the driver: the ride was taken, cancelled, or superseded. */
    WITHDRAWN
}
