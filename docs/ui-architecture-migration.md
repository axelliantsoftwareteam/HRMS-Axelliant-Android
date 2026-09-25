# UI Architecture Migration Plan

This document defines the target UI architecture for the merged HRIS + Internal Apps application. The goal is a consistent Fluent2 visual system while keeping XML layout control, RecyclerView/ListAdapter workflows, and existing feature behavior stable.

## Target Direction

Screens should remain XML-first unless a screen is intentionally approved as Compose-first.

```text
Screen layout: XML + ConstraintLayout + ViewBinding
Repeated data: RecyclerView/ListAdapter + XML row layouts
Reusable controls: ui/designsystem XML bridge components, moving one-by-one toward Fluent2-backed internals
Feature UI: feature-owned XML layouts, adapters, and UI mappers
Compose usage: hidden inside generic design-system bridge views only
Fallback: Material/AppCompat only when Fluent2 bridge is not practical yet
```

## Layered Architecture

### 1. Design Tokens

Single source of visual values:

- colors: `ds_*`
- typography: `TextAppearance.Fluent2.*`, `Text.Fluent2.*`
- spacing: `ds_space_*`
- radius: `ds_radius_*`
- elevation: `ds_card_elevation`
- states: enabled, disabled, pressed, selected, error
- theme: `values/` and `values-night/`

Legacy names can remain temporarily as aliases, but new work should use shared tokens.

### 2. Design Primitives

Only generic reusable controls belong in:

```text
app/src/main/java/com/axelliant/hris/ui/designsystem/components
```

Approved primitive families:

- `AppButton`
- `AppText`
- `AppCard`
- `AppTextField`
- `AppChip`
- `AppProgress`
- `AppDialog`
- `AppBottomSheet`
- `AppToolbar`
- `AppBottomNavigation`

Each primitive should expose one canonical XML view to feature screens. If a primitive needs real Fluent2 internals, Compose should be hidden inside that XML bridge view instead of exposed as a second public component path.

Example target shape:

```kotlin
class AppButtonView : AbstractComposeView(...)
```

The public screen usage remains XML-friendly.

### 3. Feature Components

Feature-specific UI does not belong in the global design system.

Examples of feature-specific UI:

- `LeaveDetailRow`
- `ExpenseRow`
- `AttendanceStatsCard`
- `CheckInListContent`
- `ProductComparisonRow`
- `QuoteWorkflowStep`

Target location:

```text
features/<domain>/<feature>/presentation
features/<domain>/<feature>/presentation/adapter
features/<domain>/<feature>/presentation/model
```

Feature UI should use design primitives internally, but should not directly use Microsoft Fluent controls.

### 4. Screens

Screens should use:

- XML layout
- ViewBinding
- Fragment
- RecyclerView/ListAdapter for repeated data
- UI mappers for visual status logic

Avoid this pattern for feature screens:

```kotlin
ComposeView.setContent {
    WholeFeatureScreen(...)
}
```

Preferred pattern:

```text
fragment_screen.xml
item_screen_row.xml
ScreenFragment.kt
ScreenAdapter.kt
ScreenStatusUiMapper.kt
```

## Current Inventory Snapshot

Current findings from the HRIS repo:

