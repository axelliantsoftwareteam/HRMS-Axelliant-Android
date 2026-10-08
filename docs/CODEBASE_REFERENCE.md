# HRMS Axelliant Android: Codebase Reference

Working notes for Claude and contributors. Built by reading the source on branch `claude-reference-docs` (cut from `Design-Fluent` at `ebc036c`). Complements `README.md`, `ARCHITECTURE.md` and `COMPATIBILITY-MATRIX.md`; this file is about *how the code actually behaves today*, including its warts. Line numbers are approximate and drift as code changes.

Source root: `app/src/main/java/com/axelliant/hris/` (abbreviated `…/` below).

---

## 1. What the app is

Employee and manager HR client ("AXE HR") for the Axelliant ERPNext/Frappe HRIS at `https://hris.axelliant.com/api/method/`. Features: check-in/out with geofence, attendance stats, leave (request, balance, team quota), attendance (check-in correction) requests, expense claims with attachments, document requests, resource-hours claims, and a manager approvals inbox.

| Item | Value |
|---|---|
| applicationId / namespace | `com.axelliant.hris` |
| versionName / versionCode | `1.17` / `262031249` (CI overwrites both on release) |
| compile / target / min SDK | 36 / 36 / 24 |
| Language / UI | Kotlin 1.9.10, XML Views, ViewBinding + DataBinding, single Activity + Navigation component |
| Build | AGP 8.5.2, Gradle 8.7, Java 8 source/target, `kapt` plugin applied |
| ABIs | arm64-v8a, armeabi-v7a |
| Min JDK to build | 17 (CI uses Temurin 17) |

Key libraries (`gradle/libs.versions.toml`): Koin 3.5.6, Retrofit 2.9.0 + Gson + OkHttp logging 4.12, Navigation 2.7.7 (+ SafeArgs plugin, though nav graph has no actions/args), MSAL 5.6.0, Glide 4.16, play-services-location/maps, Play in-app-update, `dhaval2404:imagepicker` 2.1, SSP/SDP, circleimageview, SmoothBottomBar, commons-net (NTP), android-gif-drawable. Unused catalog entries: jjwt, MPAndroidChart, navigation-fragment (legacy).

---

## 2. Architecture

```
Fragment (screens/)  ──by inject()──▶  ViewModel (viewmodel/)  ──▶  Repo (repos/)  ──▶  ApiInterface (network/)
   ▲  EventObserver                       MutableLiveData<Event<T?>>      observeForever          Retrofit → OkHttp
   └──────────────── LiveData<Event<T>> ◀──────────────────────────────  MutableLiveData<BaseApiModel<T>>
```

- **DI:** Koin, started in `BaseApplication`. Modules in `di/SingeltonModules.kt`:
  - `singletonModule`: `TestModelInjection` (leftover), `GlobalConfig`, `ApiInterface`, `SessionManager`, `Validator`.
  - `viewModelModule`: Login, Home, Attendance, Leave, Expense, Request, Base, ResourceManage ViewModels.
  - `factoryModule`: Login, Home, Attendance, Leave, Expense, Request, ResourceManage repos.
  - **Not registered:** `ProfileViewModel`, `ProfileRepo` (see §10).
- **Single Activity:** `MainActivity` hosts `nav_host_fragment`; `BaseActivity` owns the shared `LoadingDialog`.
- **Navigation:** `nav_graph.xml` declares fragments only (no actions/arguments). Navigation is done through `navigation/AppNavigator.kt`, which, per call, builds a `NavAction` at runtime, attaches it to the current destination (`putAction(R.id.xxx_fragment_action, …)`, ids in `values/navgation_ids.xml`) and navigates with a `Bundle`. `GlobalConfig.getInstance().navController` is the global controller (set in `MainActivity.onCreate`). All `navigateToX` methods use `getCurrentDestinationId()!!`.
- **Events:** `event/Event` + `EventObserver` implement the single-shot LiveData event pattern. ViewModels expose `isLoading: LiveData<Event<Boolean>>` (from `BaseViewModel`); fragments toggle `showDialog()`/`hideDialog()`.
- **Network layer:**
  - `ApiHandler` builds one Retrofit/OkHttp (60 s connect/read/write; interceptor adds `X-Client-Timezone`; `HttpLoggingInterceptor` at `Level.BODY` always on).
  - `ApiInterface` returns raw `Call<ResponseBody>`; each repo Gson-parses into `BaseApiModel<T>` (`{ message: { data: T } }`).
  - `BaseCallBack<T>` maps HTTP/IO errors to `ErrorMessages` strings, **and pushes every HTTP status into `AppConst.observableCode` (a global LiveData)**.
  - Auth: every repo call passes header `Authorization: token ${AppConst.TOKEN}`; `AppConst.TOKEN` is a global mutable `String?` set at login and re-read from `SessionManager` in `HomeFragment.onViewCreated`.
