package com.axelliant.hris.viewmodel

import androidx.arch.core.executor.ArchTaskExecutor
import androidx.arch.core.executor.TaskExecutor
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelStore
import com.axelliant.hris.event.Event
import com.axelliant.hris.loading.ListLoadKind
import com.axelliant.hris.loading.ListLoadState
import com.axelliant.hris.network.ApiResult
import java.lang.reflect.Type
import okhttp3.Request
import okhttp3.ResponseBody
import okio.Timeout
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class BaseLoadingTest {
    private lateinit var model: Probe
    private val store = ViewModelStore()

    @Before fun setup() {
        ArchTaskExecutor.getInstance().setDelegate(object : TaskExecutor() {
            override fun executeOnDiskIO(runnable: Runnable) = runnable.run()
            override fun postToMainThread(runnable: Runnable) = runnable.run()
            override fun isMainThread() = true
        })
        model = Probe()
        store.put("probe", model)
    }
    @After fun cleanup() { store.clear(); ArchTaskExecutor.getInstance().setDelegate(null) }

    @Test fun blockingCallsStayVisibleUntilEveryActionFinishes() {
        val first = model.action(); val second = model.action()
        model.complete(first, ApiResult.Success(1))
        assertEquals(true, model.blockingLoading.value)
        model.complete(second, ApiResult.Failure("failed"))
        assertEquals(false, model.blockingLoading.value)
    }

    @Test fun silentCallCannotDismissAnActionDialog() {
        val action = model.action(); val silent = model.action(false)
        model.complete(silent, ApiResult.Success(1))
        assertEquals(true, model.blockingLoading.value)
        model.complete(action, ApiResult.Success(1))
        assertEquals(false, model.blockingLoading.value)
    }

    @Test fun listReadDoesNotTriggerBlockingDialogAndStateReplaysToNewObserver() {
        val call = model.read()
        assertEquals(false, model.blockingLoading.value)
        var restored: ListLoadState? = null
        val observer = Observer<ListLoadState> { restored = it }
        model.listLoading().observeForever(observer)
        assertTrue(restored!!.initialLoading)
        model.listLoading().removeObserver(observer)
        model.complete(call, ApiResult.Success(5))
        model.listLoading().observeForever(observer)
        assertTrue(restored!!.hasContent)
        assertFalse(restored!!.isLoading)
        model.listLoading().removeObserver(observer)
    }

    @Test fun replacementCancelsOldReadAndRejectsLateResults() {
        val first = model.read(); val second = model.read()
        assertTrue(first.isCanceled)
        model.complete(first, ApiResult.Success(99))
        assertTrue(model.listLoading().value!!.initialLoading)
        assertNull(model.readResult.value)
        model.complete(second, ApiResult.Success(4))
        assertEquals(4, model.listLoading().value!!.itemCount)
    }

    @Test fun independentListsCannotDismissEachOthersIndicators() {
        val first = model.read("attendance"); val second = model.read("requests")
        model.complete(first, ApiResult.Success(2))
        assertFalse(model.listLoading("attendance").value!!.isLoading)
        assertTrue(model.listLoading("requests").value!!.isLoading)
        model.complete(second, ApiResult.Success(3))
    }

    @Test fun failedRefreshKeepsPreviousCount() {
        model.complete(model.read(), ApiResult.Success(3))
        val refresh = model.read(kind = ListLoadKind.FILTER)
        model.complete(refresh, ApiResult.Failure("offline"))
        assertEquals(3, model.listLoading().value!!.itemCount)
        assertEquals("offline", model.listLoading().value!!.error)
        assertFalse(model.listLoading().value!!.isLoading)
    }

    @Test fun malformedCountEndsLoadingWithFailure() {
        model.complete(model.read(), ApiResult.Success(-1))
        assertFalse(model.listLoading().value!!.isLoading)
        assertNotNull(model.listLoading().value!!.error)
        assertTrue(model.readResult.value?.peekContent() is ApiResult.Failure)
    }

    @Test fun clearedViewModelCancelsCallsAndRejectsLateActionCallbacks() {
        val action = model.action(); val read = model.read()
        store.clear()
        assertTrue(action.isCanceled)
        assertTrue(read.isCanceled)
        model.complete(action, ApiResult.Success(1))
        model.complete(read, ApiResult.Success(8))
        assertNull(model.actionResult.value)
        assertEquals(false, model.blockingLoading.value)
        assertEquals(ListLoadState(), model.listLoading().value)
    }

    private class Probe : BaseViewModel() {
        val actionResult = MutableLiveData<Event<ApiResult<Int>>>()
        val readResult = MutableLiveData<Event<ApiResult<Int>>>()
        private val callbacks = mutableMapOf<Call<ResponseBody>, (ApiResult<Int>) -> Unit>()
        fun action(blocking: Boolean = true) = FakeCall().also {
            request(it, Int::class.java, actionResult, blocking)
        }
        fun read(key: String = "list", kind: ListLoadKind = ListLoadKind.INITIAL) = FakeCall().also {
            requestList(it, Int::class.java, readResult, { count: Int -> count }, key, kind)
        }
        fun complete(call: Call<ResponseBody>, result: ApiResult<Int>) { callbacks.getValue(call)(result) }
        @Suppress("UNCHECKED_CAST")
        override fun <T> enqueue(call: Call<ResponseBody>, type: Type, complete: (ApiResult<T>) -> Unit) {
            callbacks[call] = complete as (ApiResult<Int>) -> Unit
        }
    }

    private class FakeCall : Call<ResponseBody> {
        private var cancelled = false
        override fun cancel() { cancelled = true }
        override fun isCanceled() = cancelled
        override fun isExecuted() = false
        override fun clone(): Call<ResponseBody> = FakeCall()
        override fun request(): Request = Request.Builder().url("https://example.invalid/").build()
        override fun timeout(): Timeout = Timeout.NONE
        override fun enqueue(callback: Callback<ResponseBody>) { error("Unexpected real dispatch") }
        override fun execute(): Response<ResponseBody> = error("Unexpected real dispatch")
    }
}