| Area | Current State | Target |
| --- | --- | --- |
| XML layouts with `ComposeView` | 0 usages | Completed for feature screens |
| Fragment/class `setContent`/`setFluentContent` call sites | 0 usages | Completed for feature screens |
| Direct Fluent imports | 14 imports | Only allowed under `ui/designsystem` |
| Direct Fluent imports outside design system | 0 imports | Completed screen/feature import isolation |
| XML `AppTextView` usages | 1160 usages | Canonical XML text path |
| XML `MaterialTextView` usages | 0 usages | Completed XML tag migration |
| XML plain `TextView` usages | 0 usages | Completed XML tag migration |
| `Layout.Fluent2.Screen` usages | 41 usages | Canonical screen background/direction shell |
| `Guideline.Fluent2.*` usages | 165 usages | Canonical screen edge/top/bottom spacing |
| Legacy guideline alias usages | 0 usages | Completed shell guideline canonicalization |
| XML `AppProgressBarView` usages | 22 usages | Canonical XML progress path |
| XML raw `ProgressBar` usages | 0 usages | Completed XML tag migration |
| Direct `ProgressBar(...)` constructors outside design system | 0 usages | Completed programmatic primitive migration |
| XML `AppCheckboxView` usages | 15 usages | Canonical XML checkbox path |
| XML raw/Material checkbox usages | 0 usages | Completed XML tag migration |
| Direct `CheckBox(...)` constructors outside design system | 0 usages | Completed programmatic primitive migration |
| XML `AppTextFieldLayout` usages | 114 usages | Canonical XML input container path |
| XML `AppTextFieldView` usages | 66 usages | Canonical XML input field path |
| XML Material `TextInputLayout` usages | 0 usages | Completed XML tag migration |
| XML raw `EditText` usages | 0 usages | Completed active XML tag migration |
| XML `AppButtonView` usages | 132 usages | Canonical XML button path |
| XML Material `Button` usages | 0 usages | Completed XML tag migration |
| XML `AppCardView` usages | 178 usages | Canonical XML card path |
| XML Material `CardView` usages | 0 usages | Completed XML tag migration |
| XML raw AndroidX `CardView` usages | 0 usages | Completed active XML card tag migration |
| XML `AppFloatingActionButtonView` usages | 11 usages | Canonical XML FAB path |
| XML Material `FloatingActionButton` usages | 0 active usages | Completed active XML tag migration |
| Shared bottom-sheet dialog helper usages | 13 usages | Canonical dialog creation path |
| Direct `BottomSheetDialog` constructors outside design system | 0 usages | Completed bottom-sheet creation isolation |
| Legacy HRIS `TEXT_*` style usages | 0 usages | Completed active HRIS text alias migration |
| Legacy Internal Apps `IA_TEXT_*` style usages | 0 usages | Completed active IA text alias migration |
| Legacy color alias references outside `colors.xml` | 0 references | Completed active legacy color alias migration |
| Global feature Compose folder | 0 Kotlin files in `com.axelliant.hris.components` | Keep feature UI in feature packages; convert Compose islands later |
| Button primitive | `AppButtonView` is the only public button path; unused Compose wrapper removed | Use this bridge pattern for every primitive |
| Card primitive | `AppCardView` is the public XML card path; unused Compose card wrapper removed | Keep XML card stable until a safe Fluent bridge strategy is proven |

## Component Classification

### Design-System Primitives

These should stay or move into `ui/designsystem/components`:

| Component | Current Status | Target Action |
| --- | --- | --- |
| `AppButtonView.kt` | Canonical XML button view with Fluent-style states and legacy icon compatibility | Standard bridge pattern for other primitives |
| `AppTextView.kt` | Canonical XML text view with Fluent role/tone attrs | Keep as only XML text primitive; reduce legacy style aliases gradually |
| `AppCard.kt` | Canonical XML card container lives in design system | Keep XML container stable, align tokens and XML compatibility |
| `RoundedButton.kt` | Removed after migrating all XML usages to `AppButtonView` | No new usage allowed |
| `AppTextFieldView.kt` | Native input wrapper | Keep XML control, align with Fluent2 tokens |
| `AppTextFieldLayout.kt` | Native input layout wrapper | Align with Fluent2 tokens |
| `AppProgressBarView.kt` | Canonical XML progress view with Fluent token defaults | Keep bridge native until Fluent2 parity is proven |
| `AppCheckboxView.kt` | Canonical XML checkbox view with Fluent token defaults | Keep bridge native until Fluent2 parity is proven |
| `AppFloatingActionButtonView.kt` | Canonical XML FAB view with Fluent token defaults | Keep bridge native until Fluent2 parity is proven |
| `AppBottomNavigationView.kt` | Navigation primitive | Align after shell rules |

### Feature-Specific Compose UI

These should not remain as global app-level components:

