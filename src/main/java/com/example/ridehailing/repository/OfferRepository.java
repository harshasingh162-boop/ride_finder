package com.example.ridehailing.repository;

import com.example.ridehailing.domain.RideOffer;

import java.util.List;
import java.util.Optional;

/**
 * Deliberately not built on the shared {@link Repository} contract: offers are only ever
 * reached through a ride or a driver, so a findAll() would be dead weight.
 */
public interface OfferRepository {

    RideOffer save(RideOffer offer);

    Optional<RideOffer> findById(String id);

    /** Every offer ever made for the ride, whatever its status, so re-dispatch can skip them. */
    List<RideOffer> findByRideId(String rideId);

    List<RideOffer> findPendingByDriverId(String driverId);
}
