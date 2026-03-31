# HRMS Axelliant Android — Agent Guide

This is a quick-start for automation or support agents working in this repo. It summarizes how to build, test, and what to watch out for when making changes.

## Build, Lint, and Test

- Primary quality gate: `./gradlew lint testDebugUnitTest assembleDebug`
- Other helpful commands: `./gradlew assembleDebug`, `./gradlew bundleRelease`
- `gradlew` must be executable (`chmod +x gradlew`).
- Current CI failure: plugin resolution for `com.android.application:8.2.0` (seen on `./gradlew lint testDebugUnitTest assembleDebug`); reproduce before changes and call it out if still present.

## Local Git Hook

- Install the pre-push hook to mirror CI locally:
  - `chmod +x scripts/pre-push.sh`
  - `ln -sf ../../scripts/pre-push.sh .git/hooks/pre-push`
- Skip in emergencies: `SKIP_PRE_PUSH=1 git push`

## CI Workflows

- `.github/workflows/android-ci.yml` for PR validation.
- `.github/workflows/android-release.yml` for tagged or manually triggered releases.

## Codebase Notes

- API base URL: `https://hris.axelliant.com/api/method/`
- Backend notification endpoints exist (`get_notification_capabilities`, `register_notification_device`, etc.) but client-side FCM delivery is not implemented yet.
- Android Gradle Plugin warning: project targets `compileSdk=36` while AGP is 8.2.0 (tested up to 34); adjust AGP or suppress if upgrading later.

## Performance Issues to Address

- **Unbounded LiveData observers**: Multiple `observeForever` usages without removal (e.g., `HomeViewModel`, `RequestViewModel`, `LeaveViewModel`, `AttendanceViewModel`), causing leaks and duplicate work. Migrate to `viewModelScope`/`liveData` builders with lifecycle-aware observation.
- **Adapter invalidations**: Several adapters call `notifyDataSetChanged()` (e.g., `ProfileCertificationAdapter`, `SubFilterAdapter`, `AddExpenseFragment` attachments). Replace with `ListAdapter` + `DiffUtil` to avoid full list redraws and UI jank.
- **Verbose network logging**: `ApiHandler` enables `HttpLoggingInterceptor.Level.BODY` for all builds, increasing I/O and exposing payloads. Restrict logging to debug builds and disable for production.
- **No client-side caching or retries**: OkHttp client lacks caching and retry/backoff, leading to repeated network hits and poor offline/slow-network behavior.

## Implementing Live Notifications via FCM

1. Add Firebase config (`google-services.json`), dependencies (`firebase-messaging`, `firebase-analytics` if needed), and apply `com.google.gms.google-services`.
2. Request notification permission on Android 13+ with rationale and fallback UI.
3. Create a `FirebaseMessagingService` to handle token refresh and data messages; send the FCM token (and device metadata) to the backend using `register_notification_device`.
4. Support `disable_notification_device` on logout/uninstall flows.
5. Handle data payloads in the service to render local notifications with deep links into approvals, requests, and notifications inbox.
6. Add badge counts and inbox sync by calling `get_notifications`/`mark_notification_read` after taps.
7. Secure the channel: use HTTPS only, validate sender, and avoid logging notification payloads in production.