| File | Classification | Target Action |
| --- | --- | --- |
| `AttendanceCards.kt` | HRIS attendance stats feature UI | Removed after converting `AttendanceStatsFragment` to XML cards |
| `AttendanceCardList.kt` | HRIS attendance feature UI | Removed after Home card conversion no longer referenced feature Compose UI |
| `AttendanceDetailComponents.kt` | HRIS attendance detail feature UI | Removed after converting `MyAttendanceDetailFragment` to XML + RecyclerView |
| `CheckInList.kt` | HRIS check-in feature UI | Removed after converting `CheckInListFragment` back to XML + RecyclerView |
| `ExpenseComponents.kt` | Shared Compose helper residue | Removed after detail screen conversions removed the last helper usages |
| `LeaveDetailComponents.kt` | HRIS leave detail feature UI | Removed after converting `MyLeaveDetailFragment` to XML + RecyclerView |
| `LeaveStatCard.kt` | HRIS leave feature UI | Removed after converting `LeavesFragment` stats to XML cards |

### Token Helpers

These are Fluent token helpers and should be moved or replaced after primitives are stable:

| File | Current Use | Target Action |
| --- | --- | --- |
| `AppFluentTokens.kt` | Shared Fluent button/card tokens | Keep under `ui/designsystem/tokens`, split later only if it grows |
| `AttendanceCardTokens.kt` | Feature-specific card tokens | Moved into `AppFluentTokens.kt` as shared card token helpers |
| `LeaveStatCardTokens.kt` | Feature-specific card tokens | Moved into `AppFluentTokens.kt` as shared card token helpers |

## Implementation Progress

