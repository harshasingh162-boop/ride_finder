package com.example.ridehailing.repository;

import com.example.ridehailing.domain.Ride;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryRideRepository implements RideRepository {

    private final ConcurrentHashMap<String, Ride> store = new ConcurrentHashMap<>();

    @Override
    public Ride save(Ride ride) {
        store.put(ride.id(), ride);
        return ride;
    }

    /**
     * Synchronised rather than CAS-based on purpose. "this rider has no other active ride" is an
     * invariant across many Ride objects, and compare-and-set can only guard one reference at a
     * time. Contention is limited to ride creation, so a single short lock is the honest tool.
     */
    @Override
    public synchronized boolean saveIfUserHasNoActiveRide(Ride ride) {
        boolean alreadyRiding = store.values().stream()
                .anyMatch(existing -> existing.userId().equals(ride.userId()) && existing.status().isActive());
        if (alreadyRiding) {
            return false;
        }
        store.put(ride.id(), ride);
        return true;
    }

    @Override
    public Optional<Ride> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<Ride> findAll() {
        return List.copyOf(store.values());
    }
}
