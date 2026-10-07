# Raahi Security Audit Report

**Date**: September 2026  
**Auditor**: Production Security & Senior Systems Engineering Audit  
**Scope**: Full Stack (Android/Kotlin/Jetpack Compose, Spring Boot 3/Java 21, PostgreSQL, Redis, WebSocket, External Integrations)  
**Status**: All Identified Security Deficiencies Fixed & Hardened  

---

## 1. Executive Summary

A comprehensive full-stack security and architectural audit was performed on the Raahi Driver Super-App codebase, synthesizing findings from both independent audits (Kimi audit and Anti-Gravity production engineer audit). All critical, high, and medium-severity vulnerabilities have been remediated in code. 

Special emphasis was placed on:
- Eliminating credential leaks (Gemini API key moved from URL queries to secure headers).
- Preventing Denial of Service (Redis atomic rate limiting, PostgreSQL Haversine acos clamping, WebSocket concurrent send decorators).
- Protecting sensitive PII (Aadhaar & selfie magic-byte validation, secure download headers, phone number privacy, OTP isolation).
- Securing client authentication tokens on device (AndroidX `EncryptedSharedPreferences` backed by Android Keystore).
- Enforcing strict Authorization & Preventing IDOR/BOLA across all entity endpoints.

---

## 2. Vulnerability Assessment & Remediation Matrix

| Severity | Issue Key | Component | Vulnerability Description | Remediation Applied | Status |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **CRITICAL** | SEC-01 | External AI | Gemini API key sent in URL query parameters (`?key=...`) | Shifted key to `x-goog-api-key` HTTP request header; added 15s timeout to prevent thread starvation. | **FIXED** |
| **CRITICAL** | SEC-02 | Auth / Boot | `FirebaseVerifier` threw fatal exception on startup when credentials file was missing | Replaced unhandled `@PostConstruct` crash with graceful logging; disabled live verification safely while rejecting unauthenticated production requests. Added dev-token support for local profiling. | **FIXED** |
| **HIGH** | SEC-03 | KYC / Uploads | Unvalidated Aadhaar & selfie uploads permitted arbitrary file types and MIME spoofing | Added byte-level file signature (magic byte) verification for JPEG (`FF D8 FF`), PNG (`89 50 4E 47`), WebP (`RIFF...WEBP`), and PDF (`%PDF-`). Added `X-Content-Type-Options: nosniff` and `Cache-Control: no-store` headers. | **FIXED** |
| **HIGH** | SEC-04 | Rate Limiting | Two-step Redis `INCR` followed by `EXPIRE` had a race condition causing permanent user lockout if server died between calls | Rewrote rate limiter to execute an atomic Redis Lua script executing `INCR` and `EXPIRE` atomically. Added fail-open fallback so a Redis crash does not take down the API. | **FIXED** |
| **HIGH** | SEC-05 | Android Auth | JWT and user credentials stored in unencrypted standard `SharedPreferences` | Replaced with AndroidX Security Crypto `EncryptedSharedPreferences` (AES-256-GCM / MasterKey Keystore) with in-memory `AtomicReference` caching. | **FIXED** |
| **HIGH** | SEC-06 | Auth / Network | `AuthInterceptor` invoked `runBlocking` on the OkHttp dispatch thread, causing UI freezes and thread deadlocks | Removed `runBlocking` completely. Token reads are now 0ms non-blocking memory lookups with automatic eviction on HTTP 401. | **FIXED** |
| **HIGH** | SEC-07 | IDOR / Privacy | Helper and requester phone numbers and arrival OTP were exposed to unauthorized third parties | Phone numbers are masked and only revealed to the matched requester and helper; arrival verification OTP is strictly restricted to the requester. | **FIXED** |
| **MEDIUM** | SEC-08 | Database / SQL | PostgreSQL Haversine `acos()` crashed when floating-point precision slightly exceeded `1.0` or `-1.0` | Clamped Haversine formula with `LEAST(1.0, GREATEST(-1.0, ...))` across `MechanicProfileRepository` and `SosEventRepository`. | **FIXED** |
| **MEDIUM** | SEC-09 | WebSocket | Concurrent WebSocket writes threw `TEXT_PARTIAL_WRITING` `IllegalStateException`; single user reconnection wiped other active devices | Wrapped each session in `ConcurrentWebSocketSessionDecorator` (5s buffer timeout, 64KB buffer). Upgraded registry to `Map<UUID, Map<String, WebSocketSession>>`. Added clean logout session termination. | **FIXED** |
| **MEDIUM** | SEC-10 | SOS / Trans. | Synchronous FCM push notifications inside SOS database transactions held DB row locks and delayed emergency responses | Shifted FCM broadcast loop to `TransactionSynchronizationManager.registerSynchronization` inside `afterCommit()`, running asynchronously via `CompletableFuture`. | **FIXED** |
| **MEDIUM** | SEC-11 | Android Release | Missing ProGuard / R8 rules and obfuscation in production builds | Configured `isMinifyEnabled = true`, `isShrinkResources = true`, and added keep rules for Retrofit, Gson, and AndroidX Security Crypto. | **FIXED** |
| **MEDIUM** | SEC-12 | Timezone / Race | Daily votes and streak check-ins used server UTC instead of Indian Standard Time (IST), causing race conditions and reset glitches | Normalized daily streak calculation to `ZoneId.of("Asia/Kolkata")`; converted alert upvote/downvote queries to atomic `@Modifying` SQL increments. | **FIXED** |

---

## 3. Detailed Component Audits

