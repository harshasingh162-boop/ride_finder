# Ride Finder

An in-memory Spring Boot ride-hailing backend.

## Run

```bash
./mvnw spring-boot:run
./mvnw test
```

The API is rooted at `http://localhost:8080/api/v1`.

## Assumptions

- Data is in memory; restarting the process loses users, drivers, rides, offers, and coupons.
- A user may have at most one active ride (`SEARCHING`, `DRIVER_ASSIGNED`, or `IN_PROGRESS`).
- A driver must publish a location before becoming `AVAILABLE`.
- Drivers are matched only when they are available, nearby, and their location is not stale.
- Matching may move upward through the configured free-upgrade car-type chain, never downward.
- Fare, surge, and coupon values are calculated at request time and the quoted fare is then fixed on the ride.
- Coupon codes are normalized case-insensitively; deletion is a soft delete.
- Offers are time-limited, are not reused for the same ride, and are withdrawn when a ride or driver is settled elsewhere.
- A driver can accept only one offer at a time; one ride can have only one assigned driver.
- A cancelled search cannot later be accepted, and a ride with no remaining candidates becomes `NO_DRIVER_FOUND`.
- Start and end are explicit driver actions; ending a ride returns the driver to `AVAILABLE`.
- Concurrent registration and matching must preserve uniqueness and cross-object ride/driver invariants.
- This version has no authentication or authorization layer; ownership checks are service-level checks.

## API

| Method | Path | Purpose |
|---|---|---|
| POST | `/users` | Register a user |
| POST | `/drivers` | Register a driver and car |
| PUT | `/drivers/{driverId}/location` | Update driver location |
| PATCH | `/drivers/{driverId}/availability` | Go online or offline |
| GET | `/drivers/{driverId}/rides` | Driver ride history, optionally filtered |
| GET | `/drivers/{driverId}/offers` | List pending offers |
| POST | `/drivers/{driverId}/offers/{offerId}/accept` | Accept an offer |
| POST | `/drivers/{driverId}/offers/{offerId}/reject` | Reject an offer |
| POST | `/drivers/{driverId}/rides/{rideId}/start` | Start an assigned ride |
| POST | `/drivers/{driverId}/rides/{rideId}/end` | Complete an in-progress ride |
| GET | `/users/{userId}/rides` | User ride history, optionally filtered |
| POST | `/users/{userId}/rides` | Request a ride and lock its quote |
| POST | `/users/{userId}/rides/{rideId}/accept-fare` | Accept the quote and begin dispatch |
| POST | `/users/{userId}/rides/{rideId}/cancel-search` | Cancel a searching ride |
| POST | `/matching/estimate` | Calculate a fare estimate |
| GET | `/matching/nearby-drivers` | Find ranked nearby drivers |
| GET | `/rides/{rideId}/tracking` | Read current polling-based tracking data |
| POST | `/admin/coupons` | Create a coupon |
| GET | `/admin/coupons` | List coupons |
| DELETE | `/admin/coupons/{code}` | Deactivate a coupon |
| PUT | `/admin/conditions` | Update rain and traffic conditions |

## Design

### Layers

- `api`: HTTP controllers, request validation, response mapping, and exception handling.
- `service`: use cases and business workflows.
- `domain`: immutable value objects, entities, statuses, and state transitions.
- `repository`: repository contracts and in-memory implementations.
- `pricing`, `discount`, and `matching`: replaceable calculation and selection policies.

### Strategies and extension points

- **Pricing:** `PricingStrategy` currently uses `TieredPricingStrategy`. Add an implementation and select it in `PricingConfig`.
- **Surge:** `SurgeStrategy` implementations (`RainSurgeStrategy`, `TrafficSurgeStrategy`) are injected as a list and composed by `FareCalculator`. Add another Spring bean.
- **Discount:** `DiscountStrategy` is created by `DiscountStrategyFactory`; add a `CouponType`, implementation, and exhaustive factory branch.
- **Matching:** `ProximitySearch` finds eligible drivers, while `DriverMatchingStrategy` ranks them (`NEAREST` or `HIGHEST_RATED`). Add the strategy and wire its enum branch in `MatchingConfig`.
- **Cancellation:** rider cancellation is a guarded `SEARCHING -> CANCELLED_BY_USER` transition in `RideMatchingService`; related offers are withdrawn and no separate cancellation plugin is assumed.

### Ride state machine

```text
QUOTED -> SEARCHING -> DRIVER_ASSIGNED -> IN_PROGRESS -> COMPLETED
             |               |
             +-> CANCELLED   +-> (acceptance conflict)
             +-> NO_DRIVER_FOUND
```

`DRIVER_ASSIGNED` is published only with a non-null driver assignment. `ON_TRIP` is valid only when an active ride points to that driver.

### Accept sequence and concurrency

Acceptance claims the offer, claims the driver (`AVAILABLE -> ON_TRIP`), then atomically claims and publishes the ride assignment. If a later claim fails, earlier claims are rolled back or withdrawn and redispatch is considered. Fare acceptance, cancellation, rejection, acceptance, and redispatch are serialized in the matching service; repository uniqueness uses atomic `putIfAbsent` or synchronization where the invariant spans multiple records. `ConcurrentHashMap` alone cannot make those multi-object business transactions atomic.

## Trade-offs and next steps

- Replace the linear nearby-driver scan with a geohash or S2 index.
- Add secondary indexes for driver status/location, offers by ride/driver, and ride ownership.
- Use SSE or WebSocket tracking instead of polling.
- Support per-area surge and demand/supply-aware pricing.
- Persist state in a transactional database.
- Add idempotency keys for ride requests and driver actions.
- Expire offers with a delay queue or scheduled worker instead of checking lazily.
