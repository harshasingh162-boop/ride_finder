package com.example.ridehailing.service;

import com.example.ridehailing.api.exception.ForbiddenException;
import com.example.ridehailing.api.exception.InvalidStateException;
import com.example.ridehailing.api.exception.NotFoundException;
import com.example.ridehailing.config.MatchingProperties;
import com.example.ridehailing.domain.CarType;
import com.example.ridehailing.domain.Driver;
import com.example.ridehailing.domain.DriverStatus;
import com.example.ridehailing.domain.Location;
import com.example.ridehailing.domain.OfferStatus;
import com.example.ridehailing.domain.Ride;
import com.example.ridehailing.domain.RideOffer;
import com.example.ridehailing.domain.RideStatus;
import com.example.ridehailing.pricing.FareBreakdown;
import com.example.ridehailing.repository.DriverRepository;
import com.example.ridehailing.repository.OfferRepository;
import com.example.ridehailing.repository.RideRepository;
import com.example.ridehailing.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class RideMatchingService {

    private final RideRepository rideRepository;
    private final OfferRepository offerRepository;
    private final DriverRepository driverRepository;
    private final UserRepository userRepository;
    private final MatchingService matchingService;
    private final Clock clock;
    private final int offerBatchSize;
    private final Duration offerTtl;

    public RideMatchingService(RideRepository rideRepository, OfferRepository offerRepository,
                               DriverRepository driverRepository, UserRepository userRepository,
                               MatchingService matchingService, Clock clock, MatchingProperties properties) {
        this.rideRepository = rideRepository;
        this.offerRepository = offerRepository;
        this.driverRepository = driverRepository;
        this.userRepository = userRepository;
        this.matchingService = matchingService;
        this.clock = clock;
        this.offerBatchSize = properties.offerBatchSize();
        this.offerTtl = properties.offerTtl();
    }

    // ---------------------------------------------------------------- rider

    public Ride requestRide(String userId, Location pickup, Location drop, CarType carType, String couponCode) {
        requireUser(userId);
        // Reuses the estimate path, so surge and coupon rules cannot drift between a quote
        // the rider was shown and the ride they actually booked.
        FareBreakdown quotedFare = matchingService.estimate(pickup, drop, carType, couponCode);
        Ride ride = new Ride(UUID.randomUUID().toString(), userId, pickup, drop, carType,
                quotedFare, clock.instant());
        if (!rideRepository.saveIfUserHasNoActiveRide(ride)) {
            throw new InvalidStateException("user already has an active ride: " + userId);
        }
        return ride;
    }

    public Ride acceptFare(String userId, String rideId) {
        Ride ride = requireRide(rideId);
        requireRider(ride, userId);
        if (!ride.transition(RideStatus.QUOTED, RideStatus.SEARCHING)) {
            throw new InvalidStateException("ride is not awaiting fare acceptance, it is " + ride.status());
        }
        dispatch(ride);
        return ride;
    }

    public Ride cancelSearch(String userId, String rideId) {
        Ride ride = requireRide(rideId);
        requireRider(ride, userId);
        if (!ride.transition(RideStatus.SEARCHING, RideStatus.CANCELLED_BY_USER)) {
            throw new InvalidStateException("ride is not searching, it is " + ride.status());
        }
        withdrawOffersFor(ride, null);
        return ride;
    }

    // --------------------------------------------------------------- driver

    public List<PendingOffer> offersFor(String driverId) {
        requireDriver(driverId);
        Instant now = clock.instant();
        return offerRepository.findPendingByDriverId(driverId).stream()
                .filter(offer -> !offer.isExpiredAt(now))
                .map(offer -> new PendingOffer(offer, requireRide(offer.rideId())))
                .toList();
    }

    /**
     * Three compare-and-sets in order: the offer, the driver, then the ride. Each one is the
     * single point where a concurrent actor could otherwise double-book, and a failure at any
     * step unwinds the earlier ones before reporting a conflict.
     */
    public Ride acceptOffer(String driverId, String offerId) {
        RideOffer offer = requireOffer(offerId);
        requireAddressee(offer, driverId);
        Driver driver = requireDriver(driverId);
        Ride ride = requireRide(offer.rideId());
        Instant now = clock.instant();

        if (offer.isExpiredAt(now)) {
            offer.transition(OfferStatus.PENDING, OfferStatus.EXPIRED);
            throw new InvalidStateException("offer has expired");
        }

        // 1. Claim the offer. Loses to a competing accept/reject/withdrawal of this same offer.
        if (!offer.transition(OfferStatus.PENDING, OfferStatus.ACCEPTED)) {
            throw new InvalidStateException("offer is no longer pending, it is " + offer.status());
        }
        // 2. Claim the driver. Loses if they took another ride or went offline a moment ago.
        if (!driver.transition(DriverStatus.AVAILABLE, DriverStatus.ON_TRIP)) {
            offer.transition(OfferStatus.ACCEPTED, OfferStatus.PENDING);
            throw new InvalidStateException("driver is not available, they are " + driver.status());
        }
        // 3. Claim the ride. Loses to another driver who was assigned first, or to a cancellation
        //    that landed between step 1 and here.
        if (!ride.transition(RideStatus.SEARCHING, RideStatus.DRIVER_ASSIGNED)) {
            driver.transition(DriverStatus.ON_TRIP, DriverStatus.AVAILABLE);
            // Settled rather than restored: this ride can never be accepted again.
            offer.transition(OfferStatus.ACCEPTED, OfferStatus.WITHDRAWN);
            throw new InvalidStateException("ride is no longer searching, it is " + ride.status());
        }

        ride.assignTo(driver, now);
        settleLosingOffers(ride, offer);
        return ride;
    }

    public void rejectOffer(String driverId, String offerId) {
        RideOffer offer = requireOffer(offerId);
        requireAddressee(offer, driverId);
        if (!offer.transition(OfferStatus.PENDING, OfferStatus.REJECTED)) {
            throw new InvalidStateException("offer is no longer pending, it is " + offer.status());
        }
        rideRepository.findById(offer.rideId()).ifPresent(this::redispatchIfNobodyIsConsidering);
    }

    public Ride startRide(String driverId, String rideId) {
        Ride ride = requireRide(rideId);
        requireAssignedDriver(ride, driverId);
        if (!ride.transition(RideStatus.DRIVER_ASSIGNED, RideStatus.IN_PROGRESS)) {
            throw new InvalidStateException("ride cannot start from " + ride.status());
        }
        return ride;
    }

    public Ride endRide(String driverId, String rideId) {
        Ride ride = requireRide(rideId);
        requireAssignedDriver(ride, driverId);
        if (!ride.transition(RideStatus.IN_PROGRESS, RideStatus.COMPLETED)) {
            throw new InvalidStateException("ride cannot end from " + ride.status());
        }
        requireDriver(driverId).transition(DriverStatus.ON_TRIP, DriverStatus.AVAILABLE);
        return ride;
    }

    // ------------------------------------------------------------- dispatch

    /**
     * The one place offers are created. Called on fare acceptance and on every re-dispatch, so
     * the "who have we already asked" rule can never diverge between those paths.
     */
    private void dispatch(Ride ride) {
        Set<String> alreadyOffered = offerRepository.findByRideId(ride.id()).stream()
                .map(RideOffer::driverId)
                .collect(Collectors.toUnmodifiableSet());

        List<Driver> batch = matchingService.findNearbyDrivers(ride.pickup(), ride.requestedCarType())
                .rankedCandidates().stream()
                .filter(driver -> !alreadyOffered.contains(driver.id()))
                .limit(offerBatchSize)
                .toList();

        if (batch.isEmpty()) {
            ride.transition(RideStatus.SEARCHING, RideStatus.NO_DRIVER_FOUND);
            return;
        }
        Instant now = clock.instant();
        for (Driver driver : batch) {
            offerRepository.save(new RideOffer(UUID.randomUUID().toString(), ride.id(), driver.id(),
                    now, now.plus(offerTtl)));
        }
    }

    private void redispatchIfNobodyIsConsidering(Ride ride) {
        if (ride.status() != RideStatus.SEARCHING) {
            return;
        }
        Instant now = clock.instant();
        boolean someoneStillHasIt = offerRepository.findByRideId(ride.id()).stream()
                .anyMatch(offer -> offer.isLiveAt(now));
        if (!someoneStillHasIt) {
            dispatch(ride);
        }
    }

    /**
     * After a driver wins a ride: every other offer for that ride is dead, and so is every other
     * offer that driver was holding. Rides left with nobody considering them go back out.
     */
    private void settleLosingOffers(Ride ride, RideOffer acceptedOffer) {
        withdrawOffersFor(ride, acceptedOffer.id());

        Set<String> ridesNeedingAnotherLook = new LinkedHashSet<>();
        for (RideOffer held : offerRepository.findPendingByDriverId(acceptedOffer.driverId())) {
            if (!held.rideId().equals(ride.id())
                    && held.transition(OfferStatus.PENDING, OfferStatus.WITHDRAWN)) {
                ridesNeedingAnotherLook.add(held.rideId());
            }
        }
        for (String rideId : ridesNeedingAnotherLook) {
            rideRepository.findById(rideId).ifPresent(this::redispatchIfNobodyIsConsidering);
        }
    }

    private void withdrawOffersFor(Ride ride, String exceptOfferId) {
        for (RideOffer offer : offerRepository.findByRideId(ride.id())) {
            if (!offer.id().equals(exceptOfferId)) {
                offer.transition(OfferStatus.PENDING, OfferStatus.WITHDRAWN);
            }
        }
    }

    // -------------------------------------------------------------- lookups

    private void requireUser(String userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("user not found: " + userId));
    }

    private Driver requireDriver(String driverId) {
        return driverRepository.findById(driverId)
                .orElseThrow(() -> new NotFoundException("driver not found: " + driverId));
    }

    private Ride requireRide(String rideId) {
        return rideRepository.findById(rideId)
                .orElseThrow(() -> new NotFoundException("ride not found: " + rideId));
    }

    private RideOffer requireOffer(String offerId) {
        return offerRepository.findById(offerId)
                .orElseThrow(() -> new NotFoundException("offer not found: " + offerId));
    }

    private void requireRider(Ride ride, String userId) {
        if (!ride.userId().equals(userId)) {
            throw new ForbiddenException("ride " + ride.id() + " does not belong to user " + userId);
        }
    }

    private void requireAssignedDriver(Ride ride, String driverId) {
        if (!driverId.equals(ride.driverId())) {
            throw new ForbiddenException("ride " + ride.id() + " is not assigned to driver " + driverId);
        }
    }

    private void requireAddressee(RideOffer offer, String driverId) {
        if (!offer.driverId().equals(driverId)) {
            throw new ForbiddenException("offer " + offer.id() + " was not made to driver " + driverId);
        }
    }
}
