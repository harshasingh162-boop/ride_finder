package com.example.ridehailing.api;

import com.example.ridehailing.domain.RideHistoryFilter;
import com.example.ridehailing.service.RideHistoryService;
import com.example.ridehailing.service.RideMatchingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/users/{userId}/rides")
public class UserRideController {

    private final RideMatchingService rideMatchingService;
    private final RideHistoryService rideHistoryService;

    public UserRideController(RideMatchingService rideMatchingService, RideHistoryService rideHistoryService) {
        this.rideMatchingService = rideMatchingService;
        this.rideHistoryService = rideHistoryService;
    }

    @GetMapping
    public List<RideResponse> history(@PathVariable String userId,
                                       @RequestParam(required = false) RideHistoryFilter status) {
        return rideHistoryService.forUser(userId, Optional.ofNullable(status)).stream()
                .map(RideResponse::from).toList();
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
