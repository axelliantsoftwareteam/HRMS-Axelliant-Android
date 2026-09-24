# HRMS Axelliant Android

The Android app for Axelliant HRIS (package `com.axelliant.hris`): employees check in and out,
request leave, submit expenses and document requests and log resource hours; managers approve their
team's requests. Kotlin with Android Views and MSAL for Microsoft sign-in, calling the HRIS
(Frappe/ERPNext) `hrms.api.mobile_v1.*` methods at `https://hris.axelliant.com/api/method/`.

**Documentation:** [eng-wiki-standards → projects/hrms](https://github.com/axelliantsoftwareteam/eng-wiki-standards/tree/main/projects/hrms)
(product state and roadmap in [android/overview.md](https://github.com/axelliantsoftwareteam/eng-wiki-standards/blob/main/projects/hrms/android/overview.md),
the API in [hris-api-contract.md](https://github.com/axelliantsoftwareteam/eng-wiki-standards/blob/main/projects/hrms/hris-api-contract.md), build and
release in [android/release.md](https://github.com/axelliantsoftwareteam/eng-wiki-standards/blob/main/projects/hrms/android/release.md)) ·
**Agent instructions:** [AIAGENTS.md](AIAGENTS.md) + [AIAGENTS.local.md](AIAGENTS.local.md) ·
**Ledger:** [docs/ledger.md](docs/ledger.md)

## Get running

Prerequisites: JDK 17, Android Studio (Hedgehog or newer), the Android SDK for `compileSdk 36`.

```bash
./scripts/install-git-hooks.sh        # once per clone
bash ./gradlew assembleDebug          # or open the project in Android Studio and run "app"
```

The debug build talks to the live HRIS. To work against a local stand-in instead, run the mock HRIS
(`docker compose up -d mock-hris`, on port 8000) and point `BASE_URL` in
`app/src/main/java/com/axelliant/hris/network/ApiHandler.kt` at `http://10.0.2.2:8000/api/method/`
(the host, seen from the emulator). Never commit that change. See [mock-hris/README.md](mock-hris/README.md).

## Before you push

```bash
./scripts/check-before-push.sh        # every gate CI runs, then .axelliant/local-checks.sh
```

`.axelliant/local-checks.sh` runs `bash ./gradlew lint testDebugUnitTest assembleDebug`, as CI does.

## Releasing

Every push to `main`, and every `android-v*` tag, builds the signed bundle and uploads it to
Google Play through `.github/workflows/android-release.yml`. Signing material comes from GitHub
secrets only; never commit a keystore or `google-services.json`.
