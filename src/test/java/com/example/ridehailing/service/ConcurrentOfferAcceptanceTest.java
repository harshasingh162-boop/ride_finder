package com.example.ridehailing.service;

import com.example.ridehailing.api.exception.InvalidStateException;
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
@SpringBootTest(properties = "ride-hailing.matching.offer-batch-size=10")
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
}
