package com.example.ridehailing.repository;

import com.example.ridehailing.domain.Driver;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryDriverRepository implements DriverRepository {

    private final ConcurrentHashMap<String, Driver> store = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> phoneIndex = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> plateIndex = new ConcurrentHashMap<>();

    @Override
    public Driver save(Driver driver) {
        store.put(driver.id(), driver);
        return driver;
    }

    @Override
    public SaveResult saveIfUnique(Driver driver) {
        if (phoneIndex.putIfAbsent(driver.phone(), driver.id()) != null) {
            return SaveResult.DUPLICATE_PHONE;
        }
        if (plateIndex.putIfAbsent(driver.car().plate(), driver.id()) != null) {
            // The phone was claimed a moment ago but the plate lost, so release the phone
            // again. Keyed on our own id so we can only ever release our own reservation.
            phoneIndex.remove(driver.phone(), driver.id());
            return SaveResult.DUPLICATE_PLATE;
        }
        store.put(driver.id(), driver);
        return SaveResult.SAVED;
    }

    @Override
    public Optional<Driver> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<Driver> findAll() {
        return List.copyOf(store.values());
    }
}
