# Mock HRIS

A [WireMock](https://wiremock.org/docs/) stand-in for the HRIS (Frappe) API, so the app can run
without reaching `https://hris.axelliant.com`. `compose.yaml` runs it on http://localhost:8000 (`docker compose up -d mock-hris`).

- `mappings/frappe-method-default.json` answers every `/api/method/...` call with Frappe's empty
  response envelope (`message`, `data`, `_server_messages`). Screens load, with no data.
- To give a method real behaviour, add a mapping with a higher priority (a lower number) that matches
  its path, e.g. `/api/method/hrms.api.mobile_v1.get_dashboard_overview`, and a response body in
  the shape the HRIS backend returns. Use invented data only: never copy real employee records here.

The request and response shapes are owned by the HRIS backend (`hrms.api.mobile_v1`); see the
[HRMS project documentation](https://github.com/axelliantsoftwareteam/eng-wiki-standards/tree/main/projects/hrms).
