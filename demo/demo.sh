#!/usr/bin/env bash
set -euo pipefail

base_url="${BASE_URL:-http://localhost:8080/api/v1}"
pickup_lat=12.9716
pickup_lng=77.5946
drop_lat=12.9816
drop_lng=77.6046
far_lat=19.0760
far_lng=72.8777
json='Content-Type: application/json'

post() { curl -fsS -X POST "$1" -H "$json" -d "$2"; }
put() { curl -fsS -X PUT "$1" -H "$json" -d "$2"; }
patch() { curl -fsS -X PATCH "$1" -H "$json" -d "$2"; }
get() { curl -fsS "$1"; }
pretty() { jq .; }

echo "== 1. Happy path =="
user_id="$(
  post "$base_url/users" \
    '{"name":"Demo Rider","phone":"+91-9000000001"}' | jq -r .id
)"
driver_id="$(
  post "$base_url/drivers" \
    '{"name":"Demo Driver","phone":"+91-9000000001","car":{"plate":"DEMO-HATCH-1","model":"Swift","carType":"HATCHBACK"}}' |
    jq -r .id
)"
put "$base_url/drivers/$driver_id/location" \
  "{\"lat\":$pickup_lat,\"lng\":$pickup_lng}" >/dev/null
patch "$base_url/drivers/$driver_id/availability" '{"online":true}' | pretty

post "$base_url/matching/estimate" \
  "{\"pickup\":{\"lat\":$pickup_lat,\"lng\":$pickup_lng},\"drop\":{\"lat\":$drop_lat,\"lng\":$drop_lng},\"carType\":\"HATCHBACK\"}" |
  pretty
ride_id="$(
  post "$base_url/users/$user_id/rides" \
    "{\"pickup\":{\"lat\":$pickup_lat,\"lng\":$pickup_lng},\"drop\":{\"lat\":$drop_lat,\"lng\":$drop_lng},\"carType\":\"HATCHBACK\"}" |
    jq -r .id
)"
post "$base_url/users/$user_id/rides/$ride_id/accept-fare" | pretty
offer_id="$(get "$base_url/drivers/$driver_id/offers" | jq -r '.[0].id')"
post "$base_url/drivers/$driver_id/offers/$offer_id/accept" | pretty
get "$base_url/rides/$ride_id/tracking" | pretty
post "$base_url/drivers/$driver_id/rides/$ride_id/start" | pretty
end_response="$(post "$base_url/drivers/$driver_id/rides/$ride_id/end")"
echo "$end_response" | pretty
echo "happy-path cost: $(echo "$end_response" | jq -r '.quotedFare.total')"
patch "$base_url/drivers/$driver_id/availability" '{"online":false}' | pretty

echo "== 2a. No driver in radius =="
far_user_id="$(
  post "$base_url/users" \
    '{"name":"No Driver Rider","phone":"+91-9000000002"}' | jq -r .id
)"
far_ride_id="$(
  post "$base_url/users/$far_user_id/rides" \
    "{\"pickup\":{\"lat\":$far_lat,\"lng\":$far_lng},\"drop\":{\"lat\":19.0860,\"lng\":72.8877},\"carType\":\"HATCHBACK\"}" |
    jq -r .id
)"
post "$base_url/users/$far_user_id/rides/$far_ride_id/accept-fare" | pretty

echo "== 2b. Minimum fare =="
minimum_user_id="$(
  post "$base_url/users" \
    '{"name":"Minimum Fare Rider","phone":"+91-9000000003"}' | jq -r .id
)"
post "$base_url/matching/estimate" \
  "{\"pickup\":{\"lat\":$pickup_lat,\"lng\":$pickup_lng},\"drop\":{\"lat\":12.9720,\"lng\":77.5950},\"carType\":\"HATCHBACK\"}" |
  pretty

echo "== 2c. Hatchback request upgraded to sedan =="
sedan_driver_id="$(
  post "$base_url/drivers" \
    '{"name":"Upgrade Driver","phone":"+91-9000000004","car":{"plate":"DEMO-SEDAN-1","model":"Dzire","carType":"SEDAN"}}' |
    jq -r .id
)"
put "$base_url/drivers/$sedan_driver_id/location" \
  "{\"lat\":$pickup_lat,\"lng\":$pickup_lng}" >/dev/null
patch "$base_url/drivers/$sedan_driver_id/availability" '{"online":true}' >/dev/null
upgrade_ride_id="$(
  post "$base_url/users/$minimum_user_id/rides" \
    "{\"pickup\":{\"lat\":$pickup_lat,\"lng\":$pickup_lng},\"drop\":{\"lat\":$drop_lat,\"lng\":$drop_lng},\"carType\":\"HATCHBACK\"}" |
    jq -r .id
)"
post "$base_url/users/$minimum_user_id/rides/$upgrade_ride_id/accept-fare" | pretty

echo "== 2d. Invalid coupon (expected HTTP 400) =="
set +e
curl -sS -o /tmp/ride-finder-invalid-coupon.json -w 'HTTP %{http_code}\n' \
  -X POST "$base_url/matching/estimate" -H "$json" \
  -d "{\"pickup\":{\"lat\":$pickup_lat,\"lng\":$pickup_lng},\"drop\":{\"lat\":$drop_lat,\"lng\":$drop_lng},\"carType\":\"HATCHBACK\",\"couponCode\":\"DOES-NOT-EXIST\"}"
cat /tmp/ride-finder-invalid-coupon.json | jq .
set -e

echo "== 2e. Rain surge =="
put "$base_url/admin/conditions" '{"raining":true,"trafficLevel":"LOW"}' | pretty
post "$base_url/matching/estimate" \
  "{\"pickup\":{\"lat\":$pickup_lat,\"lng\":$pickup_lng},\"drop\":{\"lat\":$drop_lat,\"lng\":$drop_lng},\"carType\":\"HATCHBACK\"}" |
  pretty

echo "== 2f. Cancellation =="
echo "The current API supports cancellation while SEARCHING only; it has no grace-period or fee field."
cancel_user_id="$(
  post "$base_url/users" \
    '{"name":"Cancellation Rider","phone":"+91-9000000005"}' | jq -r .id
)"
cancel_ride_id="$(
  post "$base_url/users/$cancel_user_id/rides" \
    "{\"pickup\":{\"lat\":$far_lat,\"lng\":$far_lng},\"drop\":{\"lat\":19.0860,\"lng\":72.8877},\"carType\":\"HATCHBACK\"}" |
    jq -r .id
)"
post "$base_url/users/$cancel_user_id/rides/$cancel_ride_id/accept-fare" | pretty
post "$base_url/users/$cancel_user_id/rides/$cancel_ride_id/cancel-search" | pretty

echo "== 3. History =="
get "$base_url/users/$user_id/rides" | pretty
get "$base_url/drivers/$driver_id/rides" | pretty