- **Result convention:** every response model carries `meta: Meta(message, status, status_code)`. Repos synthesize `Meta(errorString,false)` on failure, and fragments branch on `response?.meta?.status == true`, else `context.showErrorMsg(meta.message)`.
- **Repo shape:** all repo methods are ~35-line copies of the same enqueue/parse/failure block. A generic helper would remove roughly 3,000 lines.
- **Global state:** `GlobalConfig` (companion) holds `currEmployee: EmployProfile?` after the dashboard loads. `isCurrentManager()` returns **true when `currEmployee == null`**, so everyone looks like a manager before the dashboard returns.

### Error UX (`extention/Extentions.kt`)
`showErrorMsg()` classifies raw messages (blank/"null", "list index out of range", permission wording, network wording, exception/traceback/html) into user-safe text. Supportable errors open an AlertDialog with "Report Issue" which composes an email to `hris-support@axelliant.com` (app version, screen, user email, timestamp, raw error). Other errors are toasts. `showSuccessMsg()` is a toast (default text "Feature in progress"). Unit-tested by `ExtentionsTest`.

Other helpers: `valueQualifier()` (null/""/"null" → `"--"`), `ImageView.setUrlImage/setLocalImage` (Glide), `getNtpTimeFormatted()` (NTPUDPClient, used as the check-in timestamp), `Utils` (date ranges/format), `Validator` (mostly unused password/date validators from a template app), `ViewAnimation`, `RoundedButton` custom view, `LoadingDialog`.

---

## 3. Startup, auth and session flow

1. `SplashFragment` plays `applogo.gif` once; on animation end it **always** calls `AppNavigator.navigateToHome()`. There is no logged-in check here. It also logs the app signing hash (`printHashKey`, `getSignatureHash` which targets a different package name, `com.axelliant.android_erp`).
2. `HomeFragment.onViewCreated` loads `AppConst.TOKEN = sessionManager.getToken()` and calls the dashboard. With no/invalid token the server returns 401.
3. `BaseCallBack` publishes the 401 on `AppConst.observableCode`; `MainActivity` observes it and calls `AppNavigator.navigateToLogin()`.
4. `LoginFragment`: MSAL single-account sign-in (`R.raw.auth_config_ciam_auth`, scope `User.Read`, authority AAD multi-org). On success it sends `account.idToken` to `POST hrms.api.mobile_v1.get_set_user_token` (`LoginRequest{microsoft_token}`). Response `access_token{api_key, api_sec, email}` → token is stored as `api_key:api_sec` in `SessionManager` (SharedPreferences `UserPref`) and in `AppConst.TOKEN`; then navigate Home. On failure it shows the error and signs out of MSAL.
5. Logout: the bell icon (`ivNotification`) on Home, confirm dialog → MSAL `signOut` → `sessionManager.logoutUser()` → login.
6. The Employee/Vendor toggle and the "Login" button on the login screen are decorative (`btnLogin` shows "Feature in progress").
7. In-app updates: `MainActivity.checkForAppUpdate()` (flexible preferred, else immediate; snackbar "RESTART" when downloaded; resumes immediate updates in `onResume`).
8. Double back-press to exit on Home.

Bottom navigation exists in the layout and is wired with `setupWithNavController`, but `MainActivity` sets it `GONE` for every destination; `menu_bottom_nav.xml` has an item with no id and a `moreFragment` id that matches no destination.

