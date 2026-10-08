package com.axelliant.hris.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.axelliant.hris.config.AppConst
import com.axelliant.hris.event.Event
import com.axelliant.hris.model.approval.ApprovalActionItem
import com.axelliant.hris.model.approval.ApprovalActionRequest
import com.axelliant.hris.model.approval.BulkApprovalActionRequest
import com.axelliant.hris.model.approvals.ApprovalActionResult
import com.axelliant.hris.model.approvals.ApprovalItem
import com.axelliant.hris.model.approvals.ApprovalsPage
import com.axelliant.hris.model.approvals.BulkApprovalResult
import com.axelliant.hris.model.team.TeamNum
import com.axelliant.hris.network.ApiInterface
import com.axelliant.hris.network.ApiResult
import com.axelliant.hris.network.enqueueResult

/** A toast the screen should show. */
data class ApprovalsMessage(val text: String, val isError: Boolean)

/**
 * The manager's pending approvals: every kind of request in one list, a page at a time, with
 * approve/reject for one request or for every visible one.
 */
class ApprovalsViewModel(private val api: ApiInterface) : BaseViewModel() {

    var items: List<ApprovalItem> = emptyList()
        private set
    var total = 0
        private set
    var pageIndex = 0
        private set

    /** True while an approve/reject call runs (disables the buttons). */
    var isActing = false
        private set

    /** Why the list could not load; shown with "Try again" until a load succeeds. */
    var loadError: String? = null
        private set
    var hasLoaded = false
        private set

    private var busy = false

    private val _changed = MutableLiveData<Unit>()
    val changed: LiveData<Unit> get() = _changed

    private val _message = MutableLiveData<Event<ApprovalsMessage>>()
    val message: LiveData<Event<ApprovalsMessage>> get() = _message

    /** The requests still waiting for a decision. */
    val visibleItems: List<ApprovalItem> get() = items.filter { it.isPending }

    val canGoBack: Boolean get() = pageIndex > 0 && !busy
    val canGoForward: Boolean get() = (pageIndex + 1) * PAGE_SIZE < total && !busy

    /** "1–10 of 24". */
    val pageRange: String
        get() {
            val first = if (total == 0) 0 else pageIndex * PAGE_SIZE + 1
            val last = minOf(total, pageIndex * PAGE_SIZE + items.size)
            return "$first–$last of $total"
        }

    /** Loads one page. [silent] skips the loading overlay (pull-to-refresh shows its own spinner). */
    fun load(page: Int = pageIndex, silent: Boolean = false, done: (() -> Unit)? = null) {
        if (page < 0 || busy) {
            done?.invoke()
            return
        }
        busy = true
        if (!silent) isLoading.value = Event(true)
        _changed.value = Unit
        api.getApprovals(AppConst.authHeader(), PAGE_SIZE, page * PAGE_SIZE)
            .enqueueResult<ApprovalsPage>(ApprovalsPage::class.java) { result ->
                busy = false
                if (!silent) isLoading.value = Event(false)
                hasLoaded = true
                when (result) {
                    is ApiResult.Success -> {
                        loadError = null
                        items = result.data.items.orEmpty()
                        total = TeamNum.int(result.data.total) ?: items.size
                        pageIndex = page
                        _changed.value = Unit
                        // Everything on the last page was handled: step back to the previous page.
                        if (items.isEmpty() && page > 0 && total > 0) {
                            load(page - 1, silent, done)
                            return@enqueueResult
                        }
                    }
                    is ApiResult.Failure -> {
                        loadError = result.message
                        _message.value = Event(ApprovalsMessage("We could not load your approvals right now.", true))
                        _changed.value = Unit
                    }
                }
                done?.invoke()
            }
    }

    fun movePage(offset: Int) {
        val next = pageIndex + offset
        if (next < 0 || next * PAGE_SIZE >= total) return
        load(next)
    }

    /** Approves or rejects one request, then reloads the current page. */
    fun act(item: ApprovalItem, approve: Boolean) {
        if (isActing || !item.isPending) return
        val type = item.approval_type
        val reference = item.reference_name
        if (type.isNullOrEmpty() || reference.isNullOrEmpty()) {
            _message.value = Event(ApprovalsMessage("This request is missing its reference, so it cannot be updated.", true))
            return
        }
        setActing(true)
        val fallback = if (approve) "We could not approve this request right now." else "We could not reject this request right now."
        api.takeApprovalAction(AppConst.authHeader(), ApprovalActionRequest(type, reference, statusOf(approve)))
            .enqueueResult<ApprovalActionResult>(ApprovalActionResult::class.java) { result ->
                setActing(false)
                when (result) {
                    is ApiResult.Success -> {
                        _message.value = Event(
                            ApprovalsMessage(
                                result.data.status_message?.takeIf { it.isNotBlank() }
                                    ?: if (approve) "Request has been approved." else "Request has been rejected.",
                                false
                            )
                        )
                        load(pageIndex)
                    }
                    is ApiResult.Failure -> _message.value = Event(ApprovalsMessage(result.message.ifBlank { fallback }, true))
                }
            }
    }

    /** Approves or rejects every visible pending request, then reports how many succeeded and why any failed. */
    fun actOnVisible(approve: Boolean) {
        if (isActing) return
        val actions = visibleItems.mapNotNull { item ->
            val type = item.approval_type
            val reference = item.reference_name
            if (type.isNullOrEmpty() || reference.isNullOrEmpty()) null else ApprovalActionItem(type, reference)
        }
        if (actions.isEmpty()) {
            _message.value = Event(ApprovalsMessage("There are no pending approvals to update.", true))
            return
        }
        setActing(true)
        val fallback = if (approve) "We could not approve the visible approvals right now." else "We could not reject the visible approvals right now."
        api.bulkTakeApprovalAction(AppConst.authHeader(), BulkApprovalActionRequest(actions, statusOf(approve)))
            .enqueueResult<BulkApprovalResult>(BulkApprovalResult::class.java) { result ->
                setActing(false)
                when (result) {
                    is ApiResult.Success -> {
                        report(result.data, actions.size, fallback)
                        load(pageIndex)
                    }
                    is ApiResult.Failure -> _message.value = Event(ApprovalsMessage(result.message.ifBlank { fallback }, true))
                }
            }
    }

    private fun report(result: BulkApprovalResult, count: Int, fallback: String) {
        val failedAll = result.meta?.status == false || (result.succeeded == 0 && result.failed > 0)
        val text: String
        val isError: Boolean
        when {
            failedAll -> {
                text = result.firstFailure ?: result.meta?.message ?: fallback
                isError = true
            }
            result.failed > 0 -> {
                val summary = "Updated ${result.succeeded} request(s); ${result.failed} failed."
                text = result.firstFailure?.let { "$summary $it" } ?: summary
                isError = true
            }
            else -> {
                text = result.status_message?.takeIf { it.isNotBlank() }
                    ?: "Processed $count approval request(s) has been completed."
                isError = false
            }
        }
        _message.value = Event(ApprovalsMessage(text, isError))
    }

    private fun setActing(value: Boolean) {
        isActing = value
        isLoading.value = Event(value)
        _changed.value = Unit
    }

    private fun statusOf(approve: Boolean) = if (approve) "Approved" else "Rejected"

    companion object {
        const val PAGE_SIZE = 10
    }
}
