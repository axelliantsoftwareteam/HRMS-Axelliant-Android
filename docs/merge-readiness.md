# HRIS + Internal Apps Merge Readiness

This document tracks the pre-merge rules for moving Internal Apps into the HRIS host application without disturbing existing behavior.

## Final Direction

- HRIS remains the host app, application id, Play Store identity, launcher, and final delivery repo.
- Internal Apps should move into HRIS as feature code behind a dedicated entry point.
- HRIS app shell owns splash, common entry screen, root navigation, app theme, app icon, and application class.
- HRIS and Internal Apps features should depend on shared contracts, not directly on each other.

## Merge Boundary

Keep these contracts as the boundary while moving code:

- `AuthContract`
- `SessionContract`
- `AppNavigationContract`
- `DesignSystemContract`

Internal Apps feature code should be adapted to these contracts before it is imported into HRIS.

## Resource Rules

Resource collisions are the largest Android merge risk because same-name resources can silently override each other.

- Keep HRIS app-shell resource names as-is.
- Prefix Internal Apps feature resources with `ia_` before or during import.
- Promote only truly shared resources to neutral `ds_` or `app_` names.
- Do not import Internal Apps launcher, app icon, `activity_main`, root `nav_graph`, app theme, backup rules, or manifest app-shell declarations into HRIS.

Recommended names:

- Internal Apps layouts: `ia_fragment_login`, `ia_item_purchase_order`, `ia_bottom_sheet_quote_workflow`
- Internal Apps drawables: `ia_ic_search`, `ia_bg_quote_card`
- Internal Apps menu files: `ia_menu_bottom_nav`, `ia_menu_quote_actions`
- Internal Apps navigation graph: `ia_nav_graph`
- Shared design tokens: `ds_color_primary`, `ds_radius_md`, `ds_button_primary_background`

## Current Known Collisions

High priority collisions found before Internal Apps resource prefixing:

- App shell: `activity_main`, `nav_graph`, `menu_bottom_nav`, launcher icons, backup/data extraction XML.
- Startup/login/home: `fragment_app_entry`, `fragment_login`, `fragment_home`.
- Drawables: `applogo`, `ic_applogo`, `ic_add`, `ic_filter`, `ic_search`, `ic_mic`, launcher foreground/background.
- Values: `app_name`, `app_entry_title`, `app_entry_subtitle`, `entry_hris`, `entry_internal_apps`, `login`, `home`, `remember_me`, `black`, `white`.
- Styles/dimens: `bottom_gl`, `top_gl`, `start_gl`, `end_gl`, `TEXT_*`, `ds_radius_*`.
- Kotlin class names: `MainActivity`, `BaseApplication`, `HomeFragment`, `LoginFragment`, `LoginViewModel`, `SessionManager`, shared contract classes.

## Prefixing Completed In Internal Apps

The first Internal Apps prefixing pass has been completed and compiled successfully.

Renamed Internal Apps resources:

- `activity_main` -> `ia_activity_main`
- `fragment_app_entry` -> `ia_fragment_app_entry`
- `fragment_home` -> `ia_fragment_home`
- `fragment_login` -> `ia_fragment_login`
- `nav_graph` -> `ia_nav_graph`
- `menu_bottom_nav` -> `ia_menu_bottom_nav`
- `applogo` -> `ia_applogo`
- `ic_applogo` -> `ia_ic_applogo`
- `ic_add` -> `ia_ic_add`
- `ic_search` -> `ia_ic_search`
- `ic_mic` -> `ia_ic_mic`
- `ic_microsoft` -> `ia_ic_microsoft`
- `ic_filter*` -> `ia_ic_filter*`

Remaining expected collisions:

- HRIS-owned app shell resources: launcher icons, launcher foreground/background, backup rules, data extraction rules.
- Shared fonts: `poppins_*`.
- Kotlin class names that should be unified or feature-prefixed during import: `MainActivity`, `BaseApplication`, `HomeFragment`, `LoginFragment`, `LoginViewModel`, `SessionManager`, and shared contract classes.

