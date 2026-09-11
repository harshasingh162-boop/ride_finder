package com.example.ridehailing.api;

import com.example.ridehailing.domain.CarType;
import com.example.ridehailing.domain.Location;
import com.example.ridehailing.testsupport.TestClockConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;

import java.time.Duration;

import static com.example.ridehailing.testsupport.Locations.offsetNorth;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestClockConfig.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class RideTrackingApiTest extends AbstractRideApiTest {

    private static final Location TEN_KM_AWAY = offsetNorth(PICKUP, 10);
    private static final Location NEARBY = offsetNorth(PICKUP, 1);

    private String userId;
    private String driverId;
    private String rideId;

    private void givenAnAssignedRide() throws Exception {
        userId = registerUser("+91-100");
        driverId = onlineDriverAt("+91-200", CarType.HATCHBACK, NEARBY);
        rideId = createRide(userId, TEN_KM_AWAY, CarType.HATCHBACK, null);
        acceptFare(userId, rideId).andExpect(status().isOk());
        acceptOffer(driverId, soleOfferOf(driverId)).andExpect(status().isOk());
    }

    @Test
    void trackingAnAssignedRideReportsTheDriversLastPushedLocation() throws Exception {
        givenAnAssignedRide();

        String body = bodyOf(tracking(rideId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rideId").value(rideId))
                .andExpect(jsonPath("$.status").value("DRIVER_ASSIGNED"))
                .andExpect(jsonPath("$.distanceToDropKm").doesNotExist())
                .andExpect(jsonPath("$.locationUpdatedAt").isNotEmpty()));

        assertThat(numberAt(body, "$.driverLocation.lat")).isCloseTo(NEARBY.lat(), within(1e-9));
        assertThat(numberAt(body, "$.driverLocation.lng")).isCloseTo(NEARBY.lng(), within(1e-9));
        assertThat(numberAt(body, "$.distanceToPickupKm")).isCloseTo(1.0, within(0.001));
    }

    @Test
    void aFreshLocationPushIsVisibleOnTheNextPoll() throws Exception {
        givenAnAssignedRide();
        Location movedCloser = offsetNorth(PICKUP, 0.5);

        clock.advance(Duration.ofSeconds(10));
        pushLocation(driverId, movedCloser).andExpect(status().isNoContent());

        String body = bodyOf(tracking(rideId).andExpect(status().isOk()));
        assertThat(numberAt(body, "$.driverLocation.lat")).isCloseTo(movedCloser.lat(), within(1e-9));
        assertThat(numberAt(body, "$.distanceToPickupKm")).isCloseTo(0.5, within(0.001));
    }

    @Test
    void etaIsDistanceOverTheConfiguredAverageSpeed() throws Exception {
        givenAnAssignedRide();
        // 5 km out at 25 km/h is 12 minutes. Pushed after assignment, because a driver this far
        // away would never have been matched in the first place.
        pushLocation(driverId, offsetNorth(PICKUP, 5)).andExpect(status().isNoContent());

        tracking(rideId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.etaMinutes").value(12));
    }

    @Test
    void onceUnderwayTrackingSwitchesToTheDistanceRemainingToTheDrop() throws Exception {
        givenAnAssignedRide();
        startRide(driverId, rideId).andExpect(status().isOk());
        // Halfway along a 10 km trip.
        pushLocation(driverId, offsetNorth(PICKUP, 5)).andExpect(status().isNoContent());

        String body = bodyOf(tracking(rideId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.distanceToPickupKm").doesNotExist())
                .andExpect(jsonPath("$.etaMinutes").value(12)));

        assertThat(numberAt(body, "$.distanceToDropKm")).isCloseTo(5.0, within(0.001));
    }

    @Test
    void trackingWhileStillSearchingReturnsNoDriverDetails() throws Exception {
        String rider = registerUser("+91-100");
        onlineDriverAt("+91-200", CarType.HATCHBACK, NEARBY);
        String searchingRideId = createRide(rider, TEN_KM_AWAY, CarType.HATCHBACK, null);
        acceptFare(rider, searchingRideId).andExpect(jsonPath("$.status").value("SEARCHING"));

        tracking(searchingRideId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SEARCHING"))
                .andExpect(jsonPath("$.driverLocation").doesNotExist())
                .andExpect(jsonPath("$.locationUpdatedAt").doesNotExist())
                .andExpect(jsonPath("$.distanceToPickupKm").doesNotExist())
                .andExpect(jsonPath("$.distanceToDropKm").doesNotExist())
                .andExpect(jsonPath("$.etaMinutes").doesNotExist());
    }

    @Test
    void trackingACompletedRideConflicts() throws Exception {
        givenAnAssignedRide();
        startRide(driverId, rideId).andExpect(status().isOk());
        endRide(driverId, rideId).andExpect(status().isOk());

        tracking(rideId)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Conflict"));
    }

    @Test
    void trackingAQuoteThatWasNeverAcceptedConflicts() throws Exception {
        String rider = registerUser("+91-100");
        String quotedRideId = createRide(rider, TEN_KM_AWAY, CarType.HATCHBACK, null);

        tracking(quotedRideId)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Conflict"));
    }

    @Test
    void trackingACancelledRideConflicts() throws Exception {
        String rider = registerUser("+91-100");
        onlineDriverAt("+91-200", CarType.HATCHBACK, NEARBY);
        String cancelledRideId = createRide(rider, TEN_KM_AWAY, CarType.HATCHBACK, null);
        acceptFare(rider, cancelledRideId).andExpect(status().isOk());
        cancelSearch(rider, cancelledRideId).andExpect(status().isOk());

        tracking(cancelledRideId).andExpect(status().isConflict());
    }

    @Test
    void trackingAnUnknownRideIsNotFound() throws Exception {
        tracking("ghost")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"));
    }
}
