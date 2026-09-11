package com.example.ridehailing.api;

import com.example.ridehailing.domain.CarType;
import com.example.ridehailing.domain.DriverStatus;
import com.example.ridehailing.domain.Location;
import com.example.ridehailing.domain.RideStatus;
import com.example.ridehailing.testsupport.TestClockConfig;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;

import java.math.BigDecimal;
import java.time.Duration;

import static com.example.ridehailing.testsupport.Locations.offsetNorth;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestClockConfig.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class RideLifecycleApiTest extends AbstractRideApiTest {

    private static final Location TEN_KM_AWAY = offsetNorth(PICKUP, 10);
    private static final Location ONE_KM_AWAY = offsetNorth(PICKUP, 1);
    private static final Location NEARBY = offsetNorth(PICKUP, 1);
    private static final Location OUT_OF_RANGE = offsetNorth(PICKUP, 2.5);

    // ------------------------------------------------------------ happy path

    @Test
    void aTenKilometreRideRunsFromQuoteToCompletion() throws Exception {
        String userId = registerUser("+91-100");
        String driverId = onlineDriverAt("+91-200", CarType.HATCHBACK, NEARBY);

        String rideId = createRide(userId, TEN_KM_AWAY, CarType.HATCHBACK, null);
        assertThat(rideStatusOf(rideId)).isEqualTo(RideStatus.QUOTED);

        acceptFare(userId, rideId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SEARCHING"))
                .andExpect(jsonPath("$.quotedFare.total").value(69.00));

        String offerId = soleOfferOf(driverId);

        acceptOffer(driverId, offerId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DRIVER_ASSIGNED"))
                .andExpect(jsonPath("$.driverId").value(driverId))
                .andExpect(jsonPath("$.upgraded").value(false))
                .andExpect(jsonPath("$.assignedCarType").value("HATCHBACK"));
        assertThat(driverStatusOf(driverId)).isEqualTo(DriverStatus.ON_TRIP);

        startRide(driverId, rideId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        endRide(driverId, rideId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.quotedFare.total").value(69.00));
        assertThat(driverStatusOf(driverId)).isEqualTo(DriverStatus.AVAILABLE);
    }

    @Test
    void aShortRideIsChargedTheMinimumFareEndToEnd() throws Exception {
        String userId = registerUser("+91-100");
        String driverId = onlineDriverAt("+91-200", CarType.HATCHBACK, NEARBY);

        String rideId = createRide(userId, ONE_KM_AWAY, CarType.HATCHBACK, null);
        acceptFare(userId, rideId).andExpect(status().isOk());
        acceptOffer(driverId, soleOfferOf(driverId)).andExpect(status().isOk());
        startRide(driverId, rideId).andExpect(status().isOk());

        endRide(driverId, rideId)
                .andExpect(jsonPath("$.quotedFare.minimumFareApplied").value(true))
                .andExpect(jsonPath("$.quotedFare.total").value(50.00));
    }

    @Test
    void aHatchbackRequestUpgradedToASedanIsStillChargedTheHatchbackFare() throws Exception {
        String userId = registerUser("+91-100");
        String driverId = onlineDriverAt("+91-200", CarType.SEDAN, NEARBY);

        String rideId = createRide(userId, TEN_KM_AWAY, CarType.HATCHBACK, null);
        acceptFare(userId, rideId).andExpect(status().isOk());

        acceptOffer(driverId, soleOfferOf(driverId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignedCarType").value("SEDAN"))
                .andExpect(jsonPath("$.requestedCarType").value("HATCHBACK"))
                .andExpect(jsonPath("$.upgraded").value(true))
                .andExpect(jsonPath("$.quotedFare.total").value(69.00));
    }

    @Test
    void aCouponAppliedAtRequestTimeIsTheAmountChargedAtTheEnd() throws Exception {
        mockMvc.perform(post("/api/v1/admin/coupons").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"SAVE20","type":"PERCENTAGE","percent":20,
                                 "maxDiscount":30,"validUntil":"2030-01-01T00:00:00Z"}
                                """))
                .andExpect(status().isCreated());
        String userId = registerUser("+91-100");
        String driverId = onlineDriverAt("+91-200", CarType.HATCHBACK, NEARBY);

        String rideId = createRide(userId, TEN_KM_AWAY, CarType.HATCHBACK, "SAVE20");
        acceptFare(userId, rideId).andExpect(status().isOk());
        acceptOffer(driverId, soleOfferOf(driverId)).andExpect(status().isOk());
        startRide(driverId, rideId).andExpect(status().isOk());

        endRide(driverId, rideId)
                // Only total is rounded to 2dp, so the intermediate discount carries the
                // floating-point tail of the haversine distance. The charged amount is exact.
                .andExpect(jsonPath("$.quotedFare.discount")
                        .value(Matchers.closeTo(new BigDecimal("13.80"), new BigDecimal("0.001"))))
                .andExpect(jsonPath("$.quotedFare.total").value(55.20));
    }

    @Test
    void anOfferCarriesThePickupDropAndFareTheDriverNeedsToDecide() throws Exception {
        String userId = registerUser("+91-100");
        String driverId = onlineDriverAt("+91-200", CarType.HATCHBACK, NEARBY);
        String rideId = createRide(userId, TEN_KM_AWAY, CarType.HATCHBACK, null);
        acceptFare(userId, rideId).andExpect(status().isOk());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/api/v1/drivers/" + driverId + "/offers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].rideId").value(rideId))
                .andExpect(jsonPath("$[0].pickup.lat").value(PICKUP.lat()))
                .andExpect(jsonPath("$[0].fare.total").value(69.00))
                .andExpect(jsonPath("$[0].expiresAt").isNotEmpty());
    }

    // -------------------------------------------------------------- no driver

    @Test
    void aRideWithNobodyInRangeEndsAsNoDriverFound() throws Exception {
        String userId = registerUser("+91-100");
        onlineDriverAt("+91-200", CarType.HATCHBACK, OUT_OF_RANGE);

        String rideId = createRide(userId, TEN_KM_AWAY, CarType.HATCHBACK, null);

        acceptFare(userId, rideId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("NO_DRIVER_FOUND"));
    }

    @Test
    void aRideEveryOfferedDriverRejectsEndsAsNoDriverFound() throws Exception {
        String userId = registerUser("+91-100");
        String first = onlineDriverAt("+91-200", CarType.HATCHBACK, offsetNorth(PICKUP, 0.5));
        String second = onlineDriverAt("+91-201", CarType.HATCHBACK, offsetNorth(PICKUP, 0.8));

        String rideId = createRide(userId, TEN_KM_AWAY, CarType.HATCHBACK, null);
        acceptFare(userId, rideId).andExpect(status().isOk());

        rejectOffer(first, soleOfferOf(first)).andExpect(status().isNoContent());
        rejectOffer(second, soleOfferOf(second)).andExpect(status().isNoContent());

        assertThat(rideStatusOf(rideId)).isEqualTo(RideStatus.NO_DRIVER_FOUND);
    }

    // ------------------------------------------------------------- conflicts

    @Test
    void aRiderAlreadyOnAnActiveRideCannotRequestAnother() throws Exception {
        String userId = registerUser("+91-100");
        onlineDriverAt("+91-200", CarType.HATCHBACK, NEARBY);
        String rideId = createRide(userId, TEN_KM_AWAY, CarType.HATCHBACK, null);
        acceptFare(userId, rideId).andExpect(status().isOk());

        requestRide(userId, TEN_KM_AWAY, CarType.HATCHBACK, null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Conflict"));
    }

    @Test
    void acceptingTheFareTwiceConflicts() throws Exception {
        String userId = registerUser("+91-100");
        onlineDriverAt("+91-200", CarType.HATCHBACK, NEARBY);
        String rideId = createRide(userId, TEN_KM_AWAY, CarType.HATCHBACK, null);

        acceptFare(userId, rideId).andExpect(status().isOk());
        acceptFare(userId, rideId).andExpect(status().isConflict());
    }

    @Test
    void acceptingTheFareOnSomebodyElsesRideIsForbidden() throws Exception {
        String owner = registerUser("+91-100");
        String stranger = registerUser("+91-101");
        String rideId = createRide(owner, TEN_KM_AWAY, CarType.HATCHBACK, null);

        acceptFare(stranger, rideId)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    void acceptingAnOfferAddressedToAnotherDriverIsForbidden() throws Exception {
        String userId = registerUser("+91-100");
        String offered = onlineDriverAt("+91-200", CarType.HATCHBACK, offsetNorth(PICKUP, 0.5));
        String interloper = onlineDriverAt("+91-201", CarType.SEDAN, offsetNorth(PICKUP, 0.8));
        String rideId = createRide(userId, TEN_KM_AWAY, CarType.HATCHBACK, null);
        acceptFare(userId, rideId).andExpect(status().isOk());
        String offerId = soleOfferOf(offered);

        acceptOffer(interloper, offerId)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Forbidden"));
        assertThat(rideStatusOf(rideId)).isEqualTo(RideStatus.SEARCHING);
        assertThat(driverStatusOf(interloper)).isEqualTo(DriverStatus.AVAILABLE);
    }

    @Test
    void anExpiredOfferCannotBeAcceptedAndLeavesTheRideSearching() throws Exception {
        String userId = registerUser("+91-100");
        String driverId = onlineDriverAt("+91-200", CarType.HATCHBACK, NEARBY);
        String rideId = createRide(userId, TEN_KM_AWAY, CarType.HATCHBACK, null);
        acceptFare(userId, rideId).andExpect(status().isOk());
        String offerId = soleOfferOf(driverId);

        clock.advance(Duration.ofSeconds(31));

        acceptOffer(driverId, offerId)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Conflict"));
        assertThat(rideStatusOf(rideId)).isEqualTo(RideStatus.SEARCHING);
        assertThat(driverStatusOf(driverId)).isEqualTo(DriverStatus.AVAILABLE);
    }

    @Test
    void anExpiredOfferDisappearsFromTheDriversOfferList() throws Exception {
        String userId = registerUser("+91-100");
        String driverId = onlineDriverAt("+91-200", CarType.HATCHBACK, NEARBY);
        String rideId = createRide(userId, TEN_KM_AWAY, CarType.HATCHBACK, null);
        acceptFare(userId, rideId).andExpect(status().isOk());
        assertThat(offerIdsOf(driverId)).hasSize(1);

        clock.advance(Duration.ofSeconds(31));

        assertThat(offerIdsOf(driverId)).isEmpty();
    }

    @Test
    void acceptingAnOfferAfterTheRiderCancelledTheSearchConflicts() throws Exception {
        String userId = registerUser("+91-100");
        String driverId = onlineDriverAt("+91-200", CarType.HATCHBACK, NEARBY);
        String rideId = createRide(userId, TEN_KM_AWAY, CarType.HATCHBACK, null);
        acceptFare(userId, rideId).andExpect(status().isOk());
        String offerId = soleOfferOf(driverId);

        cancelSearch(userId, rideId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED_BY_USER"));

        acceptOffer(driverId, offerId)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Conflict"));
        assertThat(driverStatusOf(driverId)).isEqualTo(DriverStatus.AVAILABLE);
        assertThat(rideStatusOf(rideId)).isEqualTo(RideStatus.CANCELLED_BY_USER);
    }

    @Test
    void cancellingTheSearchAfterADriverIsAssignedConflicts() throws Exception {
        String userId = registerUser("+91-100");
        String driverId = onlineDriverAt("+91-200", CarType.HATCHBACK, NEARBY);
        String rideId = createRide(userId, TEN_KM_AWAY, CarType.HATCHBACK, null);
        acceptFare(userId, rideId).andExpect(status().isOk());
        acceptOffer(driverId, soleOfferOf(driverId)).andExpect(status().isOk());

        cancelSearch(userId, rideId).andExpect(status().isConflict());
    }

    @Test
    void endingARideThatNeverStartedConflicts() throws Exception {
        String userId = registerUser("+91-100");
        String driverId = onlineDriverAt("+91-200", CarType.HATCHBACK, NEARBY);
        String rideId = createRide(userId, TEN_KM_AWAY, CarType.HATCHBACK, null);
        acceptFare(userId, rideId).andExpect(status().isOk());
        acceptOffer(driverId, soleOfferOf(driverId)).andExpect(status().isOk());

        endRide(driverId, rideId)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Conflict"));
    }

    @Test
    void startingSomebodyElsesRideIsForbidden() throws Exception {
        String userId = registerUser("+91-100");
        String assigned = onlineDriverAt("+91-200", CarType.HATCHBACK, offsetNorth(PICKUP, 0.5));
        String other = onlineDriverAt("+91-201", CarType.SEDAN, offsetNorth(PICKUP, 0.9));
        String rideId = createRide(userId, TEN_KM_AWAY, CarType.HATCHBACK, null);
        acceptFare(userId, rideId).andExpect(status().isOk());
        acceptOffer(assigned, soleOfferOf(assigned)).andExpect(status().isOk());

        startRide(other, rideId)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    void requestingARideForAnUnknownUserIsNotFound() throws Exception {
        requestRide("ghost", TEN_KM_AWAY, CarType.HATCHBACK, null)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    @Test
    void acceptingTheFareOnAnUnknownRideIsNotFound() throws Exception {
        String userId = registerUser("+91-100");

        acceptFare(userId, "ghost-ride")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    @Test
    void requestingARideWithAnInvalidCouponIsRejected() throws Exception {
        String userId = registerUser("+91-100");

        requestRide(userId, TEN_KM_AWAY, CarType.HATCHBACK, "GHOST")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }

    @Test
    void requestingARideFromAndToTheSamePointIsRejected() throws Exception {
        String userId = registerUser("+91-100");

        requestRide(userId, PICKUP, CarType.HATCHBACK, null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }
}
