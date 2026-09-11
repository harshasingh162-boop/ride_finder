package com.example.ridehailing.api;

import com.example.ridehailing.service.RideMatchingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users/{userId}/rides")
public class UserRideController {

    private final RideMatchingService rideMatchingService;

    public UserRideController(RideMatchingService rideMatchingService) {
        this.rideMatchingService = rideMatchingService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RideResponse request(@PathVariable String userId, @Valid @RequestBody RequestRideRequest request) {
        return RideResponse.from(rideMatchingService.requestRide(userId,
                request.pickup().toLocation(), request.drop().toLocation(),
                request.carType(), request.couponCode()));
    }

    @PostMapping("/{rideId}/accept-fare")
    public RideResponse acceptFare(@PathVariable String userId, @PathVariable String rideId) {
        return RideResponse.from(rideMatchingService.acceptFare(userId, rideId));
    }

    @PostMapping("/{rideId}/cancel-search")
    public RideResponse cancelSearch(@PathVariable String userId, @PathVariable String rideId) {
        return RideResponse.from(rideMatchingService.cancelSearch(userId, rideId));
    }
}
