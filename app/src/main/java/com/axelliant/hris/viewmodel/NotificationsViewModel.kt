package com.axelliant.hris.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.axelliant.hris.config.AppConst
import com.axelliant.hris.config.AppState
import com.axelliant.hris.loading.ListLoadKind
import com.axelliant.hris.event.Event
import com.axelliant.hris.model.notification.NotificationPage
import com.axelliant.hris.model.notification.WorkflowNotification
import com.axelliant.hris.network.ApiInterface
import com.axelliant.hris.network.ApiResult

/** What happened that deserves a toast. */
sealed class NotificationEvent {
    object LoadFailed : NotificationEvent()
    object UpdateFailed : NotificationEvent()
    /** [message] is HRIS's own confirmation text, if it sent one. */
    data class MarkedRead(val message: String?) : NotificationEvent()
}

/** Notification feed (20 per page) and "mark all read"; also feeds the unread badge on Home and the drawer. */
open class NotificationsViewModel(private val api: ApiInterface) : BaseViewModel() {

    internal data class UnreadPayload(val unread_count: Any? = null)

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
        val revision = AppState.unreadRevision
        requestData<UnreadPayload>(api.getNotifications(AppConst.authHeader(), 1, 0), UnreadPayload::class.java) { result ->
                if (result is ApiResult.Success && revision == AppState.unreadRevision) {
                    val count = result.data.unread_count?.toString()?.toDoubleOrNull()?.toInt() ?: 0
                    AppState.unreadNotifications.value = count
                }
            }
    }

    /** Loads the first page again, replacing the list. [pull] is true for pull-to-refresh. */
    fun reload(pull: Boolean = false) {
        val before = current()
        if (before.updating) return
        if (reloading && !pull) return
        reloading = true
        loadMoreFailed = false
        generation++
        val myGeneration = generation
        val ticket = beginListLoad(kind = if (pull) ListLoadKind.PULL_REFRESH else ListLoadKind.REFRESH, replace = true)
        _state.value = before.copy(refreshing = pull, loadingMore = false)
        requestListPart<NotificationPage>(api.getNotifications(AppConst.authHeader(), PAGE_SIZE, 0), NotificationPage::class.java, "list", ticket) { result ->
                if (myGeneration != generation) return@requestListPart
                reloading = false

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
                            refreshing = false, loadingMore = false
                        )
                    }
                    is ApiResult.Failure -> {
                        _state.value = latest.copy(
                            hasLoaded = true,
                            loadFailed = latest.items.isEmpty(),
                            refreshing = false, loadingMore = false
                        )

                    }
                }
                finishListLoad("list", ticket, itemCount = if (result is ApiResult.Success) current().items.size else null,
                    error = (result as? ApiResult.Failure)?.message)
            }
    }

    /** Retries a failed page at the same offset; ordinary reloads still start at zero. */
    fun retryLoad() {
        if (loadMoreFailed) { loadMoreFailed = false; loadMore() } else reload()
    }

    /** Loads the next page; call when the last notification is on screen. */
    fun loadMore() {
        val s = current()
        if (!s.hasLoaded || s.loadFailed || s.loadingMore || s.refreshing || reloading || s.updating) return
        if (exhausted || loadMoreFailed || !s.canLoadMore) return
        val myGeneration = generation
        val ticket = beginListLoad(kind = ListLoadKind.PAGE)
        _state.value = s.copy(loadingMore = true)
        requestListPart<NotificationPage>(api.getNotifications(AppConst.authHeader(), PAGE_SIZE, s.items.size), NotificationPage::class.java, "list", ticket) { result ->
                if (myGeneration != generation) return@requestListPart
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

                    }
                }
                finishListLoad("list", ticket, itemCount = if (result is ApiResult.Success) current().items.size else null,
                    error = (result as? ApiResult.Failure)?.message)
            }
    }

    /** Marks every notification read once HRIS confirms, and clears the unread badge. */
    fun markAllRead() {
        val s = current()
        if (s.unread <= 0 || s.updating) return
        AppState.invalidateUnreadFetches()
        cancelListLoads()
        reloading = false
        generation++
        _state.value = s.copy(updating = true, refreshing = false, loadingMore = false)
        requestData<Any>(api.markAllNotificationsRead(AppConst.authHeader()), Any::class.java) { result ->
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
