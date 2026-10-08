# iOS design parity: status

Branch `ios-design-parity` (from `Design-Fluent`). Read `IOS_PARITY_BRIEF.md` for the rules the work followed
and `CODEBASE_REFERENCE.md` for the pre-redesign architecture (its screen list is superseded by this file).

## Done

| Area | What changed |
|---|---|
| Foundation | Native `DrawerLayout` (profile header, EMPLOYEE / MANAGER sections, unread pill, Sign Out), shared UI kit in `ui/` (`WsToolbar`, `WsEmptyState`, `WsDetailSheet`, `WsStatus`, `WsFormat`, avatar), iOS palette, `Ws.*` styles, vector icons, all in SDP/SSP |
| Navigation | `AppNavigator` navigates by destination id; `nav_graph.xml` holds every screen; Login/Home reset the back stack; Splash goes to Login when signed out |
| Network | `ApiResult`/`ApiParser` (handles `message.data`, payload-in-`message`, `meta.status`, 401) + `BaseViewModel.request(...)` for new calls |
| Home | Hamburger + bell with unread badge; tiles reduced to Attendance, Request, Leaves, Expense (+ Approval for managers) |
| Employee screens | Leave (quota tiles + recent requests + detail sheet), Leave/Attendance request forms, Attendance list + detail sheet, Requests overview + sheet, Expense list, Create/Edit expense (attachments), Pay slips (+ PDF viewer/share), Certifications (+ add form), Profile (all iOS sections), Notifications (+ sheet, mark all read) |
| Manager screens | Team Activity (Arrivals / Hours / Quota + filter sheet), Team Calendar, Organogram (pinch/pan), Approvals (pending list, bulk, pager), Expense approvals |
| Removed | Document Vault, Resource Hours, Check-in list, Attendance Stats, Team attendance, My/Team Leave detail screens, `PasswordSettings`, their repos/models/layouts |
| Fixes along the way | 401 handling no longer NPEs or replays on rotation; expense upload sends the right `folder` field and reports failed uploads; Profile ViewModel is registered |

Build status at the last commit: `assembleDebug`, `lint` (0 errors) and `testDebugUnitTest` (22 tests) pass.
Verified on a device: app launches, Home header, drawer (real profile data, unread count) and the
`get_notifications` / `mobile_dashboard` responses parse. The phone blocks adb input injection, so the
other screens were checked by compiling and code review only; they need a manual pass.

## Release readiness

* Demo data, the offline sandbox and the forced-manager flag were removed. Manager is `false` until the
  dashboard says `is_manager`.
* Version 1.18, versionCode 262811613 (CI recomputes the code with `date -u +%y%j%H%M`).
* Network logging is `NONE` in release builds (it used to log bodies and the auth header).
* `assembleRelease` builds unsigned locally (signing comes from CI secrets), lint has 0 errors, 23 unit tests pass.

## Please test on a device (not exercised at runtime yet)

Leave, Leave request (create/edit/delete), Attendance request, Attendance list + sheet, Requests overview,
Expense list, Create/Edit/Delete expense with camera and gallery photos, Pay slips list + PDF + share,
Certifications + add, Profile, Notifications + mark all read, Approvals (approve/reject/bulk, paging),
Expense approvals, and as a manager Team Activity / Team Calendar / Organogram. Check small and large phones.

## API assumptions to verify against the live server

* `get_leaves_detail_team` is called with POST `{}` (iOS) and falls back to the old GET.
* `get_team_checkin_detail_list` and `get_team_attendance` use GET query parameters (iOS style).
* `create_expenses` receives decimal amounts (iOS sends integers).
* Pay slip PDF comes back as raw bytes from `get_salary_slip_pdf`.
* Approvals use `get_approvals` (10 per page) and `take_approval_action` / `bulk_take_approval_action`.

## Known gaps / follow-ups

* Login screen is not redesigned (iOS has a new illustrated login); Splash is unchanged.
* Notification detail status bar shows Read and Unread in the same colour (kit limitation).
* Attendance/leave toggles render as joined segments (Material toggle group has no gap option).
* Pay slip viewer is full screen rather than a rounded sheet.
* Release hygiene issues from the reference doc still stand: committed keystore, commented token in
  `AppConst.kt`, `HttpLoggingInterceptor` at BODY level (logs auth tokens), cleartext traffic allowed,
  unminified release, Play production upload on every push to `main`.
* `LeaveViewModel`/`TeamLeaveQuotaResponse` quota code and a few orphaned colours/strings can be pruned.
