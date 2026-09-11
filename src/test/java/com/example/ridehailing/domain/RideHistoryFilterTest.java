package com.example.ridehailing.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.Arrays;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class RideHistoryFilterTest {

    @Test
    void ongoingCoversTheThreeLiveStatuses() {
        assertThat(RideHistoryFilter.bucketOf(RideStatus.SEARCHING)).contains(RideHistoryFilter.ONGOING);
        assertThat(RideHistoryFilter.bucketOf(RideStatus.DRIVER_ASSIGNED)).contains(RideHistoryFilter.ONGOING);
        assertThat(RideHistoryFilter.bucketOf(RideStatus.IN_PROGRESS)).contains(RideHistoryFilter.ONGOING);
    }

    @Test
    void cancelledCoversRiderCancellationAndAFailedSearch() {
        assertThat(RideHistoryFilter.bucketOf(RideStatus.CANCELLED_BY_USER))
                .contains(RideHistoryFilter.CANCELLED);
        assertThat(RideHistoryFilter.bucketOf(RideStatus.NO_DRIVER_FOUND))
                .contains(RideHistoryFilter.CANCELLED);
    }

    @Test
    void completedIsItsOwnBucket() {
        assertThat(RideHistoryFilter.bucketOf(RideStatus.COMPLETED)).contains(RideHistoryFilter.COMPLETED);
    }

    @Test
    void anUnacceptedQuoteIsNotHistoryAtAll() {
        assertThat(RideHistoryFilter.bucketOf(RideStatus.QUOTED)).isEqualTo(Optional.empty());
        assertThat(RideHistoryFilter.isHistorical(RideStatus.QUOTED)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(RideStatus.class)
    void everyStatusExceptQuotedIsBucketedExactlyOnce(RideStatus status) {
        long bucketsClaiming = Arrays.stream(RideHistoryFilter.values())
                .filter(filter -> filter.matches(status))
                .count();

        assertThat(bucketsClaiming).isEqualTo(status == RideStatus.QUOTED ? 0 : 1);
    }
}