## Value Prefixing Completed In Internal Apps

The second Internal Apps prefixing pass has been completed and compiled successfully.

Renamed Internal Apps values:

- `app_name` -> `ia_app_name`
- `app_entry_title` -> `ia_app_entry_title`
- `app_entry_subtitle` -> `ia_app_entry_subtitle`
- `entry_hris` -> `ia_entry_hris`
- `entry_internal_apps` -> `ia_entry_internal_apps`
- `home` and `home_*` -> `ia_home` and `ia_home_*`
- `login` and `login_*` -> `ia_login` and `ia_login_*`
- `remember_me` -> `ia_remember_me`
- `Vendor` / `vendor` references -> `ia_vendor`
- `black` -> `ia_black`
- `white` -> `ia_white`
- `ds_radius_sm`, `ds_radius_md`, `ds_radius_lg`, `ds_radius_xl` -> `ia_ds_radius_*`
- Internal Apps legacy `IA_TEXT_*` styles -> canonical `Text.Fluent2.*` styles
- Guideline styles `start_gl`, `end_gl`, `bottom_gl`, `top_gl` -> `ia_start_gl`, `ia_end_gl`, `ia_bottom_gl`, `ia_top_gl`

After this pass, the scanner reports no value-resource name collisions. Remaining scanner findings are expected file-level/app-shell collisions and duplicate class names that should be resolved during feature import.

## First HRIS Import Slice

Internal Apps has been imported into the HRIS host without replacing HRIS app-shell ownership.

Imported into HRIS:

- Internal Apps `features`
- Internal Apps `ui`
- Supporting Internal Apps core packages: `core/ui`, `core/session`, `core/network`, `core/extensions`, `core/scanner`, selected `core/di` modules, and `AppDrawerAction`
- Internal Apps screen fragments under `screens/fragments`
- Namespaced Internal Apps resources and values
- Internal Apps graph included as `ia_nav_graph`

Intentionally not imported/replaced:

- Internal Apps `MainActivity`
- Internal Apps `BaseApplication`
- Internal Apps launcher/app icon as the HRIS launcher
- Internal Apps `core/appentry`
- Internal Apps `core/contracts`

Navigation ownership:

- HRIS root `nav_graph` remains the host graph.
- Internal Apps destinations are prefixed with `ia*`.
- HRIS common entry routes `Internal Apps` to the imported graph id `iaInternalAppsNavGraph`.
- `InternalAppsEntryFragment` is the start destination inside the imported graph and opens `iaLoginFragment`.
- Internal Apps login remains separate from HRIS login until the common auth/login phase.
- HRIS does not navigate directly into child destinations of the included Internal Apps graph.

Verification:

```powershell
.\gradlew :app:compileDebugKotlin --no-daemon --console=plain
```

This compile passed after the first import slice.

## Before Moving Feature Code

1. Run the collision scanner.
2. Rename or exclude Internal Apps app-shell resources.
3. Prefix Internal Apps feature resources that collide with HRIS.
4. Keep shared contract classes only once in HRIS.
5. Import one Internal Apps feature area at a time.
6. Smoke test HRIS startup, HRIS login, HRIS dashboard, Internal Apps entry, Internal Apps login, and Internal Apps dashboard after every feature area.

## Scanner

Run from the HRIS repo:

```powershell
powershell -ExecutionPolicy Bypass -File .\tools\scan-android-merge-collisions.ps1 -HrisRoot "D:\gitAxelliant\HRIS\HRMS-Axelliant-Android" -InternalAppsRoot "D:\gitAxelliant\Android-ERP\InternalAppsAndroid"
```

## UI Architecture Migration

The merged app should use XML screens and RecyclerView/ListAdapter feature rows while hiding Fluent2 Compose internals behind generic `ui/designsystem` bridge components.

See:

- `docs/ui-architecture-migration.md`
- `tools/scan-ui-architecture.ps1`

Run the UI scanner from the HRIS repo:

```powershell
powershell -ExecutionPolicy Bypass -File .\tools\scan-ui-architecture.ps1
```
