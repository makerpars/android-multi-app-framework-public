# Cloudflare Admin API Worker

Phase 1 replacement for Firebase Functions endpoints that block when billing is disabled.

Current routes:

- `POST /adminAccessCheck`
- `POST /healthCheck` (also accepts `GET /health`)
- `POST /sendTestNotification`
- `POST /deviceCoverageReport`
- `GET /adminGetRemoteConfig`
- `POST /adminUpdateRemoteConfig`
- `POST /adminGetFlavorHubSummary`
- `POST /adminGetAnalyticsSummary`
- `POST /adminGetRevenueSummary`
- `POST /adPerformance`

Cron jobs (Cloudflare Scheduler):

- `0 * * * *` → `dispatchNotifications` (hourly)
- `15 7 * * 1` → weekly AdMob ad performance generation/persistence

## Local dev

```bash
npm install
npm run dev
```

## Required secrets/vars

Required:

- `FIREBASE_PROJECT_ID` (wrangler var)
- `FIREBASE_WEB_API_KEY` (wrangler secret)

Optional but recommended:

- `FIREBASE_SERVICE_ACCOUNT_JSON` (wrangler secret, enables Firestore `/admins/{uid}` lookup + allowlist upsert)
- `ADMIN_ALLOWED_EMAILS` (wrangler var)
- `ALLOWED_ADMIN_ORIGINS` (wrangler var)
- `ADMOB_PUBLISHER_ID` (wrangler var, optional preferred account)
- `AD_HEALTH_MIN_REQUESTS` (wrangler var)
- `AD_HEALTH_FILL_RATE_THRESHOLD` (wrangler var)
- `AD_HEALTH_SHOW_RATE_THRESHOLD` (wrangler var)
- `ADMOB_CLIENT_ID` (wrangler secret)
- `ADMOB_CLIENT_SECRET` (wrangler secret)
- `ADMOB_REFRESH_TOKEN` (wrangler secret)

Secret commands:

```bash
npx wrangler secret put FIREBASE_WEB_API_KEY
npx wrangler secret put FIREBASE_SERVICE_ACCOUNT_JSON
npx wrangler secret put ADMOB_CLIENT_ID
npx wrangler secret put ADMOB_CLIENT_SECRET
npx wrangler secret put ADMOB_REFRESH_TOKEN
```

## Deploy

```bash
npx wrangler deploy
```
