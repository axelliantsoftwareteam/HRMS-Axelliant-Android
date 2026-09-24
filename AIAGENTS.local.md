# HRMS Android — local agent instructions

The company-wide rules are in [AIAGENTS.md](AIAGENTS.md) — read that first. This file adds only what
is specific to this repository. Where the two disagree about something generic, AIAGENTS.md wins.

## Where things are

| | |
|---|---|
| Project documentation (product state, roadmap, API contract, release) | [eng-wiki-standards → projects/hrms](https://github.com/axelliantsoftwareteam/eng-wiki-standards/tree/main/projects/hrms), this repository under `android/` |
| Ledger — update in the same commit as your work | [docs/ledger.md](docs/ledger.md) |
| Ticket tracker | GitHub Issues in this repository (`TICKET_TRACKER=github`): commits carry `Refs: #<issue>` or `Refs: none` |
| Backend (not in this repository) | HRIS, Frappe/ERPNext at `https://hris.axelliant.com/api/method/`; source in [Axelliant-HRIS-Custom](https://github.com/axelliantsoftwareteam/Axelliant-HRIS-Custom) |

## Stack

| Layer | Technology |
|---|---|
| App | Kotlin, Android Views with view and data binding, Navigation (safe args), `minSdk 24`, `compileSdk`/`targetSdk 36`, JDK 17 |
| Presentation | Fragments (`screens/`) with ViewModels (`viewmodel/`), LiveData |
| Data | Retrofit + Gson over OkHttp (`network/ApiHandler.kt`), repositories in `repos/`, Koin for DI (`di/`) |
| Auth | MSAL (`res/raw/auth_config_ciam_auth.json`); the Microsoft token is exchanged by `hrms.api.mobile_v1.get_set_user_token` for an HRIS `token <api_key>:<api_secret>` |
| Release | `android-release.yml`: signed AAB to Google Play on every push to `main` and on `android-v*` tags |

## Commands

```bash
bash ./gradlew assembleDebug                                 # debug APK
bash ./gradlew lint testDebugUnitTest assembleDebug          # what CI runs (.axelliant/local-checks.sh)
docker compose up -d mock-hris                               # mock HRIS on http://localhost:8000
./scripts/check-before-push.sh                               # every gate CI runs
```

## Local rules

- **Stay on `hrms.api.mobile_v1.*`**; never go back to `hrms.hr.doctype.employee.*`. Preserve payload
  shapes unless the backend change is coordinated with the iOS and web clients.
- **Translate backend errors to user-safe text**; never show a raw Frappe message or exception.
- **Every push to `main` publishes to Google Play.** Merge to `main` only what is ready to ship.
- `BASE_URL` in `network/ApiHandler.kt` is hard-coded to production; pointing a debug build at the
  mock or a local HRIS is a local edit that must not be committed.
- Signing material (keystores, `google-services.json`, Play service-account JSON) comes from GitHub
  secrets only. `.jks`, `.keystore` and `google-services.json` are ignored; never force-add one.
- The five fragment `onViewCreated` methods over 150 lines are baselined in
  `.axelliant/method-length-baseline-app.txt`. When you change one, split it into named steps.
- Log through a logger that is silent in release builds; no `println` or unguarded `Log.d` on
  request paths (`app/src/main/java`).
