package com.axelliant.hris.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.axelliant.hris.config.AppConst
import com.axelliant.hris.config.AppState
import com.axelliant.hris.event.Event
import com.axelliant.hris.model.notification.NotificationPage
import com.axelliant.hris.model.notification.WorkflowNotification
import com.axelliant.hris.network.ApiInterface
import com.axelliant.hris.network.ApiResult
import com.axelliant.hris.network.enqueueResult

/** What happened that deserves a toast. */
sealed class NotificationEvent {
    object LoadFailed : NotificationEvent()
    object UpdateFailed : NotificationEvent()
    /** [message] is HRIS's own confirmation text, if it sent one. */
    data class MarkedRead(val message: String?) : NotificationEvent()
}

/** Notification feed (20 per page) and "mark all read"; also feeds the unread badge on Home and the drawer. */
class NotificationsViewModel(private val api: ApiInterface) : BaseViewModel() {

    private data class UnreadPayload(val unread_count: Any? = null)

    data class UiState(
        val items: List<WorkflowNotification> = emptyList(),
        val total: Int = 0,
        val unread: Int = 0,
        val hasLoaded: Boolean = false,
        val loadFailed: Boolean = false,
        val loadingMore: Boolean = false,
        val refreshing: Boolean = false,
        val updating: Boolean = false
    ) {
        /** True while HRIS has notifications the list has not loaded yet. */
        val canLoadMore: Boolean get() = items.size < total
    }

    private val _state = MutableLiveData(UiState())
    val state: LiveData<UiState> get() = _state

    private val _events = MutableLiveData<Event<NotificationEvent>>()
    val events: LiveData<Event<NotificationEvent>> get() = _events

    private var generation = 0
    private var reloading = false
    private var exhausted = false
    private var loadMoreFailed = false

    private fun current() = _state.value ?: UiState()

    /** Silent refresh of the unread badge shown on Home and in the drawer. Failures are ignored. */
    fun refreshUnreadCount() {
        api.getNotifications(AppConst.authHeader(), 1, 0)
            .enqueueResult<UnreadPayload>(UnreadPayload::class.java) { result ->
                if (result is ApiResult.Success) {
                    val count = result.data.unread_count?.toString()?.toDoubleOrNull()?.toInt() ?: 0
                    AppState.unreadNotifications.value = count
                }
            }
    }

    /** Loads the first page again, replacing the list. [pull] is true for pull-to-refresh. */
    fun reload(pull: Boolean = false) {
        val before = current()
        if (reloading) {
            if (pull) _state.value = before.copy(refreshing = true)
            return
        }
        reloading = true
        generation++
        val myGeneration = generation
        val showOverlay = !pull && before.items.isEmpty()
        if (showOverlay) isLoading.value = Event(true)
        _state.value = before.copy(refreshing = pull, loadingMore = false)
        api.getNotifications(AppConst.authHeader(), PAGE_SIZE, 0)
            .enqueueResult<NotificationPage>(NotificationPage::class.java) { result ->
                if (myGeneration != generation) return@enqueueResult
                reloading = false
                if (showOverlay) isLoading.value = Event(false)
                val latest = current()
                when (result) {
                    is ApiResult.Success -> {
                        val rows = result.data.rows().distinctBy { it.name }
                        exhausted = false
                        loadMoreFailed = false
                        publishUnread(result.data.unread())
                        _state.value = latest.copy(
                            items = rows,
                            total = maxOf(result.data.totalCount(), rows.size),
                            unread = result.data.unread(),
                            hasLoaded = true,
                            loadFailed = false,
                            refreshing = false
                        )
                    }
                    is ApiResult.Failure -> {
                        _state.value = latest.copy(
                            hasLoaded = true,
                            loadFailed = latest.items.isEmpty(),
                            refreshing = false
                        )
                        _events.value = Event(NotificationEvent.LoadFailed)
                    }
                }
            }
    }

    /** Loads the next page; call when the last notification is on screen. */
    fun loadMore() {
        val s = current()
        if (!s.hasLoaded || s.loadFailed || s.loadingMore || s.refreshing || reloading) return
        if (exhausted || loadMoreFailed || !s.canLoadMore) return
        val myGeneration = generation
        _state.value = s.copy(loadingMore = true)
        api.getNotifications(AppConst.authHeader(), PAGE_SIZE, s.items.size)
            .enqueueResult<NotificationPage>(NotificationPage::class.java) { result ->
                if (myGeneration != generation) return@enqueueResult
                val latest = current()
                when (result) {
                    is ApiResult.Success -> {
                        val known = latest.items.mapNotNull { it.name }.toHashSet()
                        val fresh = result.data.rows().filter { known.add(it.name.orEmpty()) }
                        if (fresh.isEmpty()) exhausted = true
                        val all = latest.items + fresh
                        publishUnread(result.data.unread())
                        _state.value = latest.copy(
                            items = all,
                            total = maxOf(result.data.totalCount(), all.size),
                            unread = result.data.unread(),
                            loadingMore = false
                        )
                    }
                    is ApiResult.Failure -> {
                        loadMoreFailed = true
                        _state.value = latest.copy(loadingMore = false)
                        _events.value = Event(NotificationEvent.LoadFailed)
                    }
                }
            }
    }

    /** Marks every notification read once HRIS confirms, and clears the unread badge. */
    fun markAllRead() {
        val s = current()
        if (s.unread <= 0 || s.updating) return
        _state.value = s.copy(updating = true)
        api.markAllNotificationsRead(AppConst.authHeader())
            .enqueueResult<Any>(Any::class.java) { result ->
                val latest = current()
                when (result) {
                    is ApiResult.Success -> {
                        publishUnread(0)
                        _state.value = latest.copy(
                            unread = 0,
                            updating = false,
                            items = latest.items.map { it.asRead() }
                        )
                        _events.value = Event(NotificationEvent.MarkedRead(result.message))
                    }
                    is ApiResult.Failure -> {
                        _state.value = latest.copy(updating = false)
                        _events.value = Event(NotificationEvent.UpdateFailed)
                    }
                }
            }
    }

    private fun publishUnread(count: Int) {
        AppState.unreadNotifications.value = count
    }

    companion object {
        const val PAGE_SIZE = 20
    }
}
