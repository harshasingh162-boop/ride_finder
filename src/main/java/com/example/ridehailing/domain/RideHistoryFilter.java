package com.example.ridehailing.domain;

import java.util.Optional;

/**
 * The buckets a rider or driver can filter their ride history by.
 *
 * <p>The mapping is expressed as one exhaustive switch over {@link RideStatus} rather than as a
 * set per constant, so adding a status (driver cancellation, for instance) fails to compile until
 * somebody decides which bucket it belongs in. That is the whole point: a history filter that
 * silently omits a new status is a bug nobody notices.
 */
public enum RideHistoryFilter {
    ONGOING,
    COMPLETED,
    CANCELLED;

    /** Empty for statuses that never appear in history, i.e. a quote the rider never accepted. */
    public static Optional<RideHistoryFilter> bucketOf(RideStatus status) {
        return switch (status) {
            case QUOTED -> Optional.empty();
            case SEARCHING, DRIVER_ASSIGNED, IN_PROGRESS -> Optional.of(ONGOING);
            case COMPLETED -> Optional.of(RideHistoryFilter.COMPLETED);
            case CANCELLED_BY_USER, NO_DRIVER_FOUND -> Optional.of(CANCELLED);
        };
    }

    /** True when a status belongs in history at all, whatever the bucket. */
    public static boolean isHistorical(RideStatus status) {
        return bucketOf(status).isPresent();
    }

    public boolean matches(RideStatus status) {
        return bucketOf(status).filter(this::equals).isPresent();
    }
}
