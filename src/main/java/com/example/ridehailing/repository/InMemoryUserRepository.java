package com.example.ridehailing.repository;

import com.example.ridehailing.domain.User;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryUserRepository implements UserRepository {

    private final ConcurrentHashMap<String, User> store = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> phoneIndex = new ConcurrentHashMap<>();

    @Override
    public User save(User user) {
        store.put(user.id(), user);
        return user;
    }

    @Override
    public boolean saveIfPhoneAbsent(User user) {
        // putIfAbsent is the single atomic step that decides the winner, so concurrent
        // registrations of the same phone can never both succeed.
        if (phoneIndex.putIfAbsent(user.phone(), user.id()) != null) {
            return false;
        }
        store.put(user.id(), user);
        return true;
    }

    @Override
    public Optional<User> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<User> findAll() {
        return List.copyOf(store.values());
    }
}
