# Cloudflare Migration Plan (Spark / No Billing)

## 1) Problem Statement

Current blockers when Firebase/GCP billing is disabled:

- Firebase Functions (Gen2) endpoints return 5xx and become unusable.
- Admin panel depends on these endpoints for auth policy + reports + test push.
- Mobile app still depends on backend endpoint for purchase verification.

This document defines a low-risk migration path to keep production running on free tiers.

## Current Status (2026-03-10)

- `workers/admin-api` scaffold is implemented and deployed.
- Live URL: `https://contentapp-admin-api.oaslananka.workers.dev`
- Implemented routes:
  - `POST /healthCheck`
  - `POST /adminAccessCheck`
  - `POST /sendTestNotification`
  - `POST /deviceCoverageReport`
  - `GET /adminGetRemoteConfig`
  - `POST /adminUpdateRemoteConfig`
  - `POST /adminGetFlavorHubSummary`
  - `POST /adminGetAnalyticsSummary`
  - `POST /adminGetRevenueSummary`
  - `POST /adPerformance` (today + manual refresh now generates live AdMob report)
- Implemented schedules on Worker:
  - `0 * * * *` hourly notification dispatch
  - `15 7 * * 1` weekly ad performance generation

## 2) Keep vs Move

Keep on Firebase (no-cost friendly components):

- Firebase Auth
- FCM
- Remote Config
- Crashlytics / Analytics

Move from Firebase Functions to Cloudflare Workers:

- Admin HTTP endpoints
- Public utility endpoints (already partially migrated)
- Scheduled jobs currently running on Cloud Scheduler + Functions

## 3) Endpoint Inventory and Migration Priority

### Already on Cloudflare Worker (`workers/content-api`)

- `GET /api/other-apps`
- `GET /api/audio-manifest`
- `GET /api/audio/:key`
- `POST /api/recaptcha-verify`

### Critical (migrate first)

1. `POST /adminAccessCheck`
2. `POST /healthCheck`
3. `POST /sendTestNotification`
4. `POST /deviceCoverageReport`

Reason: Admin login and day-to-day operations depend on these directly.

### Second wave

5. `GET /adminGetRemoteConfig`
6. `POST /adminUpdateRemoteConfig`
7. `POST /adminGetFlavorHubSummary`
8. `POST /adminGetAnalyticsSummary`
9. `POST /adminGetRevenueSummary`
10. `POST /adPerformance` (manual + today + cached weekly report)

### Third wave

11. `POST /verifyPurchase` (mobile runtime dependency)

## 4) Architecture Options (Free-tier focused)

## Option A: Recommended Hybrid (lowest migration risk)

- Cloudflare Workers for HTTP endpoints + cron orchestration.
- Keep Firebase Auth, Firestore, FCM, Remote Config.
- Workers call Google APIs using service account credentials (secret in Worker).

Pros:

- Minimal Android and admin panel changes.
- Reuses existing Firebase data model.
- Fastest recovery from current outage.

Cons:

- Still coupled to Google APIs (but not Firebase Functions hosting).

## Option B: Supabase-centric

- Move admin auth policy + reports + schedule storage to Supabase.
- Keep only FCM/Remote Config on Firebase (or migrate push provider too).

Pros:

- Cleaner backend ownership in one place.

Cons:

- Higher migration cost and bigger code/data changes.

## Option C: OneSignal for push orchestration

- Offload dispatch scheduling/segmentation to OneSignal.
- Keep Android app + admin panel integration through OneSignal APIs.

Pros:

- Less custom scheduler maintenance.

Cons:

- Vendor migration complexity, payload model changes.

## 5) Implementation Plan (Option A)

## Phase 0 - Safety

1. Freeze Firebase Functions changes.
2. Set admin panel to explicit backend base URL via `VITE_FUNCTIONS_BASE_URL`.
3. Keep current Cloudflare content worker untouched.

## Phase 1 - Admin Core Worker

Create new worker: `side-projects/cloudflare/workers/admin-api`

Initial routes:

- `POST /adminAccessCheck`
- `POST /healthCheck`
- `POST /sendTestNotification`
- `POST /deviceCoverageReport`

Security requirements:

- Validate Firebase ID token (JWT verification against Google certs).
- Enforce admin allowlist from Firestore `admins/{uid}` or env allowlist fallback.
- CORS allow only admin domains.
- Add request rate limits and structured logs.

## Phase 2 - Reporting + Remote Config

Add routes:

- `GET /adminGetRemoteConfig`
- `POST /adminUpdateRemoteConfig`
- `POST /adminGetFlavorHubSummary`
- `POST /adminGetAnalyticsSummary`
- `POST /adminGetRevenueSummary`
- `POST /adPerformance`

Use cached responses (KV) for expensive report calls.

## Phase 3 - Purchase Verification

Add route:

- `POST /verifyPurchase`

Requirements:

- Verify Firebase ID token + App Check token (if enforced).
- Call Google Play Android Publisher API.
- Persist verification result to Firestore collection `purchase_verifications`.

Android change:

- Set `PURCHASE_VERIFICATION_URL` to Worker URL.

## Phase 4 - Cron Migration

Cloudflare Cron triggers:

- Hourly dispatch replacement for `dispatchNotifications`.
- Weekly ad report generation replacement.

Use idempotency markers in Firestore to avoid duplicate sends.

## 6) Required Secrets / Vars Mapping

Current Firebase Functions env -> Cloudflare Worker secrets:

- `ADMIN_ALLOWED_EMAILS`
- `ADMOB_CLIENT_ID`
- `ADMOB_CLIENT_SECRET`
- `ADMOB_REFRESH_TOKEN`
- `ADMOB_PUBLISHER_ID`
- `GOOGLE_RECAPTCHA_SECRET_KEY`
- `PLAY_ANDROID_PUBLISHER_SERVICE_ACCOUNT_JSON`
- (optional) `VERIFY_PURCHASE_REQUIRE_APP_CHECK`

Worker runtime vars:

- `FIREBASE_PROJECT_ID`
- `FIREBASE_WEB_API_KEY` (only if needed for specific REST paths)
- `ALLOWED_ADMIN_ORIGINS`

## 7) Client Change Points

Admin panel (`side-projects/admin-notifications`):

- Keep code as-is, set:
- `VITE_FUNCTIONS_BASE_URL=https://<admin-api-worker-domain>`

Android app:

- `PUSH_REGISTRATION_URL`: currently direct Firestore sender is used (no immediate change if already not using function endpoint).
- `PURCHASE_VERIFICATION_URL`: point to Worker after Phase 3.

## 8) Cutover Checklist

1. Deploy admin-api worker to preview.
2. Run endpoint parity tests against old function contracts.
3. Switch admin panel `VITE_FUNCTIONS_BASE_URL` to worker in production.
4. Verify login -> authorized flow -> report panels -> test push.
5. Switch mobile `PURCHASE_VERIFICATION_URL` after endpoint parity.
6. Disable old Firebase Function routes when stable.

## 9) Rollback Strategy

- Keep old endpoint URLs in config history.
- Use env-only rollback for admin panel and Android URL targets.
- Keep payload contracts unchanged to allow instant fallback.

## 10) Success Criteria

- Admin login works without Firebase Functions billing.
- Test push and coverage reports are operational.
- Purchase verification path runs from worker endpoint.
- Hourly dispatch and weekly report run from Cloudflare cron.
- No user-visible regression in app monetization and notifications.
