# Raahi Remaining Items & Environment Configuration Guide

**Date**: September 2026  
**Document Purpose**: Transparently outline items requiring external cloud credentials, environment deployment settings, or future platform-level capabilities.

---

## 1. External Third-Party Credentials (Production Provisioning)

In accordance with strict production guidelines, no fake mock credentials, hardcoded secrets, or payment bypasses were introduced into the code. The following environment variables must be provisioned in your production/staging `.env` or deployment orchestration:

### 1.1 Razorpay Payment Gateway
- **Variables**: `RAZORPAY_KEY_ID`, `RAZORPAY_KEY_SECRET`
- **Behavior**:
  - The payment flow strictly enforces real subscription validation (`Subscription.Status.ACTIVE`).
  - Gated driver features will return HTTP 402 `PAYMENT_REQUIRED` until a subscription is activated through the real gateway.
  - No fake payment passes or mock bypasses are permitted in production.

### 1.2 Firebase Admin SDK
- **Variables**: `FIREBASE_CREDENTIALS_PATH`
- **Behavior**:
  - The server no longer crashes on startup if this path is absent or points to a non-existent file.
  - To enable production Firebase Phone Auth OTP verification and live FCM push notifications to Android devices, mount a valid Google Service Account JSON file and set this path.
  - In local development/testing, tokens formatted as `dev-mock:<uid>:<phone>` are accepted for integration testing.

### 1.3 Google Gemini AI
- **Variables**: `GEMINI_API_KEY`
- **Behavior**:
  - Used by the AI Mechanic diagnostic assistant.
  - The key is transmitted via the secure `x-goog-api-key` header with connect and read timeouts.
  - When the key is missing, `/api/v1/ai/status` gracefully reports `state: "UNAVAILABLE"` without throwing unhandled server exceptions.

---

## 2. Architecture & Platform Considerations

### 2.1 Background Location Tracking (Android Foreground Service)
- **Current Implementation**:
  - Android client requests `ACCESS_FINE_LOCATION` and `ACCESS_COARSE_LOCATION`.
  - GPS updates are collected via `LocationProvider.kt` with an 8-second timeout and fallback to `fusedLocationClient.lastLocation` for emergency SOS triggers.
- **Future Enhancement**:
  - For continuous multi-hour trip tracking while the app is backgrounded or the screen is locked, Android 10+ (API 29+) requires a dedicated Android `Service` declared with `android:foregroundServiceType="location"` and a persistent ongoing notification. This can be integrated when continuous background telemetry is enabled.

### 2.2 Vehicle Schema Multiplicity
- **Current Design**:
  - The current schema maintains a 1-to-1 relationship between `users` and `vehicles` (with `vehicle_type` and `vehicle_reg` stored on the user row, and vehicle maintenance stats in `vehicles`).
  - As instructed, this architecture was preserved to maintain complete compatibility with the current UI screens and business logic without introducing schema churn.
  - If a future requirement demands fleet management (multiple vehicles per driver), a many-to-one migration can be executed via a dedicated Flyway script.

### 2.3 PostGIS Spatial Upgrade Path
- **Current Design**:
  - Spatial queries for nearby mechanics and SOS alerts use a mathematical Haversine spherical formula in SQL, hardened with `LEAST(1.0, GREATEST(-1.0, ...))` clamping.
  - This provides optimal performance for up to ~5,000 active providers.
  - For enterprise scale (>50,000 providers), enabling the PostgreSQL `postgis` extension with spatial GiST indexing (`ST_DWithin`) is documented in `docs/STATUS.md` as the long-term scale path.
