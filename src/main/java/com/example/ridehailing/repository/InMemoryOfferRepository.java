package com.example.ridehailing.repository;

import com.example.ridehailing.domain.OfferStatus;
import com.example.ridehailing.domain.RideOffer;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryOfferRepository implements OfferRepository {

    private final ConcurrentHashMap<String, RideOffer> store = new ConcurrentHashMap<>();

    @Override
    public RideOffer save(RideOffer offer) {
        store.put(offer.id(), offer);
        return offer;
    }

    @Override
    public Optional<RideOffer> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<RideOffer> findByRideId(String rideId) {
        return store.values().stream()
                .filter(offer -> offer.rideId().equals(rideId))
                .toList();
    }

    @Override
    public List<RideOffer> findPendingByDriverId(String driverId) {
        return store.values().stream()
                .filter(offer -> offer.driverId().equals(driverId))
                .filter(offer -> offer.status() == OfferStatus.PENDING)
                .toList();
    }
}
