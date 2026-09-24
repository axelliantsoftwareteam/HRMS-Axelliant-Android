# Ledger

The single record of what exists in this repository, what is in progress and what is pending.
**Update it in the same commit as the work it describes.** Never start a second ledger.

## Built

| Date | What | Why |
|---|---|---|
| 2024-05 – 2024-08 | The app: Microsoft sign-in (MSAL), dashboard, check-in and attendance requests, leave, expenses, approvals; first Play releases | Mobile HR self-service |
| 2024-10 – 2024-12 | Filters and edit on requests, team attendance and statistics for managers, in-app updates, location display, document requests, resource management (GitHub issues #26–#42, #55–#58) | Issue backlog |
| 2026-03 | Moved to the versioned `hrms.api.mobile_v1` contract, hardened payload handling and error messages | Shared contract with iOS and web |
| 2026-04 | `android-release.yml`: signed APK/AAB built and uploaded to Google Play | Automated releases |
| 2026-09-24 | Engineering standards adopted: agent instructions, hooks, gates and workflows; docs moved to the wiki (`projects/hrms/android`, `hris-api-contract.md`); CI through `.axelliant/local-checks.sh` with the `axelliant-gate` job; mock HRIS in `compose.yaml`; tracked keystore and local Gradle home removed from the tree | Axelliant standards |

## In progress

| Since | What |
|---|---|
| 2026-07 | On `staging`, not yet in `main`: team leave quota view, Fluent UI refresh and payslip support. Open PR #76 (subscription UI) targets `main` |

## Pending

| What | Why |
|---|---|
| Apply the GitHub rulesets (`scripts/apply-github-ruleset.sh`) once the guardrail jobs are green, and decide the integration branch (`uat`, or retire `staging`/`stage`) | Branch protection; `staging` holds unmerged work |

## Standards debt

| File / area | Rule broken | Size | Found |
|---|---|---|---|
| Git history | Security: `erp_key_store.jks` and `hrms_key_store.jks` (signing keystores) are in the history of this public repository; rotate or reset the upload key | S (rotation) | 2026-09-24 |
| Git history | Hygiene: `.gradle-user/` (272 MB, a 134 MB Gradle distribution in LFS) stays in history and every clone; rewriting history is a team decision | S | 2026-09-24 |
| Build | Code quality: no ktlint, detekt or Kover; no coverage report, so no coverage floor. Unit tests are three files (two template examples, `ExtentionsTest`) | M | 2026-09-24 |
| `screens/` | Code shape: five fragment `onViewCreated` methods over 150 lines (HomeFragment 326, AddExpenseFragment 293, RequestFragment 270, ApprovalsFragment 204, AddResourceManageFragment 191; baselined) | L | 2026-09-24 |
| `app/src/main/java` | Logging: 235 `Log.*`/`println` calls, not silenced in release builds | M | 2026-09-24 |
| `network/ApiHandler.kt`, `AndroidManifest.xml`, `app/build.gradle.kts` | Mobile security: base URL hard-coded (a local IP left in a comment), `usesCleartextTraffic="true"`, release builds not minified (`isMinifyEnabled = false`) | S | 2026-09-24 |
| `app/src/test`, `app/src/androidTest` | Hygiene: duplicate template tests under a second package, `com.axelliant.hrms` | XS | 2026-09-24 |
| `.idea/` | Hygiene: IDE and SonarLint caches are still tracked (already in `.gitignore`). Deleting them crashes the diff-scoped gates, because three SonarLint cache files are not valid UTF-8 and `scripts/lib/ax_config.py` decodes the diff strictly; untrack them once the gate reads the diff tolerantly | XS | 2026-09-24 |
