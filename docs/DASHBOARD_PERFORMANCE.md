# Dashboard caching and performance

Branch: `feature/location-handling-improvements`.

Home keeps successful dashboard snapshots for 30 seconds and manager counts for
60 seconds in memory. Cache keys include the login generation, backend token,
local date and timezone. Nothing is persisted to disk. Logout/session reset clears
the cache and rejects late responses, including a later login with the same token.

Cached content renders immediately. Expired content remains visible during a
background refresh and after a refresh failure. A thin progress indicator replaces
the blocking dashboard loader. Tap “Today attendance” to force a refresh.

Home ViewModels follow the Fragment lifecycle. Reads are cancellable and concurrent
loads are deduplicated. Tile adapters rebuild only when the manager role changes.
Successful check-in/out invalidates dashboard and team snapshots before reloading.
An expired attendance snapshot must refresh before a punch; the user then reviews
the updated action and taps again. Punches are not cached, queued or replayed.

Shared envelope decoding/body reads run on a fixed two-thread worker pool, with
results and session-expiry events delivered on the main thread. Cleared ViewModels
cancel their shared API calls. Splash navigation follows a short 750 ms delay
independently of GIF decoding or animation completion.

Regression coverage checks cache expiry, concurrent reads, session/day separation,
invalidation, cancellation, stale-content retention, punch deduplication, successful
punch refresh, and worker/main-thread result scheduling. Device startup, scrolling,
rotation, background/foreground, logout/login and live attendance still need a
manual pass; no measured speedup is claimed from unit/build validation alone.
