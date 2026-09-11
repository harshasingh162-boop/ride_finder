package com.example.ridehailing.api;

import com.example.ridehailing.pricing.ConditionsProvider;
import com.example.ridehailing.pricing.RideConditions;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/conditions")
public class AdminConditionsController {

    private final ConditionsProvider conditionsProvider;

    public AdminConditionsController(ConditionsProvider conditionsProvider) {
        this.conditionsProvider = conditionsProvider;
    }

    @PutMapping
    public RideConditions update(@Valid @RequestBody UpdateConditionsRequest request) {
        RideConditions conditions = new RideConditions(request.raining(), request.trafficLevel());
        conditionsProvider.update(conditions);
        return conditions;
    }
}
