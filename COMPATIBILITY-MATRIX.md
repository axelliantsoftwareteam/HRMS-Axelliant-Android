# Axelliant HRIS Compatibility Matrix

See the backend source of truth in the Axelliant HRIS backend repository. This local copy exists so Android contributors have the current contract snapshot in-repo.

## Canonical Backend Contract

- Base ERPNext/Frappe API root: `https://hris.axelliant.com/api/method/`
- Canonical mobile and cross-channel contract: `hrms.api.mobile_v1.*`
- Teams/Power Automate contract: `hrms.integrations.power_automate.*`

## Android Status

- Android uses `hrms.api.mobile_v1.*` for active HR flows.
- Native push delivery is not implemented yet.
- Backend device registration and notification inbox APIs now exist, but the Android runtime still needs FCM, permission handling, token registration, and deep-link handling.

## Related Channels

- iOS also targets `mobile_v1`.
- Web is still on a mixed legacy contract and is not yet feature-equivalent to mobile.
- Backend remains the source of truth and hosts the aggregate dashboard, approvals, requests, and notification APIs.
