# Environment Secret Contract

Canonical secret storage is GitHub Actions repository or environment secrets.

Local development uses `.env` copied from `.env.template`. Local values are never committed.

CI/CD workflows read required values from `${{ secrets.* }}` and validate the committed contract with `scripts/ci/verify_env_contract.sh`.

## Required release/publish secrets

- `KEYSTORE_BASE64`
- `KEYSTORE_PASSWORD`
- `KEY_ALIAS`
- `KEY_PASSWORD`
- `PLAY_SERVICE_ACCOUNT_JSON_BASE64`
- `PUSH_REGISTRATION_URL`
- `PURCHASE_VERIFICATION_URL`
- `FIREBASE_WEB_CLIENT_ID`
- `FIREBASE_CONFIGS_ZIP_BASE64`
- `CF_R2_ACCOUNT_ID`
- `CF_API_TOKEN`
- `CF_R2_BUCKET`
- `CF_R2_FIREBASE_OBJECT`
- `CLOUDFLARE_ACCOUNT_ID`
- `CLOUDFLARE_API_TOKEN`
- `ADMIN_ALLOWED_EMAILS`
- `ADMOB_CLIENT_ID`
- `ADMOB_CLIENT_SECRET`
- `ADMOB_REFRESH_TOKEN`
- `ADMOB_PUBLISHER_ID`

## Validation

Run:

```bash
bash scripts/ci/verify_env_contract.sh
```

Windows PowerShell:

```powershell
bash scripts/ci/verify_env_contract.sh
```
