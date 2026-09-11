package com.example.ridehailing.api;

import com.example.ridehailing.service.TrackingService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/rides")
public class TrackingController {

    private final TrackingService trackingService;

    public TrackingController(TrackingService trackingService) {
        this.trackingService = trackingService;
    }

    @GetMapping("/{rideId}/tracking")
    public TrackingResponse track(@PathVariable String rideId) {
        return TrackingResponse.from(trackingService.track(rideId));
    }
}
