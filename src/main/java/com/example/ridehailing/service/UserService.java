package com.example.ridehailing.service;

import com.example.ridehailing.api.exception.DuplicateException;
import com.example.ridehailing.domain.User;
import com.example.ridehailing.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User register(String name, String phone) {
        User user = new User(UUID.randomUUID().toString(), name, phone);
        if (!userRepository.saveIfPhoneAbsent(user)) {
            throw new DuplicateException("phone already registered: " + phone);
        }
        return user;
    }
}