---

## 4. Navigation map

`nav_graph` start = `splashFragment`. Destinations and how they are reached:

| Destination | Fragment | Entered from | Bundle |
|---|---|---|---|
| splash | `SplashFragment` | app start / `navigateToSplash` | none |
| login | `LoginFragment` | 401 / logout | none |
| home | `HomeFragment` | splash, login | none |
| attendanceStats | `AttendanceStatsFragment` | Home "Attendance" tile | none |
| myAttendanceDetail | `MyAttendanceDetailFragment` | Attendance stats "My"; Team attendance row | `employeeId` (`AppConst.KEY_ID`) |
| teamAttendanceDetail | `TeamAttendanceDetailFragment` | Attendance stats "Team" | none |
| leaves | `LeavesFragment` | Home "Leaves" | none |
| myLeaveDetail | `MyLeaveDetailFragment` | Leaves "View" | none |
| teamLeaveDetail | `TeamLeaveDetailFragment` | Leaves "Team" | none |
| request | `RequestFragment` | Home "Request"; edit from My Leave / Check-in list | `requestType`, `leaveRequest`, `attendanceRequest` |
| checkInList | `CheckInListFragment` | Home "Check IN" | none |
| expense | `ExpenseFragment` | Home "Expense" | none |
| addExpense | `AddExpenseFragment` | Expense "+" / row edit | `expenseId`, `expenseRequest`, `expenseAttachments` |
| imageDetail | `ImageDetailFragment` | attachment thumbnail tap | `"images"` (literal; JSON `List<Attachments>`) |
| documentManage | `DocumentManagementFragment` | Home "Document Vault" | none |
| addDocument | `AddDocumnetFragment` | **nothing (dead)** | none |
| resourceManage | `ResourceManagementFragment` | Home "Resource Management" | none |
| addResourceManage | `AddResourceManageFragment` | Resource "+" / row edit | `hoursId`, `hoursRequest` |
| approval | `ApprovalsFragment` | Home "Approval" (manager only) | none |
| profile | `ProfileFragment` | **nothing reaches it** | none |

`PasswordSettingsFragment` is an empty stub, not in the graph. Complex payloads travel as Gson JSON strings in the Bundle. Key constants live in `config/AppConst.kt`.

---

## 5. Screens

### Home (`HomeFragment`, 973 lines)
- On create: request location permissions (coarse+fine), set token, `getDashboardInformation()`, build module grid, start NTP time fetch, create an MSAL client (for logout).
- Dashboard (`GET mobile_dashboard`) → `DashboardResponse{birthday_data, employee_profile, checkin_info, branch_data, shift_detail}`. Stores employee in `GlobalConfig`, populates profile card, shift note, birthdays (shown to managers only), check-in widget, branch geofences.
- **Check-in/out:** `LocationManager` GPS + network listeners (5 s). Geofence: current location within **200 m** of any `branch_data` entry ⇒ `loc="In Office"` and branch name shown; otherwise `"Work From Home"` (`LocationFilter`). The button flips via animators using `checkin_info.is_check_in_button/is_check_out_button`. Confirm dialog → `POST create_checkin` with `CheckInRequest{log_type IN/OUT, date_time (NTP), location, request_status "Approved", attendance_reason "Punch from application"}`; on success reload dashboard. Blocks with "Please wait we are fetching your location" while `loc == null`.
- Manager panel: `GET get_team_checkin_detail` → counts (checked in, checked out, on leave, in office, WFH, absent); tapping a count calls `POST get_team_checkin_detail_list{filters}` and opens `TodayTeamAttendanceDetailBottomSheet`.
- Module tiles (`HomeMenu`): Attendance, Request, Leaves, Check IN, Expense, Document Vault, Resource Management, and Approval (managers). The grid list is built in `dataPopulate()` twice per call; the manager branch also fires `getTodayTeamInfo()`, so it can run multiple times.
- QR icon is a stub.

