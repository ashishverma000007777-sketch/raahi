# Raahi — Canonical API Contract (Phase 1)

Scope: features that actually exist in the Raahi Flutter app today, plus Vehicle/Car
Health (added post-MVP as a real, user-data-driven feature — see "Vehicle & Car Health"
below and STATUS.md's "CAR HEALTH — REAL FEATURE" entry). Fair Price Check / Road Trip /
Vehicle Documents remain excluded — they still have no reference implementation and are
pending a product decision.

Base path: `/api/v1`
Auth: `Authorization: Bearer <JWT>` issued by our backend after Firebase
token verification (Firebase remains the identity provider; our JWT is
what every subsequent request uses — same pattern as today, just enforced
consistently).

All responses follow one envelope, everywhere, no exceptions:

Success: `{ "success": true, "data": { ... } }`
Error:   `{ "success": false, "error": { "code": "STRING_CODE", "message": "..." } }`

## Resolved conflicts (Flutter said X, Node said Y → final answer)

- SOS trigger: Flutter called `POST /sos`, Node expected `POST /sos/trigger` → final: `POST /sos`
- My jobs: Flutter called `GET /jobs/my-jobs`, Node had `GET /jobs/my` → final: `GET /jobs/mine`
- Selfie admin review: Flutter called `GET /selfie/pending`, Node had `GET /admin/selfies/pending` → final: `GET /admin/selfies/pending` (Flutter side gets fixed to match)
- FCM token: Flutter called `PUT /auth/fcm-token`, Node had nothing → final: implemented as `PUT /auth/fcm-token`
- Daily tips: Flutter called `GET /daily/tips`, Node had nothing → final: implemented, real rotating content, clearly presented as non-live
- Nearby places: Flutter called `GET /daily/places`, Node had nothing → final: `GET /places/nearby` via a PlacesProvider abstraction
- Aadhaar: Flutter called `POST /aadhaar/send-otp` + `verify-otp`, Node had nothing → final: OTP dropped entirely (not needed per spec), new `POST /aadhaar/submit` does image upload + AI tamper screen only
- WebSocket auth: both sides trusted `?userId=` in the query string → final: `wss://.../ws?token=<JWT>`, server derives identity from the verified JWT and never trusts the query param
- AI chat session: client-supplied `session_id` was never checked against `user_id` → final: server verifies `session.user_id == authenticated user` on every message, 403 otherwise
- Job accept race: `UPDATE ... WHERE status='pending'` worked but had no row lock → final: wrapped in a transaction with `SELECT ... FOR UPDATE` on the job row
- Mechanics-nearby fallback: backend returned fake mechanics when the DB was empty → final: removed, returns an empty result with "No verified mechanics found nearby"

## Endpoints (final)

**Auth**
- `POST /auth/verify` — body: `{ idToken, requestedRole? }` → verifies Firebase token, creates/loads user, returns `{ token, isNewUser, user, subscription }`. Replaces `verifyOtp` + `verify-firebase` (one canonical endpoint, no legacy alias).
- `GET /auth/me`
- `PUT /auth/profile`
- `PUT /auth/location` — body: `{ lat, lng }`; persists to `users.last_lat/last_lng/last_location_at`
  (fixed during the build-blocker pass — previously validated input and returned
  `{"updated": true}` without storing anything at all). Not currently read by any other
  endpoint or consumed by the Android client — general presence data for future use.
- `PUT /auth/fcm-token` — body: `{ token }`

**Jobs (roadside help marketplace)**
- `POST /jobs` — subscription is NOT required in the MVP (`raahi.subscription.enforce-job-creation=false`). When `RAAHI_SUBSCRIPTION_ENFORCE_JOB_CREATION=true`, DRIVER accounts without an active subscription get 402.
- `GET /jobs`, `POST /jobs/{id}/accept` — approved, ACTIVE HELPER/MECHANIC only (role read from DB); DRIVER/ADMIN get 403
- `GET /jobs` — nearby pending jobs for helpers
- `GET /jobs/mine` — jobs where the caller is *either* the requester or the accepted
  helper (found while wiring Android Home: this originally only matched the requester side,
  so a MECHANIC/HELPER account's own in-progress job never showed up in their own "my jobs" —
  fixed at `JobRepository.findMine`, not worked around client-side)
- `POST /jobs/{id}/accept`
- `POST /jobs/{id}/verify-otp` — body `{ otp }`. New this session, while wiring the Android
  Jobs screens: the old Node backend had no way to actually verify the OTP handoff at all —
  a "matched" job showed the requester an OTP with nothing on the other end checking it, and
  `complete` had no status check, so either participant could mark a job COMPLETED straight
  from "pending"/"matched" with no handoff ever confirmed. Only the assigned helper may call
  this, only from MATCHED, only with the correct OTP → moves to IN_PROGRESS.
- `GET /jobs/{id}`
- `POST /jobs/{id}/complete` — now requires status IN_PROGRESS (i.e. verify-otp must have run
  first); previously callable from any status, which is what let the OTP step be skipped
- `POST /jobs/{id}/en-route` — optional body: `{ lat?, lng? }`. Moves job status from `MATCHED` to `EN_ROUTE`. Only the assigned helper may call this. Records departure GPS and timestamp, activates en-route cancellation protection, and sends a push notification to the requester.
- `POST /jobs/{id}/cancel` — body: `{ reason?, reasonCode?, lat?, lng? }`. Reason code can be `CUSTOMER_CANCELLED`, `HELPER_CANCELLED`, `CUSTOMER_UNREACHABLE`, `INCORRECT_LOCATION`, `VEHICLE_EMERGENCY`, `UNSAFE_CONDITIONS`, or `OTHER`. Allowed from `PENDING`, `MATCHED`, and `EN_ROUTE` stages (returns `409 CANCEL_NOT_ALLOWED` after helper has `ARRIVED`). When cancelled while `EN_ROUTE` or when helper reports `CUSTOMER_UNREACHABLE`/`INCORRECT_LOCATION`, an audit-backed travel loss compensation claim is automatically evaluated and queued for admin review. Customer cancellation fee is strictly ₹0.

JobDto (all endpoints): `id, status, problemType, problemDesc, lat, lng, rewardAmount,
requesterName, helperName, viewerRole` always present. `viewerRole` is `"REQUESTER"` or
`"HELPER"` (whichever the caller actually is on that job), computed server-side rather than
left for the client to guess by matching names — added while building Android's "My Jobs"
list, which mixes jobs from both sides of the marketplace. `helperOtp` only present for the
requester. `helperPhone`/`helperRatingAvg`/`helperTotalHelps`/`helperVerified` only present
once a helper is assigned. `requesterPhone` only present for the assigned helper (mirrors
helperPhone) — added this session; the old Node `GET /jobs/:id` returned *both* parties'
phone numbers to any authenticated caller with no participant check at all, which is not
reproduced here. Additional fields for cancellation protection: `enRouteAt`, `cancellationReasonCode`,
`cancellationStage`, `travelLossStatus` (`ELIGIBLE_PENDING_REVIEW`, `ADMIN_APPROVED`, `REJECTED`, `NOT_ELIGIBLE`),
`payoutStatus` (`UNPAID`, `PENDING_PAYOUT`, `PAID`, `FAILED`), and helper-private details (`travelLossEstimate`,
`travelLossApproved`, `travelDistanceKm`, `travelEvidenceType`, `travelLossNotes`).

**Mechanics**
- `GET /mechanics/nearby?lat&lng&radius` — filtered to `verification_status = APPROVED` with
  a location on file; does NOT also filter by `is_available` — an approved mechanic marked
  unavailable still appears (with `isAvailable: false` so the client can show it), on the
  view that "nearby verified mechanics, however busy" is more useful for a driver deciding
  who to call than silently hiding some of them. Android (Phase 6C) renders availability as
  a green/red marker + badge rather than filtering it further client-side.
- `GET /mechanics/{id}`
- `PUT /mechanics/location`
- No `POST` to request a *specific* mechanic — the job marketplace has no concept of
  targeting one mechanic (any eligible helper accepts a posted job). Android's mechanic
  detail screen's CTA opens the real Request Help flow instead of a fake per-mechanic
  request — the Flutter reference's equivalent button showed a "Request sent!" dialog with
  no backend call behind it at all, not reproduced.

**Vehicle & Car Health** (added post-MVP — real, user-entered data only; see
STATUS.md's "CAR HEALTH — REAL FEATURE" entry for the full design rationale)
- `GET /vehicles/me` — the caller's own vehicle; 404 `VEHICLE_NOT_FOUND` if they haven't
  completed "Set up your car" yet. Android uses this 404 specifically to trigger the setup
  flow, not any other error path.
- `PUT /vehicles/me` — create-or-update (same endpoint for first-time setup and later edits).
  Required: `brand`, `model`, `modelYear`, `fuelType` (`PETROL`/`DIESEL`/`CNG`/`ELECTRIC`/
  `HYBRID`/`LPG`), `registrationNumber`, `odometerKm`. Optional: `variant`,
  `lastServiceDate`/`lastServiceOdometerKm`, `insuranceExpiry`, `pucExpiry`,
  `tyreReplacedDate`/`tyreReplacedOdometerKm`, `batteryReplacedDate`/
  `batteryReplacedOdometerKm` (all dates ISO `yyyy-MM-dd`). Nothing here is collected that
  the backend can't actually persist — no fields beyond this list exist anywhere in the flow.
- `PUT /vehicles/me/odometer` — body `{ odometerKm }`. Rejects a value lower than the current
  reading (`400 ODOMETER_DECREASED`) — there is no correction/rollback flow yet, so a genuine
  correction currently requires direct DB access; flagged as a known gap, not silently allowed.
  Every successful update is also appended to an odometer log table (audit trail, not just an
  overwritten column).
- `GET /vehicles/me/health` — returns `{ score, message, factorsConsidered, factorsTotal,
  maintenanceItems[] }`. `score` is `null` (with `message` explaining why) when none of the 3
  scoring factors (service / insurance / PUC — see `CarHealthService` for the exact
  deterministic formula) have data yet. **Never** returns a fabricated default score.
- `GET /vehicles/me/service-records` — the vehicle's service history, newest first.
- `POST /vehicles/me/service-records` — body `{ serviceDate, odometerKm, serviceType, notes?,
  cost?, workshopName?, partsReplaced? }`. Also advances `vehicles.odometer_km` if the
  record's reading is higher than what's on file (a real reading the user just gave us, not
  an inferred one), and updates the vehicle's "last service" fields when the record's
  `serviceType` is an Oil Change / General Service and it's the most recent one on file.


- `POST /sos` — body `{ lat, lng }` → creates the event, pushes `sos:nearby_alert` to nearby MECHANIC/HELPER accounts individually over the WebSocket (never a broadcast), returns `{ sos, nearbyMechanicsNotified }`
- `GET /sos/active?lat&lng&radius` — MECHANIC/HELPER role only (was open to any authenticated user with no radius filter in the old Node backend, including the triggering user's phone + vehicle reg in every row — fixed; phone number is not returned by this endpoint at all)
- `GET /sos/mine` — the caller's own SOS history
- `POST /sos/{id}/resolve` — ownership-checked via a scoped query (`findByIdAndUserId`), 404s if the event doesn't belong to the caller instead of silently no-op'ing `{success:true}` like the old Node route did

**AI Mechanic**
- `POST /ai/chat` — session ownership enforced
- `GET /ai/sessions`

**Safety / Aadhaar**
- `POST /selfie/submit`, `GET /selfie/status`
- `POST /aadhaar/submit` (front + back image) → `{ result: LIKELY_TAMPERED | LIKELY_NOT_TAMPERED | REVIEW_REQUIRED }`, never claims official authenticity
- `GET /admin/selfies/pending`, `PUT /admin/selfies/{id}/review`
- `GET /admin/aadhaar/pending`, `PUT /admin/aadhaar/{id}/review`

**Daily / Engagement**
- `GET /daily/streak`, `POST /daily/streak/checkin`
- `GET /daily/tips`
- `GET /daily/alerts`, `POST /daily/alerts`, `POST /daily/alerts/{id}/vote` (one vote per user, changeable, not incrementable)
- `GET /places/nearby`
- `GET /fuel-rates`, `GET /fuel-rates/states`

**WebSocket** — `wss://.../ws`, `Authorization: Bearer <JWT>` header on the handshake request
(same header/token every REST call uses — NOT `?userId=` or `?token=` in the query string, which
is what both the old Node backend and this doc's earlier draft assumed; a query-string token
would be logged in server access logs and browser history, so the Android OkHttp client sets it
as a real handshake header instead). Identity is derived by JwtAuthFilter before the WebSocket
handshake handler ever runs — implemented in `AuthHandshakeInterceptor`.
Server events (all sent via targeted `sendToUser`, never a broadcast):
- `job:status_change` — sent to the other participant only, on accept/complete/cancel
- `job:helper_location` — relayed from a `job:location_update` client message, only after
  verifying the sender is actually a participant on that job, to the *other* participant only
- `sos:nearby_alert` — sent individually to each MECHANIC/HELPER found within
  `NOTIFY_RADIUS_KM` (15km) of a new SOS event; payload is `{ sosId, lat, lng }`, no phone number
Client events: `job:location_update` (`{ job_id, lat, lng }`), `driver:ping` (accepted, currently
a no-op — no standalone live-location store exists yet, same as the old Node stub)
Not implemented yet: `job:new_request`, `job:taken` (no server-side trigger point wired for
either; jobs are still discovered by polling `GET /jobs`)

## Still to audit before Phase 2 is "done"
- `admin.js` `/stats` endpoint — fold into final AdminController
- Shop routes — not found in current backend at all despite Flutter having a shop screen; needs its own decision (spec says "never fake it" — likely ships as "coming soon" state)
- Subscription/payment routes — not found in current backend; Razorpay/Cashfree keys mentioned in memory but no route file exists yet

**Helper application (Aadhaar/selfie verification, Phase 7)**
- `POST /helper/apply` (multipart: aadhaarFront, aadhaarBack required; selfie, email optional) — stores each image (Postgres BYTEA), runs AiTamperScreeningProvider (advisory only, defaults to `UNAVAILABLE` — no real AI key configured in this environment, never a fabricated verdict), creates/resubmits a PENDING application. 409 if already PENDING.
- `GET /helper/application/me` — 404 `NO_APPLICATION` if never applied
- `GET /helper/documents/{docId}` — raw image bytes, authorized to the doc's owner or ADMIN only
- `GET /admin/helper-applications` (PENDING queue), `GET /admin/helper-applications/{id}`, `POST .../approve` (sets user role → HELPER), `POST .../reject` (body `{reason}`) — ADMIN role only, read from the verified JWT, never client-supplied. No Android UI for these — admin review is out of scope for the consumer app.
- Never implemented: UIDAI/eKYC, OTP verification, biometric/face matching — per the migration brief's Aadhaar MVP limits.


## Home real-data endpoints (V11)

All are authenticated; identity comes from the JWT, never from the request.

| Endpoint | Returns | Source |
|---|---|---|
| `GET /home/summary` | `weeklyKm{km,since,fullWindow,unavailableReason}`, `helpsGivenThisWeek`, `unreadNotifications` | `vehicle_odometer_logs`, `jobs` (COMPLETED as helper, 7d), `user_notifications` |
| `GET /fuel-log/summary` | `periodDays, fillUps, totalSpent, totalLitres?, lastFilledAt?, hasAnyLogs` | `fuel_logs` (7d) |
| `POST /fuel-log` | created `FuelLogDto`; needs `totalCost` OR (`litres` + `pricePerLitre`); optional `odometerKm` also advances the vehicle odometer (forward only) | `fuel_logs` |
| `GET /fuel-log` | latest fill-ups (`?limit`, max 100) | `fuel_logs` |
| `GET /notifications`, `POST /notifications/read-all` | inbox + mark seen | `user_notifications` (every `NotificationService.send` persists a row) |
| `GET /ai/status` | `state AVAILABLE/UNAVAILABLE`, `available` (= provider credential configured; does not prove reachability) | `AiMechanicProvider.isConfigured()` |
| `GET /fuel-rates` | now also `source`: `ADMIN_MANUAL` or `SEED_INDICATIVE` (seed rows are not live prices) | `fuel_rates.source` |

`weeklyKm.km` is `null` unless the vehicle has at least two real odometer readings. If the vehicle is newer than 7 days, `fullWindow=false` and `since` is the first reading's time.

## Job lifecycle (wire names: PENDING = requested, MATCHED = accepted, EN_ROUTE = helper on the way; COMPLETED + `rated` = rated)
PENDING -> MATCHED -> EN_ROUTE -> ARRIVED -> IN_PROGRESS -> WORK_DONE -> COMPLETED (-> rated). Terminal: CANCELLED, EXPIRED.
- `POST /jobs/{id}/accept` approved+ACTIVE+unblocked helper with no commission due (403 otherwise; 409 if taken)
- `POST /jobs/{id}/en-route` helper signals departure; starts route tracking and en-route cancellation protection
- `POST /jobs/{id}/arrive` body `{lat,lng,otp}`: GPS within `raahi.job.arrival-radius-m` and/or arrival OTP (also allows direct arrival from MATCHED for backward compatibility)
- `POST /jobs/{id}/start` helper; `POST /jobs/{id}/work-done` helper `{finalAmount,paymentMode: CASH|UPI}`
- `POST /jobs/{id}/confirm-completion` customer `{otp}`; creates the commission ledger row
- `POST /jobs/{id}/rate` customer `{stars,comment}`; `POST /jobs/{id}/cancel` allowed in PENDING, MATCHED, or EN_ROUTE (409 CANCEL_NOT_ALLOWED after ARRIVED)
- `POST /jobs/{id}/report` participants (Report Issue / Contact Support); `GET /jobs/{id}/history` immutable status history
## Helper portal
`GET /helper/status`, `PUT /helper/availability`, `GET /helper/commission`, `GET /helper/earnings`, `GET /helper/ratings`, `GET /helper/jobs/history`, `POST /helper/apply` (multipart, full onboarding)
## Admin (`/admin/**`, ROLE_ADMIN + DB re-check, all mutations audited)
overview, users (+block/unblock), helpers (+suspend/reinstate), helper-applications approve/reject, jobs, jobs/{id}/history, reports (+resolve), cancellations (+excuse), cancellation-compensations (review/approve/reject), ratings, commission/balances|ledger|settle, audit-logs
## Commission
Ledger (not a wallet). `raahi.commission.rate` (default 10%). COMMISSION rows negative, SETTLEMENT rows positive; balance < 0 blocks accepting jobs.

## En-Route Cancellation Protection (`/api/v1/jobs/{id}/cancel`, `/api/v1/admin/cancellation-compensations/**`)

Protects roadside helpers from unrecoverable travel loss when a customer cancels after the helper is en route, or when the customer is unreachable at the destination.

### Core Policies
- **Customer Cancellation Fee**: Always **₹0**. Raahi absorbs verified helper travel expenses from a dedicated protection fund; customers are never charged late cancellation penalties.
- **Mandatory Admin Review**: Automatic claim approvals are strictly disabled. All eligible claims are queued with status `ELIGIBLE_PENDING_REVIEW` and `approvedAmount = null`.
- **Payout Separation**: Administrative approval transitions the claim status to `ADMIN_APPROVED`. The payout settlement status remains `UNPAID` (`payout_status IN ('UNPAID', 'PENDING_PAYOUT', 'PAID', 'FAILED')`), preventing automatic or premature wallet balance movements.
- **Duplicate Protection**: Enforced both at application layer and via a database-level `UNIQUE(job_id)` constraint on `job_cancellation_compensations`.
- **Concurrency Guard**: Atomic state transition query guards approval/rejection against concurrent reviews (`409 CONCURRENT_DECISION`).

### Compensation Calculation Formula
`estimatedLossAmount = min(baseCalloutFee + (actualTravelKm + returnDistanceKm) * baseRatePerKm, maxCompensationCap)`
- `baseCalloutFee`: ₹50.0 (baseline dispatch allowance)
- `baseRatePerKm`: ₹12.0 / km
- `returnDistanceFactor`: 0.5 (50% return coverage)
- `minDispatchDistanceKm`: 0.5 km (travel below this distance is marked `NOT_ELIGIBLE`)
- `maxCompensationCap`: ₹500.0 (maximum compensation ceiling per cancellation)
- `evidenceType`: `GPS_VERIFIED` (movement $\ge 50\text{ m}$ between start and cancel coords) or `UNVERIFIED` (ineligible).
- `maxDailyClaimsPerHelper`: 3 claims in 24 hours (exceeding flags the claim note for audit).

### Admin Endpoints
- `GET /api/v1/admin/cancellation-compensations?status=...`: List compensation claims (newest first).
- `GET /api/v1/admin/cancellation-compensations/{id}`: Detailed claim breakdown with coordinates, distances, audit inputs, and party information.
- `POST /api/v1/admin/cancellation-compensations/{id}/approve`: Body `{ approvedAmount?, note? }`. Validates amount ($0 \le \text{amount} \le 50,000$), atomically sets status to `ADMIN_APPROVED`, records audit log. Idempotent. Payout status remains `UNPAID`.
- `POST /api/v1/admin/cancellation-compensations/{id}/reject`: Body `{ note }`. Atomically sets status to `REJECTED`, records audit log. Idempotent.

## Intercity Trips Marketplace (`/api/v1/intercity/**`)

Primary commercial marketplace connecting Travel Agents and Verified Drivers for city-to-city trips.

### Endpoints
- `POST /api/v1/intercity/trips`: Create a new intercity trip. Caller must be authenticated; user role is upgraded to `TRAVEL_AGENT` if not already set. Status defaults to `PUBLISHED` (or `DRAFT` if requested).
- `GET /api/v1/intercity/trips/search`: Search published trips by `pickupCity`, `destCity`, `vehicleCategory`, `fromTime`.
- `GET /api/v1/intercity/trips/{id}`: Detailed trip view. Role-gated: only the trip owner (`AGENT`) sees the OTPs in the response payload. Drivers never receive OTPs via API.
- `GET /api/v1/intercity/trips/agent`: List all trips posted by the authenticated travel agent.
- `GET /api/v1/intercity/trips/driver`: List all trips assigned to the authenticated driver and trips where driver has applied.
- `GET /api/v1/intercity/trips/active`: Get the single active trip (`ASSIGNED`, `ARRIVED`, or `IN_PROGRESS`) for the authenticated user.
- `POST /api/v1/intercity/trips/{id}/apply`: Driver submits application for a published trip.
- `GET /api/v1/intercity/trips/{id}/applications`: List all driver applications for a trip (Trip owner only).
- `POST /api/v1/intercity/trips/{id}/select-driver/{driverId}`: Agent assigns driver atomically. Status moves to `ASSIGNED`, generates a 6-digit pickup OTP, and rejects all other pending applications.
- `POST /api/v1/intercity/trips/{id}/arrive`: Assigned driver notifies arrival at pickup location. Status moves to `ARRIVED`.
- `POST /api/v1/intercity/trips/{id}/verify-pickup-otp`: Assigned driver submits the 6-digit pickup OTP verbally received from agent/passenger. On success, status moves to `IN_PROGRESS` and a 6-digit completion OTP is generated. Max 5 failed attempts allowed before lockout.
- `POST /api/v1/intercity/trips/{id}/verify-completion-otp`: Assigned driver submits the 6-digit completion OTP received at destination. On success, status moves to `COMPLETED`, payment status moves to `PAID`, and driver's `totalHelps` counter is incremented.
- `POST /api/v1/intercity/trips/{id}/cancel`: Cancel trip with reason. Permitted only before trip is `IN_PROGRESS`.
- `POST /api/v1/intercity/trips/{id}/settle`: Agent marks payment as `SETTLED`.

### Status Progression
`DRAFT` -> `PUBLISHED` -> `ASSIGNED` -> `ARRIVED` -> `IN_PROGRESS` -> `COMPLETED`. Terminal: `CANCELLED`.
