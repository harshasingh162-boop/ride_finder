package com.example.ridehailing.repository;

import com.example.ridehailing.domain.Ride;

public interface RideRepository extends Repository<Ride, String> {

    /**
     * Stores the ride only if its rider has no other ride in an active status.
     *
     * @return false if the rider is already on an active ride, in which case nothing is stored
     */
    boolean saveIfUserHasNoActiveRide(Ride ride);
}