- Removed unused public Compose-only design primitives so XML bridge components are the only screen-facing design-system API.
- Migrated active raw Material FAB XML usages to `AppFloatingActionButtonView`.
- Added FAB bridge metrics to `tools/scan-ui-architecture.ps1`.
- Aligned `AppTextFieldLayout` error text with Fluent2 caption/error tokens and made `isErrorEnabled` refresh the error row immediately.
- Added default Fluent2 text/hint colors to `AppTextFieldView` when XML/style does not provide explicit colors.
- Preserved compact fixed-height inline editors in `AppTextFieldView` so purchase-order row fields can use the canonical bridge without layout resizing.
- Migrated the final six raw `EditText` XML usages to `AppTextFieldView`.
- Updated `AppCheckboxView` so explicit XML/style text colors are preserved while Fluent2 tokens remain the default.
- Reparented legacy HRIS `TEXT_*` and Internal Apps `IA_TEXT_*` style aliases through `Text.Fluent2`.
- Replaced legacy text-style color aliases in `styles.xml` and `internalapps_styles.xml` with `ds_*`/neutral tokens.
- Migrated legacy color aliases (`black`, `white`, `grey`, `colorApp`, `btn_text_color`) to canonical `ds_*` tokens across active app resources.
- Migrated all active Internal Apps `IA_TEXT_*` layout usages to canonical `Text.Fluent2.*` styles and removed the unused IA alias definitions.
- Added HRIS Fluent2 text variants for caption/body/subheading primary, muted, and inverse cases.
- Migrated the first HRIS `TEXT_*` repeated-row batch to canonical `Text.Fluent2.*` styles: spinner rows, filter/simple rows, team-attendance bottom sheet, certification row, leave quota rows, upcoming/remaning leave rows, check-in/attendance/leave/expense/resource/team rows.
- Migrated medium-sized HRIS fragments to canonical `Text.Fluent2.*` styles: Home, Leaves, Check-In List, Attendance Stats, and Request.
- Migrated the remaining HRIS `TEXT_*` hotspots to canonical `Text.Fluent2.*` styles: add-new expense/resource rows, add document, profile, attendance/team/resource/detail fragments, remaining dialogs, leave usage, image detail, and add-leave layouts.
- Added scanner metrics for legacy HRIS text styles, Internal Apps text styles, and old color alias drift.
- Aligned `AppCardView`, `AppProgressBarView`, `AppFloatingActionButtonView`, and `AppTextView` with the same bridge rule used for `AppButtonView`: Fluent2 defaults, XML compatibility, and explicit attrs preserved.
- Updated `AppCardView` so XML `cardCornerRadius` and `cardElevation` are respected instead of overwritten by defaults.
- Migrated remaining raw AndroidX card roots to `AppCardView`.
- Added scanner metrics for direct `AppTextFieldView`, raw `EditText`, and raw AndroidX `CardView` drift.
- Simplified `AppCard.kt` so `AppCardView` is the canonical XML card primitive.
- Moved shared Fluent token helpers into `ui/designsystem/tokens`.
- Moved legacy `RoundedButton` into the design-system namespace and updated XML usages.
- Added Fluent-style XML states to `AppButtonView`: primary, outline, subtle, and destructive.
- Migrated seven primary legacy `RoundedButton` XML usages to `AppButtonView`.
- Added a shared destructive Fluent button token and migrated the final delete button to `AppButtonView`.
- Removed the legacy `RoundedButton` class, layout, and custom attrs.
- Removed the duplicate `FluentButtonView` XML bridge; XML button usage now goes through `AppButtonView`.
- Added Fluent typography attrs to `AppTextView`: `fluentTextStyle` and `fluentTextTone`.
- Migrated request-document dialog labels from `MaterialTextView` to `AppTextView`.
- Migrated 67 more HRIS request/expense/resource XML text usages from `MaterialTextView` to `AppTextView`.
- Migrated all remaining XML `MaterialTextView` tags to `AppTextView`.
- Migrated all remaining plain XML `TextView` tags to `AppTextView`.
- Moved HRIS feature Compose files from global `components` into `features/hris/presentation/components`.
- Moved feature card token helpers into the design-system token package.
- Added app-owned card and button size/style abstractions for Compose primitives.
- Routed HRIS feature Compose card usages through `AppCard` instead of direct Fluent `BasicCard`.
- Routed remaining screen `FluentTheme` usages through `setFluentContent`.
- Converted `CheckInListFragment` from `ComposeView` content to XML + RecyclerView.
- Removed the unused `CheckInList.kt` Compose screen component.
- Converted `ExpenseFragment` from `ComposeView` content to XML + RecyclerView.
- Added `MyExpenseAdapter` for the self-expense XML list path.
- Trimmed unused expense Compose row/list/filter functions.
- Converted `LeavesFragment` self/team stats from Compose to XML card groups.
- Removed the unused `LeaveStatCard.kt` Compose screen component.
- Converted `AttendanceStatsFragment` stats and shift details from Compose to XML card groups.
- Removed the unused `AttendanceCards.kt` Compose screen component.
- Converted `MyAttendanceDetailFragment` from `ComposeView` content to XML + existing attendance detail RecyclerView adapter.
- Converted `MyLeaveDetailFragment` from `ComposeView` content to XML + existing leave detail RecyclerView adapter.
- Removed unused attendance detail, leave detail, and expense helper Compose component files.
- Converted Home today attendance card from `ComposeView`/`AndroidViewBinding` to XML `AppCardView` + `AppButtonView`.
- Removed the unused `AttendanceCardList.kt` feature Compose component and unused `ComposeViewExtensions.kt` interop helper.
- Added Fluent2-backed XML primitive defaults for progress bars and checkboxes.
- Made `AppTextFieldView` multiline-safe, so XML input fields can opt out of single-line behavior.
- Migrated raw XML `ProgressBar`, raw/Material checkbox, Material text-field, Material button, and Material card tags to app-owned design-system bridge components.
- Migrated direct programmatic `ProgressBar(...)` and `CheckBox(...)` creation outside the design system to app-owned primitive views.
- Added `createAppBottomSheetDialog()` as the canonical bottom-sheet creation helper and routed direct app constructors through it.
- Routed old bottom-sheet style aliases through Fluent2 bottom-sheet styles for backward-compatible visual consistency.
- Extended `tools/scan-ui-architecture.ps1` to track primitive bridge coverage and direct bottom-sheet constructor drift.
- Applied `Layout.Fluent2.Screen` across fragment roots where safe.
- Replaced legacy HRIS/Internal Apps guideline aliases with canonical `Guideline.Fluent2.*` usage across layouts.
- Added screen-shell metrics to `tools/scan-ui-architecture.ps1`.
- Added text usage metrics to `tools/scan-ui-architecture.ps1`.
- Added `tools/scan-ui-architecture.ps1` with `-SummaryOnly` for repeatable migration tracking.

