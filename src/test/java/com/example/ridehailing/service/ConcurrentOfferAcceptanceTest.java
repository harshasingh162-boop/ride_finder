package com.example.ridehailing.service;

import com.example.ridehailing.api.exception.InvalidStateException;
import com.example.ridehailing.api.exception.DuplicateException;
import com.example.ridehailing.domain.Car;
import com.example.ridehailing.domain.CarType;
import com.example.ridehailing.domain.DriverStatus;
import com.example.ridehailing.domain.Ride;
import com.example.ridehailing.domain.RideStatus;
import com.example.ridehailing.repository.DriverRepository;
import com.example.ridehailing.repository.OfferRepository;
import com.example.ridehailing.repository.RideRepository;
import com.example.ridehailing.repository.UserRepository;
import com.example.ridehailing.testsupport.MutableClock;
import com.example.ridehailing.testsupport.TestClockConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static com.example.ridehailing.testsupport.Locations.BENGALURU;
import static com.example.ridehailing.testsupport.Locations.offsetNorth;
import static com.example.ridehailing.testsupport.TestDrivers.available;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * The accept sequence is the one place two actors can genuinely collide, so it gets a real
 * concurrency test rather than only sequential ones.
 */
@SpringBootTest(properties = "ride-hailing.matching.offer-batch-size=1000")
@Import(TestClockConfig.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ConcurrentOfferAcceptanceTest {

    @Autowired
    private RideMatchingService rideMatchingService;

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DriverRepository driverRepository;

    @Autowired
    private OfferRepository offerRepository;

    @Autowired
    private RideRepository rideRepository;

    @Autowired
    private DriverService driverService;

    @Autowired
    private MutableClock clock;

    @Test
    void whenTenDriversAcceptTheSameRideAtOnceExactlyOneIsAssigned() throws Exception {
        int driverCount = 10;
        String userId = userService.register("Asha", "+91-9000000000").id();
        for (int i = 0; i < driverCount; i++) {
            driverRepository.save(available("d" + i, CarType.HATCHBACK, 5.0,
                    offsetNorth(BENGALURU, 0.1 * (i + 1)), clock.instant()));
        }

        Ride ride = rideMatchingService.requestRide(userId, BENGALURU, offsetNorth(BENGALURU, 10),
                CarType.HATCHBACK, null);
        rideMatchingService.acceptFare(userId, ride.id());
        List<String> offerIds = offerRepository.findByRideId(ride.id()).stream()
                .map(offer -> offer.id()).toList();
        assertThat(offerIds).hasSize(driverCount);

        ExecutorService pool = Executors.newFixedThreadPool(driverCount);
        CountDownLatch startGun = new CountDownLatch(1);
        AtomicInteger assigned = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();

        try {
            List<Future<Void>> futures = new ArrayList<>();
            for (String offerId : offerIds) {
                String driverId = offerRepository.findById(offerId).orElseThrow().driverId();
                Callable<Void> task = () -> {
                    startGun.await();
                    try {
                        rideMatchingService.acceptOffer(driverId, offerId);
                        assigned.incrementAndGet();
                    } catch (InvalidStateException expectedForLosers) {
                        rejected.incrementAndGet();
                    }
                    return null;
                };
                futures.add(pool.submit(task));
            }
            startGun.countDown();
            for (Future<Void> future : futures) {
                future.get(10, TimeUnit.SECONDS);
            }
        } finally {
            pool.shutdownNow();
        }

        assertThat(assigned).hasValue(1);
        assertThat(rejected).hasValue(driverCount - 1);

        Ride stored = rideRepository.findById(ride.id()).orElseThrow();
        assertThat(stored.status()).isEqualTo(RideStatus.DRIVER_ASSIGNED);
        assertThat(stored.driverId()).isNotNull();

        // Exactly one driver is on the trip; every loser was rolled back to AVAILABLE.
        assertThat(driverRepository.findAll())
                .filteredOn(driver -> driver.status() == DriverStatus.ON_TRIP)
                .extracting(driver -> driver.id())
                .containsExactly(stored.driverId());
        assertThat(driverRepository.findAll())
                .filteredOn(driver -> driver.status() == DriverStatus.AVAILABLE)
                .hasSize(driverCount - 1);
    }

    @Test
    void whenOneDriverAcceptsOffersForTwoRidesAtOnceOnlyOneRideWins() throws Exception {
        for (int iteration = 0; iteration < 100; iteration++) {
            String driverId = "shared-" + iteration;
            driverRepository.save(available(driverId, CarType.HATCHBACK, 5.0,
                    BENGALURU, clock.instant()));
            String firstUser = userService.register("Asha " + iteration, "+91-910000" + iteration).id();
            String secondUser = userService.register("Beena " + iteration, "+91-920000" + iteration).id();
            Ride first = prepareSearchingRide(firstUser);
            Ride second = prepareSearchingRide(secondUser);

            List<String> offerIds = List.of(offerFor(driverId, first.id()), offerFor(driverId, second.id()));
            List<Boolean> results = runTogether(offerIds, offerId -> {
                try {
                    rideMatchingService.acceptOffer(driverId, offerId);
                    return true;
                } catch (InvalidStateException expected) {
                    return false;
                }
            });

            assertThat(results).containsExactlyInAnyOrder(true, false);
            List<Ride> rides = List.of(rideRepository.findById(first.id()).orElseThrow(),
                    rideRepository.findById(second.id()).orElseThrow());
            assertThat(rides).filteredOn(ride -> ride.status() == RideStatus.DRIVER_ASSIGNED).hasSize(1);
            Ride loser = rides.stream().filter(ride -> ride.status() != RideStatus.DRIVER_ASSIGNED)
                    .findFirst().orElseThrow();
            assertThat(loser.status()).isIn(RideStatus.NO_DRIVER_FOUND, RideStatus.SEARCHING);
            if (loser.status() == RideStatus.SEARCHING) {
                assertThat(offerRepository.findByRideId(loser.id()).stream()
                        .anyMatch(offer -> offer.isLiveAt(clock.instant())))
                        .isTrue();
            }
            assertInvariants();
        }
    }

    @Test
    void whenThreeDriversAcceptOneRideAtOnceLosersReturnToAvailable() throws Exception {
        for (int iteration = 0; iteration < 100; iteration++) {
            List<String> driverIds = List.of("one-" + iteration, "two-" + iteration, "three-" + iteration);
            for (String driverId : driverIds) {
                driverRepository.save(available(driverId, CarType.HATCHBACK, 5.0,
                        offsetNorth(BENGALURU, 0.1), clock.instant()));
            }
            String userId = userService.register("Asha " + iteration, "+91-930000" + iteration).id();
            Ride ride = prepareSearchingRide(userId);
            List<String> offerIds = driverIds.stream().map(driverId -> offerFor(driverId, ride.id())).toList();

            List<Boolean> results = runTogether(offerIds, offerId -> {
                String driverId = offerRepository.findById(offerId).orElseThrow().driverId();
                try {
                    rideMatchingService.acceptOffer(driverId, offerId);
                    return true;
                } catch (InvalidStateException expected) {
                    return false;
                }
            });

            assertThat(results).containsExactlyInAnyOrder(true, false, false);
            String winner = rideRepository.findById(ride.id()).orElseThrow().driverId();
            assertThat(winner).isIn(driverIds);
            assertThat(driverRepository.findAll()).filteredOn(driver -> driverIds.contains(driver.id()))
                    .filteredOn(driver -> driver.status() == DriverStatus.ON_TRIP)
                    .extracting(driver -> driver.id()).containsExactly(winner);
            assertThat(driverRepository.findAll()).filteredOn(driver -> driverIds.contains(driver.id()))
                    .filteredOn(driver -> driver.status() == DriverStatus.AVAILABLE)
                    .hasSize(2);
            assertInvariants();
        }
    }

    @Test
    void whenUserCancelAndDriverAcceptRaceTheFinalStatesRemainConsistent() throws Exception {
        for (int iteration = 0; iteration < 100; iteration++) {
            String driverId = "cancel-driver-" + iteration;
            driverRepository.save(available(driverId, CarType.HATCHBACK, 5.0,
                    BENGALURU, clock.instant()));
            String userId = userService.register("Asha cancel-" + iteration,
                    "+91-940000" + iteration).id();
            Ride ride = prepareSearchingRide(userId);
            String offerId = offerFor(driverId, ride.id());
            List<Boolean> results = runTogether(List.of("cancel", offerId), action -> {
                try {
                    if (action.equals("cancel")) {
                        rideMatchingService.cancelSearch(userId, ride.id());
                    } else {
                        rideMatchingService.acceptOffer(driverId, offerId);
                    }
                    return true;
                } catch (InvalidStateException expected) {
                    return false;
                }
            });

            Ride finalRide = rideRepository.findById(ride.id()).orElseThrow();
            if (finalRide.status() == RideStatus.CANCELLED_BY_USER) {
                assertThat(driverRepository.findById(driverId).orElseThrow().status())
                        .isEqualTo(DriverStatus.AVAILABLE);
            } else {
                assertThat(finalRide.status()).isEqualTo(RideStatus.DRIVER_ASSIGNED);
                assertThat(finalRide.driverId()).isEqualTo(driverId);
                assertThat(driverRepository.findById(driverId).orElseThrow().status())
                        .isEqualTo(DriverStatus.ON_TRIP);
            }
            assertThat(results).contains(true);
            assertInvariants();
        }
    }

    @Test
    void whenTwentyThreadsRegisterTheSamePlateExactlyOneSucceeds() throws Exception {
        for (int iteration = 0; iteration < 100; iteration++) {
            int run = iteration;
            String plate = "CONCURRENT-PLATE-" + iteration;
            List<Boolean> results = runTogether(
                    java.util.stream.IntStream.range(0, 20).mapToObj(Integer::toString).toList(),
                    phoneSuffix -> {
                        try {
                            driverService.register("Driver " + phoneSuffix,
                                    "+91-95" + run + phoneSuffix,
                                    new Car(plate, "Model", CarType.HATCHBACK));
                            return true;
                        } catch (DuplicateException expected) {
                            return false;
                        }
                    });

            assertThat(results).filteredOn(Boolean.TRUE::equals).hasSize(1);
            assertThat(driverRepository.findAll()).filteredOn(driver -> driver.car().plate().equals(plate))
                    .hasSize(1);
            assertInvariants();
        }
    }

    private Ride prepareSearchingRide(String userId) {
        Ride ride = rideMatchingService.requestRide(userId, BENGALURU, offsetNorth(BENGALURU, 10),
                CarType.HATCHBACK, null);
        rideMatchingService.acceptFare(userId, ride.id());
        return ride;
    }

    private String offerFor(String driverId, String rideId) {
        return offerRepository.findByRideId(rideId).stream()
                .filter(offer -> offer.driverId().equals(driverId))
                .findFirst().orElseThrow().id();
    }

    private <T> List<Boolean> runTogether(List<T> inputs,
                                           java.util.function.Function<T, Boolean> action)
            throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(inputs.size());
        CountDownLatch startGate = new CountDownLatch(1);
        try {
            List<Future<Boolean>> futures = inputs.stream()
                    .map(input -> pool.submit(() -> {
                        startGate.await();
                        return action.apply(input);
                    }))
                    .toList();
            startGate.countDown();
            List<Boolean> results = new ArrayList<>();
            for (Future<Boolean> future : futures) {
                results.add(future.get(10, TimeUnit.SECONDS));
            }
            return results;
        } finally {
            pool.shutdownNow();
        }
    }

    private void assertInvariants() {
        List<Ride> rides = rideRepository.findAll();
        for (Ride ride : rides) {
            if (ride.status() == RideStatus.DRIVER_ASSIGNED) {
                assertThat(ride.driverId()).as("assigned ride " + ride.id()).isNotNull();
            }
        }
        for (var driver : driverRepository.findAll()) {
            if (driver.status() == DriverStatus.ON_TRIP) {
                assertThat(rides).as("active ride for driver " + driver.id())
                        .anyMatch(ride -> ride.status().isActive() && driver.id().equals(ride.driverId()));
            }
        }
    }
}
