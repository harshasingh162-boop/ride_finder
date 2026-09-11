package com.example.ridehailing.api;

import com.example.ridehailing.domain.CarType;
import com.example.ridehailing.domain.Location;
import com.example.ridehailing.domain.OfferStatus;
import com.example.ridehailing.domain.RideStatus;
import com.example.ridehailing.testsupport.TestClockConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;

import static com.example.ridehailing.testsupport.Locations.offsetNorth;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * One offer at a time, so "who gets asked next" is unambiguous rather than depending on how
 * many drivers happened to fit in the first batch.
 */
@SpringBootTest(properties = "ride-hailing.matching.offer-batch-size=1")
@AutoConfigureMockMvc
@Import(TestClockConfig.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class RideRedispatchApiTest extends AbstractRideApiTest {

    private static final Location TEN_KM_AWAY = offsetNorth(PICKUP, 10);

    @Test
    void onlyTheTopRankedDriverIsOfferedFirst() throws Exception {
        String userId = registerUser("+91-100");
        String closer = onlineDriverAt("+91-200", CarType.HATCHBACK, offsetNorth(PICKUP, 0.5));
        String further = onlineDriverAt("+91-201", CarType.HATCHBACK, offsetNorth(PICKUP, 1.5));

        String rideId = createRide(userId, TEN_KM_AWAY, CarType.HATCHBACK, null);
        acceptFare(userId, rideId).andExpect(status().isOk());

        assertThat(offerIdsOf(closer)).hasSize(1);
        assertThat(offerIdsOf(further)).isEmpty();
    }

    @Test
    void aRejectionPassesTheRideToTheNextDriverWhoCanAcceptIt() throws Exception {
        String userId = registerUser("+91-100");
        String closer = onlineDriverAt("+91-200", CarType.HATCHBACK, offsetNorth(PICKUP, 0.5));
        String further = onlineDriverAt("+91-201", CarType.HATCHBACK, offsetNorth(PICKUP, 1.5));

        String rideId = createRide(userId, TEN_KM_AWAY, CarType.HATCHBACK, null);
        acceptFare(userId, rideId).andExpect(status().isOk());

        rejectOffer(closer, soleOfferOf(closer)).andExpect(status().isNoContent());

        assertThat(offerIdsOf(further)).hasSize(1);
        acceptOffer(further, soleOfferOf(further))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DRIVER_ASSIGNED"))
                .andExpect(jsonPath("$.driverId").value(further));
    }

    @Test
    void aRejectedDriverIsNotAskedAboutTheSameRideAgain() throws Exception {
        String userId = registerUser("+91-100");
        String closer = onlineDriverAt("+91-200", CarType.HATCHBACK, offsetNorth(PICKUP, 0.5));
        onlineDriverAt("+91-201", CarType.HATCHBACK, offsetNorth(PICKUP, 1.5));

        String rideId = createRide(userId, TEN_KM_AWAY, CarType.HATCHBACK, null);
        acceptFare(userId, rideId).andExpect(status().isOk());
        rejectOffer(closer, soleOfferOf(closer)).andExpect(status().isNoContent());

        assertThat(offerIdsOf(closer)).isEmpty();
    }

    @Test
    void takingOneRideWithdrawsTheDriversOfferForAnotherAndSendsThatRideBackOut() throws Exception {
        String riderA = registerUser("+91-100");
        String riderB = registerUser("+91-101");
        // The popular driver is nearest to both pickups, so batch-size 1 offers them both rides.
        String popular = onlineDriverAt("+91-200", CarType.HATCHBACK, offsetNorth(PICKUP, 0.3));
        String backup = onlineDriverAt("+91-201", CarType.HATCHBACK, offsetNorth(PICKUP, 1.2));

        String rideA = createRide(riderA, TEN_KM_AWAY, CarType.HATCHBACK, null);
        acceptFare(riderA, rideA).andExpect(status().isOk());
        String rideB = createRide(riderB, TEN_KM_AWAY, CarType.HATCHBACK, null);
        acceptFare(riderB, rideB).andExpect(status().isOk());

        assertThat(offerIdsOf(popular)).hasSize(2);
        String offerForA = offerForRide(popular, rideA);
        String offerForB = offerForRide(popular, rideB);

        acceptOffer(popular, offerForA)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(rideA));

        assertThat(offerStatusOf(offerForB)).isEqualTo(OfferStatus.WITHDRAWN);
        assertThat(rideStatusOf(rideB)).isEqualTo(RideStatus.SEARCHING);
        // Ride B went back out, and the only driver left is the backup.
        assertThat(offerIdsOf(backup)).hasSize(1);
        acceptOffer(backup, soleOfferOf(backup))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(rideB))
                .andExpect(jsonPath("$.driverId").value(backup));
    }

    @Test
    void aRideWithNoRemainingDriversAfterARejectionEndsAsNoDriverFound() throws Exception {
        String userId = registerUser("+91-100");
        String onlyDriver = onlineDriverAt("+91-200", CarType.HATCHBACK, offsetNorth(PICKUP, 0.5));

        String rideId = createRide(userId, TEN_KM_AWAY, CarType.HATCHBACK, null);
        acceptFare(userId, rideId).andExpect(status().isOk());

        rejectOffer(onlyDriver, soleOfferOf(onlyDriver)).andExpect(status().isNoContent());

        assertThat(rideStatusOf(rideId)).isEqualTo(RideStatus.NO_DRIVER_FOUND);
    }

    @Test
    void rejectingAnOfferTwiceConflicts() throws Exception {
        String userId = registerUser("+91-100");
        String driverId = onlineDriverAt("+91-200", CarType.HATCHBACK, offsetNorth(PICKUP, 0.5));
        String rideId = createRide(userId, TEN_KM_AWAY, CarType.HATCHBACK, null);
        acceptFare(userId, rideId).andExpect(status().isOk());
        String offerId = soleOfferOf(driverId);

        rejectOffer(driverId, offerId).andExpect(status().isNoContent());
        rejectOffer(driverId, offerId).andExpect(status().isConflict());
    }

    private String offerForRide(String driverId, String rideId) {
        return offerRepository.findByRideId(rideId).stream()
                .filter(offer -> offer.driverId().equals(driverId))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no offer for ride " + rideId + " to driver " + driverId))
                .id();
    }
}
