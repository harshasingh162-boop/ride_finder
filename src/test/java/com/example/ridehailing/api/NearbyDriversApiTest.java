package com.example.ridehailing.api;

import com.example.ridehailing.domain.CarType;
import com.example.ridehailing.repository.DriverRepository;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Clock;

import static com.example.ridehailing.testsupport.Locations.BENGALURU;
import static com.example.ridehailing.testsupport.Locations.offsetNorth;
import static com.example.ridehailing.testsupport.TestDrivers.available;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class NearbyDriversApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DriverRepository driverRepository;

    @Autowired
    private Clock clock;

    @Test
    void returnsNearbyDriversRankedByDistance() throws Exception {
        driverRepository.save(available("far", CarType.HATCHBACK, 5.0, offsetNorth(BENGALURU, 1.5), clock.instant()));
        driverRepository.save(available("near", CarType.HATCHBACK, 4.2, offsetNorth(BENGALURU, 0.4), clock.instant()));

        mockMvc.perform(get("/api/v1/matching/nearby-drivers")
                        .param("lat", "12.9716").param("lng", "77.5946").param("carType", "HATCHBACK"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value("near"))
                .andExpect(jsonPath("$[0].rating").value(4.2))
                .andExpect(jsonPath("$[0].carType").value("HATCHBACK"))
                .andExpect(jsonPath("$[0].distanceKm").value(Matchers.closeTo(0.4, 0.001)))
                .andExpect(jsonPath("$[1].id").value("far"))
                .andExpect(jsonPath("$[1].distanceKm").value(Matchers.closeTo(1.5, 0.001)));
    }

    @Test
    void returnsUpgradedSedansWhenNoHatchbackIsNearby() throws Exception {
        driverRepository.save(available("sedan", CarType.SEDAN, 5.0, offsetNorth(BENGALURU, 0.4), clock.instant()));

        mockMvc.perform(get("/api/v1/matching/nearby-drivers")
                        .param("lat", "12.9716").param("lng", "77.5946").param("carType", "HATCHBACK"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value("sedan"))
                .andExpect(jsonPath("$[0].carType").value("SEDAN"));
    }

    @Test
    void returnsAnEmptyListWhenNoDriversAreNearby() throws Exception {
        mockMvc.perform(get("/api/v1/matching/nearby-drivers")
                        .param("lat", "12.9716").param("lng", "77.5946").param("carType", "HATCHBACK"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void returns400WhenLatIsMissing() throws Exception {
        mockMvc.perform(get("/api/v1/matching/nearby-drivers")
                        .param("lng", "77.5946").param("carType", "HATCHBACK"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("missing required parameter: lat"));
    }

    @Test
    void returns400ForAnUnknownCarType() throws Exception {
        mockMvc.perform(get("/api/v1/matching/nearby-drivers")
                        .param("lat", "12.9716").param("lng", "77.5946").param("carType", "SUV"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }

    @Test
    void returns400ForAnOutOfRangeLatitude() throws Exception {
        mockMvc.perform(get("/api/v1/matching/nearby-drivers")
                        .param("lat", "95.0").param("lng", "77.5946").param("carType", "HATCHBACK"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }
}
