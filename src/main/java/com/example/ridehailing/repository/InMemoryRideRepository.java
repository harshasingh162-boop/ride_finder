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

    @Override
    public Optional<Ride> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<Ride> findAll() {
        return List.copyOf(store.values());
    }
}