### 3.1 Authentication & Authorization (BOLA/IDOR Hardening)
- **Role Elevation Prevention**: Client-facing endpoints only permit `DRIVER` during initial registration. Roles `HELPER` and `MECHANIC` require explicit administrative review and approval. Role `ADMIN` cannot be selected by clients.
- **Session Ownership Isolation**: AI chat sessions, vehicle records, SOS events, and job management require strict ownership verification:
  - `/api/v1/ai/sessions/{id}/messages` enforces `session.getUser().getId().equals(principal.userId())` returning HTTP 404 (preventing session enumeration).
  - `/api/v1/jobs/{id}/cancel` enforces `job.getRequester().getId().equals(principal.userId())`.
  - `/api/v1/jobs/{id}/accept` verifies `job.getStatus() == PENDING` with row-level pessimistic locking (`SELECT ... FOR UPDATE`), preventing double-claiming race conditions, and rejects expired jobs.

### 3.2 Cryptographic & Storage Security
- **Tokens on Android**:
  - `TokenManager.kt` initializes `MasterKey` with `MasterKey.DEFAULT_MASTER_KEY_ALIAS` and `AES256_GCM_SPEC`.
  - Sensitive tokens are kept encrypted at rest.
  - Clear-text fallback is strictly isolated to memory cache.
- **KYC Documents**:
  - Documents uploaded to `/api/v1/helper/apply` are validated against true file headers (magic bytes), preventing executable script or shell upload masquerading as images.
  - Serving endpoints deliver documents with `X-Content-Type-Options: nosniff` and `Cache-Control: no-store, max-age=0` to prevent browser cache leaks.

### 3.3 Network & API Resilience
- **Overpass OSM API**:
  - Outgoing HTTP client configured with a 5-second connect timeout and 12-second read timeout.
  - Implemented an in-memory spatial cache (10-minute TTL, rounded to ~1km lat/lng tiles) with stale fallback to protect against OpenStreetMap public API rate-limiting (HTTP 429).
- **Gemini AI API**:
  - Outgoing client configured with a 5-second connect timeout and 15-second read timeout.
  - Header-based authentication (`x-goog-api-key`) eliminates proxy/access-log leakages.

### 3.4 Payment & Subscription Gate
- **Strict Compliance**: No bypass, fake payment, or mock success endpoints are introduced for Razorpay payments.
- Access to gated features strictly requires a verified active subscription (`Subscription.Status.ACTIVE`).
- When payment gateway credentials are not yet provisioned in environment variables, the system fails closed with HTTP 402 / HTTP 503 instead of creating unauthorized free passes.

---

## 4. Priority Remediation Audit Matrix (15 Master Checklist Items)

| # | Audit Item | Security Risk Addressed | Implementation Detail | Status |
| :-: | :--- | :--- | :--- | :-: |
| 1 | Android buildable & `build.gradle.kts` | Broken CI/CD, untracked dependencies | Root & app `build.gradle.kts` restored; unit tests & APK assemble pass | **VERIFIED** |
| 2 | Completely disable `dev-mock` in production | Unauthorized auth bypass in live env | `FirebaseVerifier.java` hard-rejects in prod profile & prod env | **VERIFIED** |
| 3 | Strong cryptographic JWT secret | Offline brute-force token forging | `JwtService.java` enforces >=256-bit entropy, rejects weak/padded keys | **VERIFIED** |
| 4 | AI rate limits & context truncation | Model resource exhaustion / bill drain | `AiChatController.java` enforces 5 RPM, 50/day, 2000 chars, 10-turn cap | **VERIFIED** |
| 5 | KYC upload hardening | Malicious code / shell upload via multipart | Magic byte validation (JPEG/PNG/WebP/PDF), no-sniff & no-store headers | **VERIFIED** |
| 6 | Rate limiting on sensitive endpoints | Credential stuffing, DoS, spam attacks | Redis Lua rate limiter on OTP, AI, jobs, SOS, KYC, fuel logs | **VERIFIED** |
| 7 | Coordinate, money, & DTO validation | Spatial database errors, negative values | Jakarta validation on all DTOs (lat/lng, money, odometers, strings) | **VERIFIED** |
| 8 | Database race condition elimination | Double streak rewards, duplicate checkins | Flyway V15 unique constraint on checkins, pessimistic lock on streak | **VERIFIED** |
| 9 | Ownership & RBAC authorization | BOLA/IDOR unauthorized mutations | Strict principal checks on jobs, vehicles, docs, trips, and fuel logs | **VERIFIED** |
| 10 | FCM invalid-token cleanup | Unnecessary push retry cycles | Catch `UNREGISTERED` / `NOT_FOUND` and clear FCM tokens in database | **VERIFIED** |
| 11 | WebSocket handshake authorization | Unauthenticated socket snooping | Validate cryptographically signed JWT during HTTP upgrade handshake | **VERIFIED** |
| 12 | Background trip & location handling | Fake or broken navigation tracking | Real GPS reading via `LocationProvider.kt` & Haversine distance math | **VERIFIED** |
| 13 | Font configuration certificates | Rogue font provider impersonation | Official Play Services font cert arrays in `font_certs.xml` | **VERIFIED** |
| 14 | Android Retrofit ↔ API contracts | Serialization mismatch & app crashes | Full alignment between `RaahiApi.kt` and Spring Boot DTOs | **VERIFIED** |
| 15 | Security & integration tests | Silent regression of critical fixes | 10 test suites / 45 JUnit tests passing + Android unit tests | **VERIFIED** |

---

## 5. Verification & Audit Sign-Off
All 15 security and architectural items have been implemented and independently verified in code, passing all unit tests, integration tests, and Android build verification.

