# HRMS Axelliant Android

Android mobile client for the Axelliant HRIS platform backed by ERPNext/Frappe custom methods hosted at `https://hris.axelliant.com/api/method/`.

Repository: [HRMS-Axelliant-Android](https://github.com/axelliantsoftwareteam/HRMS-Axelliant-Android)

See [ARCHITECTURE.md](ARCHITECTURE.md) for runtime boundaries, data flows, and the modernization path.
See [COMPATIBILITY-MATRIX.md](COMPATIBILITY-MATRIX.md) for the current shared backend/client support matrix.

## Product Intent

This app should do four things reliably:

1. Let employees complete daily HR actions in under a minute.
2. Let managers approve or reject requests without opening the web portal.
3. Keep users informed with real notifications, not just passive screens.
4. Feel modern, stable, and trustworthy enough to use every day.

## Current State

The codebase is functional but behind modern product expectations.

- Dashboard, attendance, leave, expense, and approvals are implemented through custom ERPNext/Frappe methods.
- Microsoft login is wired through MSAL.
- The app still has legacy fragility in screen state and payload handling.
- Native push notifications are not implemented. There is no FCM dependency, no notification permission flow, and no delivery pipeline yet. The backend now exposes a registration and inbox contract, but Android runtime delivery still needs to be built.
- `android:usesCleartextTraffic="true"` is enabled in the manifest and should be removed unless there is a documented non-HTTPS dependency.
- Build verification currently depends on local Java/Android SDK setup.

## ERPNext and Frappe Integration

The app now targets the versioned mobile contract under:

- `hrms.api.mobile_v1.*`

This is valid for Frappe because custom whitelisted methods can be exposed under `/api/method/...`, but it creates product risk when there is no explicit API contract, schema validation, or versioning policy.

Official references:

- Frappe REST API: [https://docs.frappe.io/framework/user/en/api/rest](https://docs.frappe.io/framework/user/en/api/rest)
- Frappe document API: [https://docs.frappe.io/framework/user/en/api/document](https://docs.frappe.io/framework/user/en/api/document)
- Frappe webhooks: [https://docs.frappe.io/framework/user/en/guides/integration/webhooks](https://docs.frappe.io/framework/user/en/guides/integration/webhooks)

## Mobile API Reference

Base URL:

- `https://hris.axelliant.com/api/method/`

Authentication:

- Microsoft login exchange:
  - `POST hrms.api.mobile_v1.get_set_user_token`
- all other authenticated calls use:
  - `Authorization: token <api_key>:<api_secret>`

Core endpoint groups:

- Dashboard and attendance
  - `POST hrms.api.mobile_v1.get_dashboard_overview`
  - `GET hrms.api.mobile_v1.mobile_dashboard`
  - `GET hrms.api.mobile_v1.get_week_attendance_stats`
  - `POST hrms.api.mobile_v1.get_month_attendance_stats`
  - `POST hrms.api.mobile_v1.get_filtered_attendance`
  - `POST hrms.api.mobile_v1.get_team_attendance`
  - `GET hrms.api.mobile_v1.get_team_checkin_detail`
  - `POST hrms.api.mobile_v1.get_team_checkin_detail_list`
- Check-in and attendance requests
  - `GET hrms.api.mobile_v1.get_checkin_select_field`
  - `POST hrms.api.mobile_v1.create_checkin`
  - `POST hrms.api.mobile_v1.get_employee_checkins` (replaces legacy `get_checkin_request`)
  - `POST hrms.api.mobile_v1.update_checkin_request`
  - `POST hrms.api.mobile_v1.removed_checkin`
  - `POST hrms.api.mobile_v1.get_checkin_approvals`
  - `POST hrms.api.mobile_v1.update_checkin_approval_status`
- Leave
  - `POST hrms.api.mobile_v1.get_leave_details_self`
  - `POST hrms.api.mobile_v1.get_leave_application_requests`
  - `POST hrms.api.mobile_v1.get_leave_application_requests_team`
  - `GET hrms.api.mobile_v1.get_leave_type`
  - `GET hrms.api.mobile_v1.get_leaves_detail`
  - `POST hrms.api.mobile_v1.create_leave_application`
  - `POST hrms.api.mobile_v1.update_leave_application`
  - `POST hrms.api.mobile_v1.removed_leave_application`
  - `POST hrms.api.mobile_v1.update_leave_status`
  - `POST hrms.api.mobile_v1.get_leave_days`
  - `POST hrms.api.mobile_v1.upcoming_leave_self`
- Expense
  - `POST hrms.api.mobile_v1.get_expense_claim_requests`
  - `GET hrms.api.mobile_v1.get_expense_type`
  - `POST hrms.api.mobile_v1.create_expenses`
  - `POST hrms.api.mobile_v1.update_expense`
  - `POST hrms.api.mobile_v1.delete_expense_claim`
  - `POST hrms.api.mobile_v1.delete_attachment`
  - `POST hrms.api.mobile_v1.get_expense_approvals`
  - `POST hrms.api.mobile_v1.change_status_of_expense`
  - `POST hrms.api.mobile_v1.upload_file_attachment`
- Aggregate approvals and requests
  - `POST hrms.api.mobile_v1.get_approvals`
  - `POST hrms.api.mobile_v1.take_approval_action`
  - `POST hrms.api.mobile_v1.get_requests`
- Notifications
  - `GET hrms.api.mobile_v1.get_notification_capabilities`
  - `POST hrms.api.mobile_v1.get_notifications`
  - `POST hrms.api.mobile_v1.mark_notification_read`
  - `POST hrms.api.mobile_v1.mark_all_notifications_read`
  - `POST hrms.api.mobile_v1.register_notification_device`
  - `POST hrms.api.mobile_v1.disable_notification_device`

API rules for developers:

- keep the Android app pointed at `mobile_v1`, not legacy `hrms.hr.doctype.employee.*`
- preserve current payload shapes when changing backend wrappers unless you are coordinating a client migration
- approval/update endpoints now enforce owner or approver scope on the backend
- the aggregate approval API makes approval ownership explicit by `approval_type`
- malformed backend errors must be translated to user-safe UI text, not shown raw

## Key Findings From Audit

- The home grid was relying on fragile runtime ordering. Initial fixes have started.
- Some request flows assume perfect payload formatting and index directly into split arrays.
- Notification UX exists visually, but delivery capability does not exist technically.
- The app has no typed backend compatibility layer, no contract tests, and very limited crash-prevention around malformed payloads.
- Approval flows exist, but they are only as reliable as the custom ERPNext methods behind them.

## Next Take

This repo should move in the following order.

### Phase 1: Stabilize

- Remove brittle list indexing and unsafe payload parsing across dashboard, request, approval, and detail screens.
- Add central API error normalization so backend messages do not surface as raw technical toasts.
- Audit every custom endpoint for nullable and missing-field behavior.
- Remove cleartext traffic unless explicitly required.
- Add smoke tests for login, dashboard load, check-in request, leave request, and approval actions.

### Phase 2: Make It Useful

- Add real push notifications using FCM.
- Register device tokens against the backend user profile.
- Define notification event types for approvals, leave status, expense status, missed punch-outs, and announcements.
- Add a notification inbox screen backed by ERPNext Notification Log or a custom notification doctype.
- Add badge counts and deep links into approval and request detail screens.

### Phase 3: Make It Good

- Redesign the dashboard around "today" rather than a static tile grid.
- Show pending approvals, next action, current attendance status, and team risk signals above the fold.
- Improve empty states, skeleton loading, retry behavior, and offline messaging.
- Add analytics and crash reporting before expanding feature scope.

## Recommended Architecture Direction

- Introduce a typed domain layer for `Dashboard`, `Attendance`, `Leave`, `Expense`, and `Approval`.
- Replace screen-specific payload parsing with mapper classes and sealed result states.
- Version the ERPNext custom methods or place them behind a BFF layer if backend ownership allows it.
- Add feature flags for rollout of notifications, approvals redesign, and dashboard refresh.

## Local Setup

Prerequisites:

- JDK 17 or the version required by your Android Gradle setup
- Android Studio Hedgehog or newer
- Android SDK for `compileSdk 34`

Run:

```bash
bash ./gradlew assembleDebug
```

Useful commands:

```bash
bash ./gradlew lint
bash ./gradlew testDebugUnitTest
bash ./gradlew assembleDebug
bash ./gradlew bundleRelease
```

Release workflow:

```bash
git checkout develop
git pull --rebase origin develop
git push origin develop
git tag android-v1.5.0
git push origin android-v1.5.0
```

Notes:

- CI validation runs on pull requests and pushes.
- the release workflow is triggered by tags matching `android-v*` or by manual workflow dispatch
- cut release tags from the branch you intend to ship after CI is green

## GitHub Actions

This repo now includes:

- `android-ci.yml` for PR and main-branch validation
- `android-release.yml` for tagged or manually triggered release builds

Store-ready release signing and Google Play publishing are driven by GitHub configuration.

GitHub secrets:

- `ANDROID_KEYSTORE_BASE64`
- `ANDROID_KEYSTORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`
- `GOOGLE_PLAY_SERVICE_ACCOUNT_JSON`

GitHub variables:

- `ANDROID_PACKAGE_NAME`
- `ANDROID_PLAY_TRACK`
- `ANDROID_PLAY_RELEASE_STATUS`

Recommended values:

- `ANDROID_PACKAGE_NAME=com.axelliant.hris`
- `ANDROID_PLAY_TRACK=internal`
- `ANDROID_PLAY_RELEASE_STATUS=completed`

If the signing secrets are not configured, the release workflow still builds artifacts, but they will not be signed for store distribution. If `GOOGLE_PLAY_SERVICE_ACCOUNT_JSON` and `ANDROID_PACKAGE_NAME` are configured, the workflow will also push the generated `.aab` to Google Play.

## Immediate Product Priorities

If we are funding only the next small release, the order should be:

1. Approval reliability
2. Notification capability
3. Dashboard redesign
4. Test coverage and crash prevention

## Status

Recent engineering work has already started to reduce home-screen fragility and unsafe indexing. The next engineering pass should target approval workflows, notification plumbing, and payload hardening across all manager flows.
