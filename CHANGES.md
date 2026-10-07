# Raahi - change log (source-level only: NOT compiled, NOT run)

## Images
All scenic/vehicle/banner/illustration images, the artwork composables and their call sites were removed. Only the Raahi logo remains.

## Backend (Spring Boot, migration V16__helper_job_system.sql)
- Job lifecycle with state machine (JobStateMachine), ARRIVED (GPS and/or OTP), work-done + completion OTP, rating, immutable status history.
- No normal cancellation after ARRIVED; Report Issue endpoint instead.
- Cancellation tracking + escalating blocks (4 abusive cancellations / 30 days -> 1 day, then 7 days, then SUSPENDED), admin can excuse.
- Commission ledger (10% default, configurable), negative balance blocks accepting jobs, admin records settlements.
- Helper onboarding fields + validation, SUSPENDED state, online/offline, helper status/earnings/commission/ratings/history endpoints.
- Admin API (KYC, users, helpers, jobs, reports/disputes, cancellations, ratings, commission, audit logs); audit_logs and ledgers are append-only (DB triggers).
- Earlier P0: RBAC on accept, subscription gate off by default, DB-checked admin.

## Android
- Helper flows rebuilt in the shared peach/orange kit (RaahiPrimaryButton/TextField/ScreenHeader/StatusPill added to RaahiComponents): onboarding wizard + status states, dashboard (online, requests, active, history, earnings/commission, profile), job status for both roles.
- Entry points: Home card (Earn with Raahi / Helper Dashboard by role) and Profile menu.
- Vehicle picker no longer accepts free text; shows "Can't find your vehicle? Request it".
- Auth gate (debug-only bypass), encrypted token storage, MapLibre teardown, nav-arg hardening.