### Attendance
- `AttendanceStatsFragment`: Week (`GET get_week_attendance_stats`) / Month (`POST get_month_attendance_stats`) toggle; self counts, shift, and (manager) team counts. Navigates to My/Team detail. Quirk: "Half day" tile shows `present`; team stats are skipped on Month (stale values stay); `!!` on `team_attendance_counts` even for non-managers.
- `MyAttendanceDetailFragment`: `POST get_filtered_attendance` with `employee_list=[employeeId]`, Week/Month/Custom (MaterialDatePicker), status chips (`SubFilterAdapter`, "All" prepended). Row action button is wired to nothing.
- `TeamAttendanceDetailFragment` (manager): `POST get_team_attendance`, team-member spinner (`GlobalConfig.getReportingEmploys()`, "All Team" → `[]`), row → My detail. Fires the request twice on entry (explicit call + spinner initial callback).

### Leaves
- `LeavesFragment`: `POST get_leave_details_self` (Week = last 7 days, Month = calendar month), balance grid (`RemainingLeaveAdapter`, 3 cols), upcoming leaves (`POST upcoming_leave_self`, tomorrow → 31 Dec; inverted on 31 Dec). Manager team stats. Layout ids are reused from attendance, so `tvPresentTxt` shows "all leaves" etc.
- `MyLeaveDetailFragment`: `POST get_leave_application_requests`; edit only if `status == "Open"` (literal) → `RequestFragment` in LEAVE edit mode.
- `TeamLeaveDetailFragment`: tab 1 "Requests" `POST get_leave_application_requests_team`; tab 2 "Quota" `GET get_leaves_detail_team` (lazy, searchable by name via `LeaveQuotaAdapter`; `quotaLoaded` is set before the response so a failure is never retried).

### Request (`RequestFragment`, 898 lines)
Create/edit/delete a **Leave** or **Attendance** request (`RequestFilter.LEAVE/ATTENDANCE`).
- Leave: `GET get_leaves_detail` (types + remaining balance) → `POST get_leave_days` (server computes days; half-day supported via `half_day`, `half_day_date`) → `create_leave_application` / `update_leave_application` / `removed_leave_application`. Validation order: type, start, end, reason, then requested days ≤ remaining balance (balance check also runs on edit, so an edit can be blocked by its own already-deducted days). Edit mode locks the type spinner; half-day state is not restored on edit.
- Attendance: `GET get_checkin_select_field` (attendance types + locations) → `create_checkin` (`AttendanceRequest`, `request_status "Pending"`) / `update_checkin_request` / `removed_checkin`. Date max = today; time formatted unpadded (`9:5`).
- Edit gating lives in callers: leave `"Open"`, check-in `"Pending"`.

### Check-ins (`CheckInListFragment`)
`POST get_employee_checkins` (`LeaveCountRequest`, `filters: List<String>?`), Week/Month/Custom + status chips. The "Approved" chip expands to `[Approved, approved, Processed, processed, waiting, Waiting]`. Edit only when `requeststatus == "Pending"`; the error text uses `ErrorMessages.X.toString()` (prints enum name) instead of `.errorString`.

### Expenses
- `ExpenseFragment`: `POST get_expense_claim_requests`, filters, edit only if `status == "Draft"`.
- `AddExpenseFragment` (640 lines): multi-row form (type from `GET get_expense_type`, date ≤ today, amount > 0, description) + attachments. Submit is two-step: `create_expenses`/`update_expense` → on success upload each pending image sequentially via multipart `upload_file_attachment` (docname = expense id, folder `Home/Attachments`, doctype `Expense Claim`). Delete: `delete_expense_claim`. Attachment X on an existing file calls `delete_attachment` **immediately** (not on Update). Pick via ImagePicker (camera or gallery, 1024 compress, square crop) through deprecated `onActivityResult`.

### Document requests (`DocumentManagementFragment`)
Lists via **legacy** `hrms.hr.doctype.employee.employee_request_form.get_employee_forms`; "+" opens an AlertDialog (subject + description, two-way DataBinding to `ExpenseViewModel`) → legacy `create_employee_form`. `AddDocumnetFragment` is a dead commented-out stub.

