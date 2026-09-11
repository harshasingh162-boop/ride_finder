package com.example.ridehailing.service;

import com.example.ridehailing.api.exception.DuplicateException;
import com.example.ridehailing.domain.User;
import com.example.ridehailing.repository.InMemoryUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserServiceTest {

    private InMemoryUserRepository userRepository;
    private UserService userService;

    @BeforeEach
    void setUp() {
        userRepository = new InMemoryUserRepository();
        userService = new UserService(userRepository);
    }

    @Test
    void registeringAUserStoresThemUnderAGeneratedId() {
        User user = userService.register("Asha", "+91-9000000000");

        assertThat(user.id()).isNotBlank();
        assertThat(userRepository.findById(user.id())).contains(user);
    }

    @Test
    void registeringADuplicatePhoneIsRejected() {
        userService.register("Asha", "+91-9000000000");

        assertThatThrownBy(() -> userService.register("Someone Else", "+91-9000000000"))
                .isInstanceOf(DuplicateException.class);
    }

    @Test
    void aRejectedDuplicateLeavesOnlyTheFirstUserStored() {
        userService.register("Asha", "+91-9000000000");

        assertThatThrownBy(() -> userService.register("Someone Else", "+91-9000000000"))
                .isInstanceOf(DuplicateException.class);

        assertThat(userRepository.findAll()).singleElement()
                .extracting(User::name).isEqualTo("Asha");
    }

    @Test
    void onlyOneOfTwentyConcurrentRegistrationsOfTheSamePhoneSucceeds() throws Exception {
        int threadCount = 20;
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startGun = new CountDownLatch(1);
        AtomicInteger created = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();

        try {
            List<Future<Void>> futures = new ArrayList<>();
            for (int i = 0; i < threadCount; i++) {
                Callable<Void> task = () -> {
                    startGun.await();
                    try {
                        userService.register("Asha", "+91-9000000000");
                        created.incrementAndGet();
                    } catch (DuplicateException expectedForLosers) {
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

        assertThat(created).hasValue(1);
        assertThat(rejected).hasValue(threadCount - 1);
        assertThat(userRepository.findAll()).hasSize(1);
    }
}
