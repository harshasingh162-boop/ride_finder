package com.example.ridehailing.api;

import com.example.ridehailing.pricing.FareBreakdown;
import com.example.ridehailing.service.MatchingService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/matching")
public class MatchingController {

    private final MatchingService matchingService;

    public MatchingController(MatchingService matchingService) {
        this.matchingService = matchingService;
    }

    @PostMapping("/estimate")
    public FareBreakdown estimate(@Valid @RequestBody EstimateRequest request) {
        return matchingService.estimate(request.pickup().toLocation(), request.drop().toLocation(),
                request.carType(), request.couponCode());
    }
}
