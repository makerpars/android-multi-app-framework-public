# Release (Local) - Standard Akis

Bu repo'da release cikarmak icin hedef:

- Yanlislikla `Debug` yerine `Release` build almamak
- Signing + Crashlytics mapping upload + (opsiyonel) Play publish adimlarini tek komutla kosmak
- Cikti yollarini (AAB/APK) net gostermek

## Gereken Ortam Degiskenleri

Signing icin (iki yoldan biri):

- `KEYSTORE_FILE` = `C:\path\to\release.jks` (dosya yolu)
  - `KEYSTORE_PASSWORD`
  - `KEY_ALIAS`
  - `KEY_PASSWORD`

veya

- `KEYSTORE_BASE64` (release.jks base64)
  - `KEYSTORE_PASSWORD`
  - `KEY_ALIAS`
  - `KEY_PASSWORD`

Play'a publish icin:

- `PLAY_SERVICE_ACCOUNT_JSON`
  - Dosya yolu (onerilen): `C:\secure\service-account.json`
  - veya JSON icerigi (script gecici dosyaya yazar)
- `FIREBASE_WEB_CLIENT_ID`
  - Google Sign-In web OAuth client id (`*.apps.googleusercontent.com`)
  - publish/internal preflight'ta zorunlu cross-check icin kullanilir

Opsiyonel Firebase override (CI ile ayni model):

- `FIREBASE_CONFIGS_ZIP_BASE64` (zip base64) + `-OverrideFirebaseFromZipBase64`

## CI Bootstrap Akisi

Release ve publish workflow'lari artik tekrar eden shell bloklari yerine ortak composite action'lar kullanir:

1. `resolve-release-secrets`
   - signing env'lerini cozer
   - publish icin gerekli temel secret'lari normalize eder
2. `decode-play-service-account`
   - Play service account'i decode eder
   - JSON yapisini dogrular
   - `PLAY_SERVICE_ACCOUNT_JSON` yolunu export eder
3. `verify-google-signin-config`
   - `FIREBASE_WEB_CLIENT_ID` ile flavor `google-services.json` uyumunu kontrol eder
4. `export-firebase-override-env`
   - gerekiyorsa Firebase Web / R2 override env'lerini standardize eder

Bu sayede `release.yml` ve `release-parallel.yml` icinde ayni bootstrap mantigi paylasilir ve drift azalir.

## Fail-fast Dogrulama

Release/publish akislarinda `app/build.gradle.kts` icinde `validateReleaseConfig` gorevi zorunlu preflight olarak baglidir.

Bu dogrulama su alanlari kontrol eder:

- Signing (dosya tabanli): `KEYSTORE_FILE` mevcut + dosya var
- Signing (secretlar): `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD` bos degil
- Push registration: `PUSH_REGISTRATION_URL` bos degil ve `https://` ile basliyor
- Publish gorevleri icin ek olarak: `PLAY_SERVICE_ACCOUNT_JSON` mevcut + dosya var
- Publish/internal preflight icin: `FIREBASE_WEB_CLIENT_ID` bos olamaz

Notlar:

- `assemble*Release`, `bundle*Release`, `publish*Release*` tasklari fail-fast dogrulamadan gecer.
- `Debug` tasklari (`assembleDebug`, debug build config tasklari vb.) bu kontrolden etkilenmez.
- Runtime taraftaki bos URL guard'i (`HttpPushRegistrationSender`) korunur; bu kontrol build-time guvence saglar.

## On Dogrulama Komutlari

Release oncesi tavsiye edilen minimum preflight:

```powershell
./gradlew :app:validateFlavorVersions
./gradlew :app:validateReleaseConfig
```

Not:

- `validateReleaseConfig` release/publish secret/env durumunu dogrular.
- `validateFlavorVersions` flavor bazli version tanimlarini kontrol eder.

## Komutlar

### 1) Sadece AAB (bundleRelease)

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\release.ps1 -Flavor amenerrasulu -Action bundle -NoDaemon -LoadFromDotEnv
```

### 2) Patch bump + AAB

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\release.ps1 -Flavor amenerrasulu -Bump patch -Action bundle -NoDaemon -LoadFromDotEnv
```

### 3) Play'a publish (Gradle Play Publisher)

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\release.ps1 -Flavor amenerrasulu -Action publish -NoDaemon -LoadFromDotEnv
```

> Not: `app/build.gradle.kts` icindeki `play { track = "internal" }` ayarini kullaniyor.

### 4) Tag olustur (opsiyonel)

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\release.ps1 -Flavor amenerrasulu -Action bundle -CreateTag
```

Tag formati: `v<major>.<minor>.<patch>+<build>` (ornek: `v1.0.0+12`)

## Ciktilar

- AAB: `app/build/outputs/bundle/<flavor>Release/`
- APK: `app/build/outputs/apk/<flavor>/release/`

## Ek QA/CI Referanslari

- Manuel regresyon checklist: `docs/ADS_QURAN_RELEASE_QA_CHECKLIST.md`
- CI release dogrulama plani (workflow patchsiz): `docs/CI_RELEASE_VALIDATION_PLAN.md`

## GitHub Actions: Paralel Publish Stratejisi

Mevcut `Release` workflow'u korunmustur ve publish tarafinda global seri calisma davranisi devam eder.

Yeni eklenen workflow:

- `.github/workflows/release-parallel.yml`

Ne saglar:

- Farkli flavor'lari ayri runner job'larinda ayni anda publish edebilir.
- Ayni flavor + ayni track icin eszamanli publish'i engeller (job-level concurrency lock).
- Boylece:
  - `amenerrasulu` ve `ayetelkursi` ayni anda publish olabilir
  - ama iki ayri run'da `amenerrasulu + production` ayni anda publish olamaz

Workflow inputlari:

- `target_flavors`: `all` veya virgulle ayrilmis flavor listesi
- `publish_track`: `production` veya `internal`
- `update_play_listing`: listing update ac/kapa
- `max_parallel`: ayni anda calisacak max flavor job sayisi (ornek: `16`)

Onerilen kullanim:

1. Coklu ve bagimsiz flavor publish icin `Release Parallel Publish`.
2. Tek flavor veya daha konservatif akis icin mevcut `Release`.
