# Android Architecture

## Role

This app is the manager and employee mobile client for Axelliant HRIS on Android.

## Current Stack

- Kotlin + Android Views
- MSAL for Microsoft authentication
- Retrofit-style backend access
- ERPNext/Frappe backend methods under `/api/method/...`

Compatibility across backend, mobile, and web is documented in [COMPATIBILITY-MATRIX.md](COMPATIBILITY-MATRIX.md).

## Runtime Boundaries

- Auth boundary: Microsoft identity
- Business boundary: ERPNext/Frappe custom HR endpoints
- UI boundary: fragments and screen-specific view models

## Core Flows

### Login

1. User signs in with Microsoft.
2. App exchanges the Microsoft identity token with backend token mapping.
3. App stores the resulting API token and session data.

### Daily Employee Actions

- punch in / punch out
- attendance requests
- leave requests
- expense requests

### Manager Actions

- attendance approvals
- leave approvals
- expense approvals

## Current Architecture Risks

- screen logic still understands raw backend payloads too directly
- aggregate `mobile_v1` APIs exist, but UI flows still need mapper-first adoption
- no FCM notification pipeline
- weak automated coverage on high-value manager flows

## Target Shape

1. Typed domain models for dashboard, approvals, requests, and profile.
2. Mapper layer between backend payloads and UI state.
3. Unified error policy with user-safe recovery and support escalation.
4. Push notification registration, inbox, deep links, and badge counts.

## Backend Contract Direction

The app should stay on the owned integration layer under:

- `hrms.api.mobile_v1.*`

and avoid any direct return to `hrms.hr.doctype.employee.*`.

## Immediate Priorities

1. keep check-in, leave, expense, and approvals flows crash-free
2. align with hardened backend responses
3. add FCM and notification inbox
4. add smoke coverage for manager actions
