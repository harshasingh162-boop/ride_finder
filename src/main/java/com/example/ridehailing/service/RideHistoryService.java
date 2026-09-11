package com.example.ridehailing.service;

import com.example.ridehailing.api.exception.NotFoundException;
import com.example.ridehailing.domain.Ride;
import com.example.ridehailing.domain.RideHistoryFilter;
import com.example.ridehailing.repository.DriverRepository;
import com.example.ridehailing.repository.RideRepository;
import com.example.ridehailing.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

@Service
public class RideHistoryService {

    private final RideRepository rideRepository;
    private final UserRepository userRepository;
    private final DriverRepository driverRepository;

    public RideHistoryService(RideRepository rideRepository, UserRepository userRepository,
                              DriverRepository driverRepository) {
        this.rideRepository = rideRepository;
        this.userRepository = userRepository;
        this.driverRepository = driverRepository;
    }

    public List<Ride> forUser(String userId, Optional<RideHistoryFilter> filter) {
        userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("user not found: " + userId));
        return history(ride -> ride.userId().equals(userId), filter);
    }

    public List<Ride> forDriver(String driverId, Optional<RideHistoryFilter> filter) {
        driverRepository.findById(driverId)
                .orElseThrow(() -> new NotFoundException("driver not found: " + driverId));
        return history(ride -> driverId.equals(ride.driverId()), filter);
    }

    /**
     * Scans every ride. Fine while everything is in one map; the scaling fix is a
     * userId -> rideIds (and driverId -> rideIds) index maintained on save and on assignment,
     * turning this into a lookup plus a fetch per id.
     */
    private List<Ride> history(Predicate<Ride> belongsToOwner, Optional<RideHistoryFilter> filter) {
        return rideRepository.findAll().stream()
                .filter(belongsToOwner)
                .filter(ride -> filter
                        .map(bucket -> bucket.matches(ride.status()))
                        .orElseGet(() -> RideHistoryFilter.isHistorical(ride.status())))
                .sorted(Comparator.comparing(Ride::requestedAt).reversed())
                .toList();
    }
}
