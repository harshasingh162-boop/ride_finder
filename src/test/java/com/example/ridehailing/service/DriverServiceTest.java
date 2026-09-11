package com.example.ridehailing.service;

import com.example.ridehailing.api.exception.DuplicateException;
import com.example.ridehailing.api.exception.InvalidStateException;
import com.example.ridehailing.api.exception.NotFoundException;
import com.example.ridehailing.domain.Car;
import com.example.ridehailing.domain.CarType;
import com.example.ridehailing.domain.Driver;
import com.example.ridehailing.domain.DriverStatus;
import com.example.ridehailing.domain.Location;
import com.example.ridehailing.repository.InMemoryDriverRepository;
import com.example.ridehailing.testsupport.MutableClock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DriverServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-11T10:00:00Z");
    private static final Location BENGALURU = new Location(12.9716, 77.5946);

    private MutableClock clock;
    private InMemoryDriverRepository driverRepository;
    private DriverService driverService;

    @BeforeEach
    void setUp() {
        clock = MutableClock.startingAt(NOW);
        driverRepository = new InMemoryDriverRepository();
        driverService = new DriverService(driverRepository, clock);
    }

    private Driver registerKiran() {
        return driverService.register("Kiran", "+91-9000000002",
                new Car("KA-01-AB-1234", "Swift", CarType.HATCHBACK));
    }

    private Driver registerAvailableKiran() {
        Driver driver = registerKiran();
        driverService.updateLocation(driver.id(), BENGALURU);
        driverService.setAvailability(driver.id(), true);
        return driver;
    }

    @Test
    void aNewlyRegisteredDriverStartsOffline() {
        Driver driver = registerKiran();

        assertThat(driver.status()).isEqualTo(DriverStatus.OFFLINE);
        assertThat(driverRepository.findById(driver.id())).contains(driver);
    }

    @Test
    void pushingALocationRecordsItWithTheClocksInstant() {
        Driver driver = registerKiran();
        clock.advance(Duration.ofMinutes(7));

        driverService.updateLocation(driver.id(), BENGALURU);

        assertThat(driver.currentLocation()).isEqualTo(BENGALURU);
        assertThat(driver.lastLocationAt()).isEqualTo(NOW.plus(Duration.ofMinutes(7)));
    }

    @Test
    void aDriverOnATripMayStillPushLocations() {
        Driver driver = registerAvailableKiran();
        driver.transition(DriverStatus.AVAILABLE, DriverStatus.ON_TRIP);
        Location moved = new Location(12.9800, 77.6000);

        driverService.updateLocation(driver.id(), moved);

        assertThat(driver.currentLocation()).isEqualTo(moved);
    }

    @Test
    void goingOnlineAfterPushingALocationMakesTheDriverAvailable() {
        Driver driver = registerKiran();
        driverService.updateLocation(driver.id(), BENGALURU);

        assertThat(driverService.setAvailability(driver.id(), true)).isEqualTo(DriverStatus.AVAILABLE);
        assertThat(driver.status()).isEqualTo(DriverStatus.AVAILABLE);
    }

    @Test
    void goingOnlineTwiceIsIdempotent() {
        Driver driver = registerAvailableKiran();

        assertThat(driverService.setAvailability(driver.id(), true)).isEqualTo(DriverStatus.AVAILABLE);
        assertThat(driver.status()).isEqualTo(DriverStatus.AVAILABLE);
    }

    @Test
    void goingOfflineFromAvailableIsAllowed() {
        Driver driver = registerAvailableKiran();

        assertThat(driverService.setAvailability(driver.id(), false)).isEqualTo(DriverStatus.OFFLINE);
        assertThat(driver.status()).isEqualTo(DriverStatus.OFFLINE);
    }

    @Test
    void goingOfflineTwiceIsIdempotent() {
        Driver driver = registerKiran();

        assertThat(driverService.setAvailability(driver.id(), false)).isEqualTo(DriverStatus.OFFLINE);
        assertThat(driver.status()).isEqualTo(DriverStatus.OFFLINE);
    }

    @Test
    void goingOnlineWithoutALocationIsRejected() {
        Driver driver = registerKiran();

        assertThatThrownBy(() -> driverService.setAvailability(driver.id(), true))
                .isInstanceOf(InvalidStateException.class);
        assertThat(driver.status()).isEqualTo(DriverStatus.OFFLINE);
    }

    @Test
    void aDriverOnATripCannotGoOffline() {
        Driver driver = registerAvailableKiran();
        driver.transition(DriverStatus.AVAILABLE, DriverStatus.ON_TRIP);

        assertThatThrownBy(() -> driverService.setAvailability(driver.id(), false))
                .isInstanceOf(InvalidStateException.class);
        assertThat(driver.status()).isEqualTo(DriverStatus.ON_TRIP);
    }

    @Test
    void aDriverOnATripCannotGoBackToAvailable() {
        Driver driver = registerAvailableKiran();
        driver.transition(DriverStatus.AVAILABLE, DriverStatus.ON_TRIP);

        assertThatThrownBy(() -> driverService.setAvailability(driver.id(), true))
                .isInstanceOf(InvalidStateException.class);
    }

    @Test
    void registeringADuplicatePhoneIsRejected() {
        registerKiran();

        assertThatThrownBy(() -> driverService.register("Meera", "+91-9000000002",
                new Car("KA-02-CD-5678", "Baleno", CarType.HATCHBACK)))
                .isInstanceOf(DuplicateException.class);
    }

    @Test
    void registeringADuplicatePlateIsRejected() {
        registerKiran();

        assertThatThrownBy(() -> driverService.register("Meera", "+91-9000000003",
                new Car("KA-01-AB-1234", "Baleno", CarType.HATCHBACK)))
                .isInstanceOf(DuplicateException.class);
    }

    @Test
    void aPlateClashReleasesThePhoneSoItCanStillBeUsed() {
        registerKiran();

        assertThatThrownBy(() -> driverService.register("Meera", "+91-9000000003",
                new Car("KA-01-AB-1234", "Baleno", CarType.HATCHBACK)))
                .isInstanceOf(DuplicateException.class);

        Driver meera = driverService.register("Meera", "+91-9000000003",
                new Car("KA-02-CD-5678", "Baleno", CarType.HATCHBACK));

        assertThat(meera.id()).isNotBlank();
        assertThat(driverRepository.findAll()).hasSize(2);
    }

    @Test
    void pushingALocationForAnUnknownDriverIsRejected() {
        assertThatThrownBy(() -> driverService.updateLocation("ghost", BENGALURU))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void changingAvailabilityForAnUnknownDriverIsRejected() {
        assertThatThrownBy(() -> driverService.setAvailability("ghost", true))
                .isInstanceOf(NotFoundException.class);
    }
}