### Resource hours
- `ResourceManagementFragment`: **legacy** `employee_resource_hours.get_resource_hour_requests`; select Draft rows (`docstatus==0`) and `submit_resource_hours`.
- `AddResourceManageFragment`: rows of project/date/hours (`get_projects`), `create_resource_hours` / `update_resource_hours` / `delete_employee_resource_hours`. Hours have no daily cap or duplicate check.

### Approvals (`ApprovalsFragment`, manager)
Four tabs, each fetched with `employeeId` filter and `for_approvals=1`:

| Tab | Fetch | Row action |
|---|---|---|
| Leave | `get_leave_application_requests_team` | `take_approval_action` type `leave` |
| Attendance | `get_checkin_approvals` | `take_approval_action` type `checkin` |
| Expense | `get_expense_approvals` | `take_approval_action` type `expense` |
| Resources | legacy `get_resource_hour_approvals` | bulk checkbox → legacy `submit_resource_hours{status}` |

`bulk_take_approval_action` (`BulkApprovalActionRequest`) has repo + ViewModel support for leave/checkin/expense but **no UI calls it**.

### Profile (`ProfileFragment`)
Profile + certifications (`get_profile_of_employee`, `get_employee_certifications`, `create_employee_certification`). Fully coded but unreachable and its DI is missing (§10).

---

## 6. API surface (`network/ApiInterface.kt`)

Prefix `hrms.api.mobile_v1.` unless marked **L** (legacy `hrms.hr.doctype.employee.*`).

| Area | Endpoint | Verb |
|---|---|---|
| Auth | `get_set_user_token` (no Authorization header) | POST |
| Dashboard | `mobile_dashboard` | GET |
| | `get_team_checkin_detail` / `get_team_checkin_detail_list` | GET / POST |
| | `create_checkin` (punch **and** attendance request) | POST |
| Attendance | `get_week_attendance_stats` / `get_month_attendance_stats` | GET / POST |
| | `get_filtered_attendance`, `get_team_attendance` | POST |
| | `get_employee_checkins`, `update_checkin_request`, `removed_checkin`, `get_checkin_select_field` | POST/POST/POST/GET |
| | `get_checkin_approvals`, `update_checkin_approval_status` (unused) | POST |
| Leave | `get_leave_details_self`, `get_leave_application_requests`, `get_leave_application_requests_team`, `upcoming_leave_self`, `get_leave_days` | POST |
| | `get_leaves_detail`, `get_leaves_detail_team`, `get_leave_type` (unused) | GET |
| | `create_leave_application`, `update_leave_application`, `removed_leave_application`, `update_leave_status` (unused) | POST |
| Expense | `get_expense_claim_requests`, `create_expenses`, `update_expense`, `delete_expense_claim`, `delete_attachment`, `get_expense_approvals`, `upload_file_attachment` (multipart) | POST |
| | `get_expense_type` | GET |
| | `change_status_of_expense` (only via dead path) | POST |
| Approvals | `take_approval_action`, `bulk_take_approval_action` | POST |
| Profile | `get_profile_of_employee`, `get_employee_certifications` / `create_employee_certification` | GET / POST |
| Docs **L** | `employee_request_form.get_employee_forms`, `.create_employee_form` | POST |
| Resources **L** | `employee_resource_hours.get_resource_hour_requests`, `.get_projects` (GET), `.create_resource_hours`, `.update_resource_hours`, `.delete_employee_resource_hours`, `.submit_resource_hours`, `.get_resource_hour_approvals` | POST |

README says the project should stay on `mobile_v1` and avoid `hrms.hr.doctype.employee.*`, yet document requests and all resource-hours calls still use the legacy path. Notification endpoints listed in the README (`get_notifications`, `register_notification_device`, …) are **not** implemented in the app.

---

## 7. Data models