## Phase Plan

## Fluent2 XML Bridge Roadmap

This is the new cleanup and Path 2 implementation sequence.

### Phase 1: Safe Component Audit

Status: complete.

Deliverables:

- identify unused Compose-only primitive APIs
- confirm active XML bridge usage
- confirm no direct Fluent imports outside `ui/designsystem`

### Phase 2: Safe Design-System Cleanup

Status: complete for the current scope.

Deliverables:

- remove unused Compose-only public primitive files
- keep active XML bridge components stable
- simplify mixed files so each primitive has one public screen-facing path

### Phase 3: Canonical XML Bridge Documentation

Status: in progress.

Deliverables:

- document `App*View` as the public XML primitive API
- keep feature screens XML-first
- state that real Fluent2 internals should be hidden inside bridge views

### Phase 4: Verification Baseline

Status: pending.

Deliverables:

- architecture scanner passes
- Kotlin compile passes
- lint passes when time allows

### Phase 5: Button Path 2 Pilot

Status: implemented, awaiting full Gradle/device verification.

Deliverables:

- convert `AppButtonView` internals from `AppCompatButton` to a real Fluent2 `Button` hosted behind the XML bridge
- preserve XML attributes and click behavior where practical
- do not migrate other primitives until button is stable
- preserve the current public XML tag and TextView-style compatibility APIs used by existing feature code

### Phase 6: Button Regression

Status: static regression complete; full Gradle/device regression pending.

Deliverables:

- test login, app-entry, dialogs, list rows, disabled buttons, destructive buttons, and icon-only buttons
- confirm sizing in ConstraintLayout and RecyclerView rows
- verify all XML `AppButtonView` icons are vector drawables before using Fluent2 `ImageVector`
- verify `AppButtonView` compatibility APIs used by existing Kotlin code: `text`, `setText`, `setTextColor`, `setTextSize`, `isAllCaps`, `minWidth`, `maxLines`, `ellipsize`, and `setIconResource`
- run full compile/lint/device checks once the local Gradle wrapper lock is released

### Phase 7: Low-Risk Fluent2 Bridges

Status: bridge consolidation complete; real Fluent2 internal conversion deferred until button pilot is build/device-proven.

Deliverables:

- evaluate `AppCheckboxView`
- evaluate `AppProgressBarView`
- evaluate `AppFloatingActionButtonView`
- route active XML FAB usage through `AppFloatingActionButtonView`
- track FAB bridge coverage in `tools/scan-ui-architecture.ps1`

Notes:

- Checkbox and progress are already centralized on app-owned XML bridge views.
- Active Material FAB XML usages have been migrated to `AppFloatingActionButtonView`.
- Real Fluent2 Compose-backed internals for checkbox/progress/FAB should wait until `AppButtonView` passes compile and device regression.

### Phase 8: High-Risk Fluent2 Bridges

Status: XML-native token alignment complete; Compose-backed conversion deferred.

Deliverables:

- evaluate `AppTextFieldLayout` and `AppTextFieldView`
- keep validation, keyboard, password toggle, error, cursor, and focus behavior stable
- evaluate whether `AppCardView` should remain token-accurate XML instead of Compose-backed
- preserve XML child support for cards by keeping `AppCardView` native
- migrate remaining raw inline `EditText` usages to `AppTextFieldView`

### Phase 1: Audit And Classification

Status: complete.

Deliverables:

- this architecture document
- scanner tool for Compose/Fluent usage
- current component classification

No runtime behavior changes should be made in this phase.

### Phase 2: Freeze New UI Rules

Status: complete.

Rules:

- No new full-screen Compose islands.
- No direct Microsoft Fluent imports outside `ui/designsystem`.
- No new generic components outside `ui/designsystem/components`.
- XML screens should use design primitives.
- Lists should use RecyclerView/ListAdapter and XML row layouts.
- Feature UI must not be promoted to app-level generic components.

### Phase 3: Token Stabilization

Status: complete for generic component ownership; visual replacement still continues through Phase 7.

Actions:

- make `ds_*` tokens the source of truth
- keep legacy names as aliases only where needed
- align `values-night` with light tokens
- remove one-off text and button styles gradually

