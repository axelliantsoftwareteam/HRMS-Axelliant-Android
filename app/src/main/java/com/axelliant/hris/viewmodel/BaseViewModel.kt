package com.axelliant.hris.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.axelliant.hris.event.Event
import com.axelliant.hris.loading.ListLoadKind
import com.axelliant.hris.loading.ListLoadState
import com.axelliant.hris.loading.ListLoadTicket
import com.axelliant.hris.loading.ListLoadTracker
import com.axelliant.hris.network.ApiResult
import com.axelliant.hris.network.ErrorMessages
import com.axelliant.hris.network.enqueueResult
import okhttp3.ResponseBody
import retrofit2.Call
import java.lang.reflect.Type

open class BaseViewModel : ViewModel() {
    private data class ActiveCall(val blocking: Boolean, val listKey: String? = null, val ticket: ListLoadTicket? = null)
    private val activeCalls = mutableMapOf<Call<ResponseBody>, ActiveCall>()
    private val listStates = mutableMapOf<String, MutableLiveData<ListLoadState>>()
    private val trackers = mutableMapOf<String, ListLoadTracker>()
    private val _blockingLoading = MutableLiveData(false)
    private var blockingCount = 0

    /** Compatibility bridge for screens not yet migrated. New action observers use [blockingLoading]. */
    val isLoading: MutableLiveData<Event<Boolean>> by lazy { MutableLiveData() }
    fun getIsLoading(): LiveData<Event<Boolean>> = isLoading
    val blockingLoading: LiveData<Boolean> get() = _blockingLoading

    /** Separate keys isolate multiple lists/lookups owned by the same ViewModel. */
    fun listLoading(key: String = "list"): LiveData<ListLoadState> =
        listStates.getOrPut(key) { MutableLiveData(ListLoadState()) }

    private fun tracker(key: String): ListLoadTracker = trackers.getOrPut(key) {
        listLoading(key)
        ListLoadTracker { listStates.getValue(key).value = it }
    }

    /** Batch callers begin once, merge their responses, then finish with the displayed count. */
    protected fun beginListLoad(key: String = "list", kind: ListLoadKind = ListLoadKind.INITIAL,
                                replace: Boolean = false): ListLoadTicket {
        if (replace) activeCalls.filterValues { it.listKey == key }.keys.toList().forEach(::cancelRequest)
        return tracker(key).begin(kind, replace)
    }

    protected fun finishListLoad(key: String, ticket: ListLoadTicket, itemCount: Int? = null, error: String? = null) =
        tracker(key).finish(ticket, itemCount, error)

    protected fun isCurrentListLoad(key: String, ticket: ListLoadTicket): Boolean = tracker(key).isCurrent(ticket)

    /** Registers one cancellable member of an aggregate load without ending the aggregate ticket. */
    protected fun <T> requestListPart(call: Call<ResponseBody>, type: Type, key: String,
                                      ticket: ListLoadTicket, complete: (ApiResult<T>) -> Unit) {
        activeCalls[call] = ActiveCall(false, key, ticket)
        enqueue<T>(call, type) { result ->
            if (activeCalls.remove(call) == null || !isCurrentListLoad(key, ticket)) return@enqueue
            complete(result)
        }
    }

    /** Optional lookups have their own lifetime and never alter the list's progress/error state. */
    protected fun <T> requestData(call: Call<ResponseBody>, type: Type, complete: (ApiResult<T>) -> Unit) {
        activeCalls[call] = ActiveCall(false)
        enqueue<T>(call, type) { result ->
            if (activeCalls.remove(call) != null) complete(result)
        }
    }

    protected fun clearListLoading(key: String = "list") {
        activeCalls.filterValues { it.listKey == key }.keys.toList().forEach(::cancelRequest)
        tracker(key).clear()
    }

    /** A list read never opens the blocking action dialog. Replacement rejects obsolete callbacks. */
    protected fun <T> requestList(call: Call<ResponseBody>, type: Type,
                                  target: MutableLiveData<Event<ApiResult<T>>>, itemCount: (T) -> Int,
                                  key: String = "list", kind: ListLoadKind = ListLoadKind.INITIAL,
                                  replace: Boolean = true) {
        val ticket = beginListLoad(key, kind, replace)
        activeCalls[call] = ActiveCall(false, key, ticket)
        enqueue<T>(call, type) { result ->
            activeCalls.remove(call)
            if (!tracker(key).isCurrent(ticket)) return@enqueue
            val outcome: ApiResult<T> = when (result) {
                is ApiResult.Success -> try {
                    tracker(key).finish(ticket, itemCount(result.data))
                    result
                } catch (_: Exception) {
                    val failure = ApiResult.Failure(ErrorMessages.UnknownError.errorString)
                    tracker(key).finish(ticket, error = failure.message)
                    failure
                }
                is ApiResult.Failure -> {
                    tracker(key).finish(ticket, error = result.message)
                    result
                }
            }
            target.value = Event(outcome)
        }
    }

    /** Legacy reads/actions: overlapping requests now keep progress visible until all have ended. */
    protected fun <T> request(call: Call<ResponseBody>, type: Type,
                              target: MutableLiveData<Event<ApiResult<T>>>, showLoading: Boolean = true) {
        activeCalls[call] = ActiveCall(showLoading)
        if (showLoading) updateBlocking(1)
        enqueue<T>(call, type) { result ->
            val active = activeCalls.remove(call) ?: return@enqueue
            if (active.blocking) updateBlocking(-1)
            target.value = Event(result)
        }
    }

    /** Overrides allow deterministic scheduling tests without real network/Android dispatchers. */
    protected open fun <T> enqueue(call: Call<ResponseBody>, type: Type, complete: (ApiResult<T>) -> Unit) =
        call.enqueueResult(type, complete)

    protected fun cancelRequest(call: Call<ResponseBody>) {
        val active = activeCalls.remove(call) ?: return
        if (active.blocking) updateBlocking(-1)
        if (active.listKey != null && active.ticket != null) tracker(active.listKey).cancel(active.ticket)
        call.cancel()
    }

    private fun updateBlocking(delta: Int) {
        blockingCount += delta
        check(blockingCount >= 0)
        val busy = blockingCount > 0
        if (_blockingLoading.value != busy) {
            _blockingLoading.value = busy
            isLoading.value = Event(busy)
        }
    }

    override fun onCleared() {
        activeCalls.keys.toList().forEach(::cancelRequest)
        trackers.values.forEach { it.clear() }
        super.onCleared()
    }
}
