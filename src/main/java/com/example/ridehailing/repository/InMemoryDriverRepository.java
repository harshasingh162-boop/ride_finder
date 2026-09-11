package com.example.ridehailing.repository;

import com.example.ridehailing.domain.Driver;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryDriverRepository implements DriverRepository {

    private final ConcurrentHashMap<String, Driver> store = new ConcurrentHashMap<>();

    @Override
    public Driver save(Driver driver) {
        store.put(driver.id(), driver);
        return driver;
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