### Phase 4: Fluent2 XML Bridge Primitives

Status: complete.

Order:

1. `AppButtonView`
2. `AppTextView`
3. `AppCardView`
4. `AppTextFieldView` / `AppTextFieldLayout`
5. `AppChipView`
6. `AppProgressView`
7. dialogs and bottom sheets

Each primitive must support the XML attributes already used before replacing internals.

### Phase 5: Replace Legacy Generic Components

Status: in progress.

Actions:

- remove duplicate primitives outside design system
- replace direct Fluent `Button`, `BasicCard`, etc. in feature code
- keep old behavior first, improve visuals second

### Phase 6: Move Feature-Specific UI Out Of Global Components

Status: complete for package ownership; behavior remains unchanged.

Actions:

- move feature-specific files into feature packages
- for Compose screen islands, plan conversion to XML rows/adapters
- use UI mappers for status chips, colors, labels, and enabled states

### Phase 7: Convert Compose Islands To XML + Adapters

Status: in progress.

Suggested order:

1. check-in list - complete
2. expense list - complete
3. leave stats - complete
4. attendance stats - complete
5. attendance detail - complete
6. leave detail - complete
7. home cards - complete

Each conversion should be done as a separate, testable change.

### Phase 8: Screen Shell Consistency

Status: complete for current Phase 1-8 scope.

Actions:

- use `Layout.Fluent2.Screen`
- use `Guideline.Fluent2.*`
- standardize app background and margins
- avoid per-screen one-off shell spacing

Notes:

- Login keeps its intentional custom artwork/background while still using the shared shell style.
- Screen-specific content spacing can still be refined during normal feature work, but the shared shell contract is now in place.

### Phase 9: Light/Dark Theme

Status: not started.

Actions:

- finish token aliases
- add/complete `values-night` tokens
- ensure bridge components read theme tokens
- verify login, app entry, HRIS dashboard, and Internal Apps dashboard

### Phase 10: Cleanup And Enforcement

Status: safe cleanup/QA complete for current scope.

Actions:

- delete unused Compose feature files
- delete unused legacy styles
- delete duplicate resources after audit
- add scanner usage to PR checklist
- eventually fail CI if direct Fluent imports appear outside design system

Current cleanup/QA checkpoint:

- removed compiler-confirmed dead warning points in quote, purchase-order, and product-filter flows
- fixed lint-blocking drawable namespace issue
- replaced unsafe `resources.getFont(...)` calls with `ResourcesCompat.getFont(...)`
- added API targeting for light navigation bar theme attrs
- fixed invalid ConstraintLayout sibling references in quote preview loading/error overlays
- added CameraX experimental image opt-in for scanner analysis
- added app-entry feedback for unavailable workspaces, so visually disabled buttons explain why they cannot open
- replaced splash signature fallback `TODO()` with a safe pre-Android 9 signature path
- `compileDebugKotlin`, `lintDebug`, `git diff --check`, and UI architecture scanner pass

## Migration Rules For Developers

Use this decision tree:

```text
Is this a generic control?
  yes -> ui/designsystem/components
  no -> feature package

Is this repeated data?
  yes -> RecyclerView/ListAdapter + XML row
  no -> XML layout section

Does it need Fluent2?
  yes -> use App* design-system bridge
  no -> use Material/AppCompat fallback only with reason

Does the screen need a full Compose island?
  default no
  exception only if approved
```

## Verification Strategy

After every implementation batch:

```powershell
$env:JAVA_HOME=[Environment]::GetEnvironmentVariable('JAVA_HOME','User'); .\gradlew :app:compileDebugKotlin
```

For login/auth changes:

```powershell
$env:JAVA_HOME=[Environment]::GetEnvironmentVariable('JAVA_HOME','User'); .\gradlew :app:testDebugUnitTest --tests "com.axelliant.hris.features.commonlogin.presentation.CommonLoginViewModelTest"
```

Manual smoke tests:

- splash
- common login
- Microsoft login
- app entry
- HRIS entry/dashboard
- Internal Apps entry/dashboard
- logout/session expiry