- Packages under `model/`: `base` (`BaseApiModel<T>`, `BaseModel<T>`, `Meta`), `login`, `dashboard` (`DashboardResponse`, `EmployProfile`, `CheckInInfoResponse`, `BranchDataResponse`, `Birthday`, `FilterModel`), `attendance`, `leave` (+ `leaveCount`), `checkin`, `expense`, `documentRequest`, `resourceManage`, `todayTeam`, `approval`, `post`, `profile`, plus `Modules` (home tile with Drawables) and `ImagePath` (multipart wrapper).
- Common request body `AttRequest{start_date,end_date,filters,employee_list,for_approvals}` (also `AttendanceInput` with an `AttendanceFilter` enum used inside the ViewModel layer, and `LeaveCountRequest` with `filters: List<String>`).
- Many non-null Kotlin `String` fields are populated by Gson from server JSON, so a null in the payload slips through the type system and crashes later (`AttendanceDetail`, `LeaveDetail`).
- `SubmitDocument` (resource hours) lives in `model/documentRequest`. `LeaveApproval` doubles as the attendance-approval request. `AddExpense.expenseTypeList` and `ProjectHour.expenseTypeList` are client-only lists that get serialized into request bodies.
- Unused: `AddResourceType`, `UpdateResourceHour`, `ExpenseStatu`, `Test`/`TestModelInjection`, `loginMic/*`.

Enums (`enums/`): `HomeMenu`, `RequestFilter`, `AttendanceFilter`, `CheckRequestFilter`, `LeaveStatus` (string statuses reused across modules), `ResourceStatus` (docstatus 0/1), `LocationFilter`, `TodayTeamStatus`, `ValidateEnum`.

---

## 8. Build, CI and release

- `app/build.gradle.kts`: release signing is read from env vars `ANDROID_KEYSTORE_PATH/PASSWORD`, `ANDROID_KEY_ALIAS/PASSWORD`; **`isMinifyEnabled = false`** for release. `buildFeatures{viewBinding, dataBinding}`.
- `.github/workflows/android-ci.yml`: on PR/push to `main`: JDK 17, `./gradlew lint testDebugUnitTest assembleDebug`, uploads APK + reports.
- `.github/workflows/android-release.yml`: on tag `android-v*`, push to `main`, or manual: validates secrets, **rewrites `versionCode` (`date -u +%y%j%H%M`) and `versionName` (`1.0.0+YYYY.MM.DD.<run>`) via `sed`**, decodes keystore from secrets, builds APK+AAB, uploads AAB to Google Play (`production` track, status `completed` by default). A push to `main` therefore publishes to production. `versionCode` in `build.gradle.kts` must stay above the store value.
- Tests: `ExtentionsTest` (error-message mapping) and template `ExampleUnitTest`/`ExampleInstrumentedTest` (there are two packages: `hris` and a stale `hrms`). No coverage of repos, ViewModels, or approvals.
- Local commands: `./gradlew assembleDebug`, `lint`, `testDebugUnitTest`, `bundleRelease`.

---

## 9. Conventions and gotchas for editing

- Adding a screen: fragment in `screens/` extending `BaseFragment`, destination in `nav_graph.xml`, `values/navgation_ids.xml` action id, `navigateToX` in `AppNavigator`, ViewBinding layout `fragment_*.xml`.
- Adding an API call: method in `ApiInterface` → repo method (copy the existing block) → ViewModel method + `MutableLiveData<Event<T?>>` → observe with `EventObserver`. Register any new ViewModel/Repo in `SingeltonModules.kt` (this was missed for Profile).
- ViewModels are obtained with `by inject()` in almost every fragment, not `by viewModel()`. That makes them unscoped (recreated on rotation, `onCleared` never called). `ProfileFragment` is the only one using `by viewModel()`.
- Repos observe with `observeForever` and never remove the observer; each call leaks one.
- Status strings are scattered literals (`"Open"`, `"Pending"`, `"Draft"`, `"Approved"`) and `LeaveStatus` values; keep them in sync with ERPNext.
- Packages: code is under `com.axelliant.hris` (the `hrms` test dirs are stale). Many identifiers are misspelled and must be matched exactly: `Extentions`, `extention/`, `SingeltonModules`, `AddDocumnetFragment`, `navgation_ids`.
- Copy-paste artifacts to expect: `ResourceManageViewModel.deleteExpense/myPostExpenseResponse/expenseApprovalStatus`, `TeamLeaveDetailFragment` layout ids reused from attendance, `ApprovalsDetailAdapter.tvLeaveApproverTxt` showing request status.

