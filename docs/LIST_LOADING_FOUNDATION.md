# Shared loading foundation

This implements the shared foundation (plan steps 1–3). Attendance, Requests and
the other list migrations are separate follow-ups; the new shimmer does not
automatically replace their existing dialogs.

## Ownership

`BaseFragment.showDialog()`/`hideDialog()` now operate on that Fragment's own
blocking dialog. A stopped view dismisses its window; its requested state can be
restored on start. Destroying the view releases it. A previous screen cannot
dismiss the destination's loader. `MainActivity` no longer dismisses dialogs on a
200 ms timer. Navigation itself does not request progress.

Keep blocking progress for mutations (submit/delete/approve). New action screens
can observe `BaseViewModel.blockingLoading`, a replayable Boolean. The old
`getIsLoading()` events remain as a compatibility bridge. Calls using the shared
`request()` helper are reference-counted; legacy ViewModels publishing events
directly still need migration.

## Persistent list state

`listLoading(key)` exposes `LiveData<ListLoadState>` independently of result events.
The state retains successful/empty/error results and loading intent: initial,
refresh/filter, pull refresh, or another page. Content itself must also remain in
the ViewModel (LiveData/StateFlow); the loading state stores only its total count.

Use `requestList(call, type, result, itemCount, key, kind)` for one-API reads. It
does not open a dialog. By default, a replacement cancels old calls for the same
key and ignores late results. Separate keys isolate independent lists.

For multi-API lists such as Requests, use one `beginListLoad(...)` ticket for the
whole batch, merge its responses, then `finishListLoad(key, ticket, totalCount,
error)`. On partial failure pass the retained/merged count and an error together.
Finish all terminal paths and check `isCurrentListLoad(key, ticket)` before updating
the batch's content. Pagination count is the complete displayed count,
not only the newly returned page size. Do not merge results from obsolete tickets.

## Reusable view

`WsListLoading(viewLifecycleOwner, hostFrameLayout, onRetry)` wraps only the list
region below the toolbar. It moves the host's existing children into a retained
content area, preserving the views and scroll state. Do not pass a host containing
the toolbar, or a SwipeRefreshLayout that owns the entire toolbar/root.

```kotlin
loading = WsListLoading(viewLifecycleOwner, binding.listHost, onRetry = {
    viewModel.reload()
}).also { it.bind(viewModel.listLoading()) }
```

The controller automatically releases on view lifecycle destruction. Explicit
`release()` is idempotent and restores the original host children. Drop a stored
controller reference in `onDestroyView()` as with other view references.

Presentation:

- First load: static card placeholders with a shimmer sweep; toolbar stays outside.
- Refresh/filter: existing content plus a slim progress bar.
- Pull refresh: only the screen's existing swipe spinner; bind it to
  `state.pullRefreshing` (the shared component does not add a second spinner).
- Pagination: existing content plus a footer spinner.
- Failed initial load: inline error card and Retry.
- Failed refresh/page: retain content and show an error banner with Retry.
- Successful zero rows: empty state, configurable through constructor copy.

Retry only calls the supplied handler; it does not submit/replay mutations. A
pending operation disables Retry. Shimmer/progress animations stop on lifecycle
stop and restart on start if still loading. Disabled system animations leave
static placeholders. No additional animation dependency is required.

## Verification

Regression tests cover overlapping actions/reads, silent reads, replacement,
stale callbacks, cancellation, view-observer reattachment, independent list keys,
empty vs error, partial failures, retained content, pull-refresh indicators, and
pagination. Debug resource compilation and lint validate the reusable layouts.
Actual list integration, device lifecycle checks and visual QA belong to the
screen migration steps.
