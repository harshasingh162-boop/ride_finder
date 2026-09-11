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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestClockConfig.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class RideHistoryApiTest extends AbstractRideApiTest {

    private static final Location TEN_KM_AWAY = offsetNorth(PICKUP, 10);
    private static final Location NEARBY = offsetNorth(PICKUP, 1);

    private String userId;
    private String driverId;
    private String completedRideId;
    private String ongoingRideId;

    /**
     * One finished ride and one still under way, both with the same driver, separated in time so
     * that "newest first" is a real assertion rather than an accident of map ordering.
     */
    private void givenOneCompletedAndOneOngoingRide() throws Exception {
        userId = registerUser("+91-100");
        driverId = onlineDriverAt("+91-200", CarType.HATCHBACK, NEARBY);

        completedRideId = createRide(userId, TEN_KM_AWAY, CarType.HATCHBACK, null);
        acceptFare(userId, completedRideId).andExpect(status().isOk());
        acceptOffer(driverId, soleOfferOf(driverId)).andExpect(status().isOk());
        startRide(driverId, completedRideId).andExpect(status().isOk());
        endRide(driverId, completedRideId).andExpect(status().isOk());

        clock.advance(Duration.ofMinutes(5));
        // A real driver app keeps pinging. Without this the location is now older than the 60s
        // staleness window and the driver would correctly drop out of the next search.
        pushLocation(driverId, NEARBY).andExpect(status().isNoContent());

        ongoingRideId = createRide(userId, TEN_KM_AWAY, CarType.HATCHBACK, null);
        acceptFare(userId, ongoingRideId).andExpect(status().isOk());
        acceptOffer(driverId, soleOfferOf(driverId)).andExpect(status().isOk());
    }

    @Test
    void unfilteredUserHistoryReturnsBothRidesNewestFirst() throws Exception {
        givenOneCompletedAndOneOngoingRide();

        userHistory(userId, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(ongoingRideId))
                .andExpect(jsonPath("$[0].status").value("DRIVER_ASSIGNED"))
                .andExpect(jsonPath("$[1].id").value(completedRideId))
                .andExpect(jsonPath("$[1].status").value("COMPLETED"));
    }

    @Test
    void filteringUserHistoryByOngoingReturnsOnlyTheLiveRide() throws Exception {
        givenOneCompletedAndOneOngoingRide();

        userHistory(userId, "ONGOING")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(ongoingRideId));
    }

    @Test
    void filteringUserHistoryByCompletedReturnsOnlyTheFinishedRide() throws Exception {
        givenOneCompletedAndOneOngoingRide();

        userHistory(userId, "COMPLETED")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(completedRideId));
    }

    @Test
    void driverHistoryMirrorsTheSameRidesInTheSameOrder() throws Exception {
        givenOneCompletedAndOneOngoingRide();

        driverHistory(driverId, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(ongoingRideId))
                .andExpect(jsonPath("$[1].id").value(completedRideId));

        driverHistory(driverId, "COMPLETED")
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(completedRideId));
    }

    @Test
    void aFailedSearchShowsUpUnderCancelled() throws Exception {
        String rider = registerUser("+91-100");
        // Nobody online at all, so the search finds nothing.
        String rideId = createRide(rider, TEN_KM_AWAY, CarType.HATCHBACK, null);
        acceptFare(rider, rideId)
                .andExpect(jsonPath("$.status").value("NO_DRIVER_FOUND"));

        userHistory(rider, "CANCELLED")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(rideId));
    }

    @Test
    void aCancelledSearchShowsUpUnderCancelled() throws Exception {
        String rider = registerUser("+91-100");
        onlineDriverAt("+91-200", CarType.HATCHBACK, NEARBY);
        String rideId = createRide(rider, TEN_KM_AWAY, CarType.HATCHBACK, null);
        acceptFare(rider, rideId).andExpect(status().isOk());
        cancelSearch(rider, rideId).andExpect(status().isOk());

        userHistory(rider, "CANCELLED")
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].status").value("CANCELLED_BY_USER"));
    }

    @Test
    void oneRidersHistoryNeverLeaksIntoAnothers() throws Exception {
        givenOneCompletedAndOneOngoingRide();
        String otherRider = registerUser("+91-101");
        String otherRideId = createRide(otherRider, TEN_KM_AWAY, CarType.HATCHBACK, null);
        acceptFare(otherRider, otherRideId).andExpect(status().isOk());

        userHistory(otherRider, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(otherRideId));

        userHistory(userId, null)
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[?(@.id == '" + otherRideId + "')]").isEmpty());
    }

    @Test
    void aRiderWithNoRidesGetsAnEmptyList() throws Exception {
        String rider = registerUser("+91-100");

        userHistory(rider, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void anUnacceptedQuoteDoesNotAppearInHistory() throws Exception {
        String rider = registerUser("+91-100");
        createRide(rider, TEN_KM_AWAY, CarType.HATCHBACK, null);

        userHistory(rider, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void aDriverWhoHasNotTakenARideGetsAnEmptyList() throws Exception {
        String rider = registerUser("+91-100");
        String idleDriver = onlineDriverAt("+91-200", CarType.HATCHBACK, NEARBY);
        // A ride exists and is even offered to them, but they have not accepted it.
        String rideId = createRide(rider, TEN_KM_AWAY, CarType.HATCHBACK, null);
        acceptFare(rider, rideId).andExpect(status().isOk());

        driverHistory(idleDriver, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void historyForAnUnknownUserIsNotFound() throws Exception {
        userHistory("ghost", null)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    @Test
    void historyForAnUnknownDriverIsNotFound() throws Exception {
        driverHistory("ghost", null)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    @Test
    void anUnknownStatusFilterIsRejected() throws Exception {
        String rider = registerUser("+91-100");

        userHistory(rider, "FOO")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }
}