---

## 10. Known issues and risks (prioritized)

### Security / release
1. **`erp_key_store.jks` is committed** at the repo root. Treat the keystore as compromised if it is a real release key; remove it from history and rotate.
2. `AppConst.kt:8` contains a commented-out real-looking `key:secret` API token. Remove it and rotate it.
3. `android:usesCleartextTraffic="true"` and `allowBackup="true"` in the manifest; `HttpLoggingInterceptor.Level.BODY` is always on (logs auth headers and HR payloads), and repos log request URLs at `Log.e`.
4. Release builds are unminified; the API token and the user's password slot are stored in plain SharedPreferences (`SessionManager`).
5. Release workflow publishes to Play production on every push to `main`.
6. Repo hygiene: ~282 files from `.gradle-user/` (wrapper zip, daemon logs, native dlls) and `.idea/sonarlint/*` are tracked despite `.gitignore`. Untrack them.
7. Manifest requests `WRITE/READ_EXTERNAL_STORAGE` with no `maxSdkVersion` while targeting 36 and no `READ_MEDIA_*`; `AddExpenseFragment` requests WRITE+CAMERA on every open and ignores the result. `roundIcon` points at `@color/white`.

### Crashes / correctness
8. **401 handling can NPE:** `MainActivity` calls `mSingleAccountApp!!.signOut(...)` on 401, but `mSingleAccountApp` is never assigned there. Only the first `observableCode` observer (registered when logged in) does this; a second observer also navigates to login.
9. **Profile module crashes if ever opened:** `ProfileFragment` uses `by viewModel()` for a ViewModel with no Koin definition (`ProfileViewModel`/`ProfileRepo` missing from `SingeltonModules.kt`) and nothing navigates to it.
10. Multipart upload `folder` part is created with the field name `"docname"` (`ExpenseRepo.kt` ~152, `ResourceManageRepo.kt` ~150): the form has two `docname` parts and no `folder`. `imagePath.file!!/docname!!/folder!!` can NPE.
11. Upload failures are ignored: `postExpenseResponse` observer in `AddExpenseFragment` never checks `meta.status`; the user can leave with attachments silently lost. Also `AttachmentsAdapter` uses `uri!!`/`imageUrl!!`.
12. Resource-hours edit gate mismatch: the row's Edit button is shown for `docstatus==0` but the click checks `status=="Pending"` (`ResourceManagementFragment` ~163).
13. `!!` on possibly-null response fields: `team_attendance_counts`, `self_count`, `team_count`, `remaining_balance`, `attendance_data`, `leave_status`, `expense_status`, `leaves!!`, `values!!`. Non-managers hit `team_*!!` on Week.
14. `BaseCallBack` has a duplicate `is UnknownHostException` branch (second is unreachable); `SocketException` message is never produced.
15. `AttendanceRepo.getAttendanceStats` for `Custom` filter never enqueues, leaving the loading dialog open.
16. `ApprovalsFragment`: resource Approve/Reject send an empty selection and have no confirmation or result observer/refresh; approve/reject success is reported through `showErrorMsg`; `lateinit` adapter can be touched before init; stale `position` used in checkbox listener.
17. `GlobalConfig.isCurrentManager()` defaults to `true` until the profile loads.
18. Date/time bugs: unpadded time (`9:5`) in attendance requests; Upcoming leaves range breaks on 31 Dec; `UpcomingLeaveAdapter` prints `from_date` twice; `ImageDetailFragment` counter breaks at ≥10 images; `MyAttendanceDetailAdapter` hours unit comparison never matches.
19. Leave edit: balance check counts the leave's own days; half-day state not restored; `RequestViewModel` posts delete/update-attendance results to `updateLeaveResponse` (`deleteLeaveResponse` is dead).
20. Duplicate network calls: Week/Month/Custom "Custom pill" fires a request with stale dates then again after the picker; team spinners fire on first layout; `HomeFragment.dataPopulate()` repeats manager work.
21. Splash always goes Home; login depends on a round-trip 401.
22. `TodayTeamAttendanceDetailBottomSheet` has a non-default constructor (cannot be recreated by the system) and calls `setStyle` too late.

