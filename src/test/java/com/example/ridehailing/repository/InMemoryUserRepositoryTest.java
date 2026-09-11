package com.example.ridehailing.repository;

import com.example.ridehailing.domain.User;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryUserRepositoryTest {

    private final InMemoryUserRepository repository = new InMemoryUserRepository();

    @Test
    void savedUserCanBeFoundById() {
        User user = new User("user-1", "Asha", "+91-9000000000");

        repository.save(user);

        assertThat(repository.findById("user-1")).contains(user);
    }

    @Test
    void findAllReturnsEverySavedUser() {
        User first = new User("user-1", "Asha", "+91-9000000000");
        User second = new User("user-2", "Ravi", "+91-9000000001");

        repository.save(first);
        repository.save(second);

        assertThat(repository.findAll()).containsExactlyInAnyOrder(first, second);
    }

    @Test
    void findByIdReturnsEmptyForUnknownId() {
        assertThat(repository.findById("missing")).isEmpty();
    }
}
