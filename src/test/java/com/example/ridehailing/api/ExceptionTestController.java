package com.example.ridehailing.api;

import com.example.ridehailing.api.exception.DuplicateException;
import com.example.ridehailing.api.exception.ForbiddenException;
import com.example.ridehailing.api.exception.InvalidCouponException;
import com.example.ridehailing.api.exception.InvalidStateException;
import com.example.ridehailing.api.exception.NotFoundException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Test-only controller that exists purely to exercise GlobalExceptionHandler
 * through a real Spring MVC dispatch, including bean validation.
 */
@RestController
@RequestMapping("/test")
class ExceptionTestController {

    @PostMapping("/not-found")
    void notFound() {
        throw new NotFoundException("ride not found");
    }

    @PostMapping("/invalid-state")
    void invalidState() {
        throw new InvalidStateException("ride is not in a cancellable state");
    }

    @PostMapping("/forbidden")
    void forbidden() {
        throw new ForbiddenException("not your ride");
    }

    @PostMapping("/duplicate")
    void duplicate() {
        throw new DuplicateException("phone already registered");
    }

    @PostMapping("/invalid-coupon")
    void invalidCoupon() {
        throw new InvalidCouponException("coupon has expired");
    }

    @PostMapping("/validate")
    void validate(@Valid @RequestBody ValidatedBody body) {
    }

    record ValidatedBody(@NotBlank String name) {
    }
}
