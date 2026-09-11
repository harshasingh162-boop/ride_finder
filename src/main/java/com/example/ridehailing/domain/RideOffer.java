package com.example.ridehailing.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public class RideOffer {

    private final String id;
    private final String rideId;
    private final String driverId;
    private final Instant createdAt;
    private final Instant expiresAt;
    private final AtomicReference<OfferStatus> status;

    public RideOffer(String id, String rideId, String driverId, Instant createdAt, Instant expiresAt) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.rideId = Objects.requireNonNull(rideId, "rideId must not be null");
        this.driverId = Objects.requireNonNull(driverId, "driverId must not be null");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt must not be null");
        this.status = new AtomicReference<>(OfferStatus.PENDING);
    }

    public boolean transition(OfferStatus from, OfferStatus to) {
        return status.compareAndSet(from, to);
    }

    /** Inclusive of the expiry instant itself: an offer is still live at exactly expiresAt. */
    public boolean isExpiredAt(Instant now) {
        return now.isAfter(expiresAt);
    }

    public boolean isLiveAt(Instant now) {
        return status.get() == OfferStatus.PENDING && !isExpiredAt(now);
    }

    public String id() {
        return id;
    }

    public String rideId() {
        return rideId;
    }

    public String driverId() {
        return driverId;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant expiresAt() {
        return expiresAt;
    }

    public OfferStatus status() {
        return status.get();
    }
}