### Architecture / maintainability
23. Backend contract drift (legacy endpoints for documents and resource hours); no mapper/domain layer; screens parse raw payload shapes directly.
24. Unscoped ViewModels via `inject()`; leaked `observeForever` observers; bindings not nulled in several fragments.
25. Heavy copy-paste: repos, Week/Month/Custom filter code (7 fragments), approval handlers.
26. Dead code: `AddDocumnetFragment`, `PasswordSettingsFragment`, `Test.kt`, `LeaveAdapter`, `WeeklyAdapter`, `CustomSpinnerAdapter`, `TestModelInjection`, `loginMic` models, unused bulk approval stack, `update_checkin_approval_status`/`update_leave_status`/`change_status_of_expense`/`get_leave_type` endpoints, `ResourceManageRepo` duplicates of document/upload methods, commented-out blocks in `RequestFragment`.
27. Docs drift: README says `compileSdk 34` and Android Studio Hedgehog; actual is 36/AGP 8.5.2. README says the Android client uses `get_dashboard_overview`; the app calls `mobile_dashboard`. ARCHITECTURE.md describes a target shape that is not yet implemented.
28. No push notifications / FCM although `POST_NOTIFICATIONS` is declared.
29. Hard-coded UI strings (error messages, tab titles) rather than `strings.xml`; a custom `ImageDetail` bundle key `"images"` is not in `AppConst`.

---

## 11. Quick file index

| Area | Files |
|---|---|
| App/DI | `BaseApplication.kt`, `di/SingeltonModules.kt`, `di/Components.kt` |
| Config/state | `config/AppConst.kt`, `config/GlobalConfig.kt`, `utils/SessionManager.kt` |
| Network | `network/ApiHandler.kt`, `ApiInterface.kt`, `BaseCallBack.kt`, `ErrorMessages.kt`, `observable/ObservableCode.kt` |
| Navigation | `navigation/AppNavigator.kt`, `res/navigation/nav_graph.xml`, `res/values/navgation_ids.xml` |
| Base | `base/BaseFragment.kt`, `screens/BaseActivity.kt`, `viewmodel/BaseViewModel.kt`, `event/*` |
| Auth | `screens/SplashFragment.kt`, `LoginFragment.kt`, `repos/LoginRepo.kt`, `viewmodel/LoginViewModel.kt`, `res/raw/auth_config_ciam_auth.json` |
| Home | `screens/HomeFragment.kt`, `repos/HomeRepo.kt`, `viewmodel/HomeViewModel.kt`, `bottomSheet/TodayTeamAttendanceDetailBottomSheet.kt` |
| Attendance | `AttendanceStats/MyAttendanceDetail/TeamAttendanceDetail/CheckInList` fragments, `repos/AttendanceRepo.kt`, `viewmodel/AttendanceViewModel.kt` |
| Leave | `Leaves/MyLeaveDetail/TeamLeaveDetail` fragments, `repos/LeaveRepo.kt`, `viewmodel/LeaveViewModel.kt` |
| Requests | `screens/RequestFragment.kt`, `repos/RequestRepo.kt`, `viewmodel/RequestViewModel.kt` |
| Approvals | `screens/ApprovalsFragment.kt` (uses Leave/Attendance/Expense/ResourceManage ViewModels) |
| Expense/Docs | `Expense/AddExpense/DocumentManagement/ImageDetail` fragments, `repos/ExpenseRepo.kt`, `viewmodel/ExpenseViewModel.kt` |
| Resources | `ResourceManagement/AddResourceManage` fragments, `repos/ResourceManageRepo.kt`, `viewmodel/ResourceManageViewModel.kt` |
| Profile (dormant) | `screens/ProfileFragment.kt`, `repos/ProfileRepo.kt`, `viewmodel/ProfileViewModel.kt`, `model/profile/ProfileResponse.kt` |
| Build/CI | `app/build.gradle.kts`, `gradle/libs.versions.toml`, `.github/workflows/*` |
