package com.example.ridehailing.service;

import com.example.ridehailing.api.exception.DuplicateException;
import com.example.ridehailing.api.exception.InvalidStateException;
import com.example.ridehailing.api.exception.NotFoundException;
import com.example.ridehailing.domain.Car;
import com.example.ridehailing.domain.Driver;
import com.example.ridehailing.domain.DriverStatus;
import com.example.ridehailing.domain.Location;
import com.example.ridehailing.repository.DriverRepository;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.UUID;

@Service
public class DriverService {

    private final DriverRepository driverRepository;
    private final Clock clock;

    public DriverService(DriverRepository driverRepository, Clock clock) {
        this.driverRepository = driverRepository;
        this.clock = clock;
    }

    public Driver register(String name, String phone, Car car) {
        Driver driver = new Driver(UUID.randomUUID().toString(), name, phone, car);
        return switch (driverRepository.saveIfUnique(driver)) {
            case SAVED -> driver;
            case DUPLICATE_PHONE -> throw new DuplicateException("phone already registered: " + phone);
            case DUPLICATE_PLATE -> throw new DuplicateException("plate already registered: " + car.plate());
        };
    }

    /** Allowed in any status: a driver on a trip still streams their position. */
    public void updateLocation(String driverId, Location location) {
        requireDriver(driverId).updateLocation(location, clock.instant());
    }

    public DriverStatus setAvailability(String driverId, boolean online) {
        Driver driver = requireDriver(driverId);
        DriverStatus target = online ? DriverStatus.AVAILABLE : DriverStatus.OFFLINE;
        DriverStatus current = driver.status();

        if (current == target) {
            return target;
        }
        if (current == DriverStatus.ON_TRIP) {
            throw new InvalidStateException("driver is on a trip and cannot change availability");
        }
        if (target == DriverStatus.AVAILABLE && driver.currentLocation() == null) {
            throw new InvalidStateException("driver must push a location before going online");
        }
        if (!driver.transition(current, target)) {
            throw new InvalidStateException("driver status changed concurrently, please retry");
        }
        return target;
    }

    private Driver requireDriver(String driverId) {
        return driverRepository.findById(driverId)
                .orElseThrow(() -> new NotFoundException("driver not found: " + driverId));
    }
}
