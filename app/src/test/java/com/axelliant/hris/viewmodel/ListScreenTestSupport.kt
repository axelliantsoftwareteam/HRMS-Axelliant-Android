package com.axelliant.hris.viewmodel

import androidx.arch.core.executor.ArchTaskExecutor
import androidx.arch.core.executor.TaskExecutor
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import com.axelliant.hris.network.ApiInterface
import com.axelliant.hris.network.ApiResult
import java.lang.reflect.Proxy
import okhttp3.Request
import okhttp3.ResponseBody
import okio.Timeout
import org.junit.After
import org.junit.Before
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/** Captures calls and deliberately delivers cancelled callbacks to exercise stale-result guards. */
internal class ListNetworkHarness {
    val calls = mutableListOf<TestListCall>()
    private val callbacks = mutableMapOf<Call<ResponseBody>, (ApiResult<Any?>) -> Unit>()
    val api = Proxy.newProxyInstance(ApiInterface::class.java.classLoader, arrayOf(ApiInterface::class.java)) { _, method, args ->
        TestListCall(method.name, args?.toList().orEmpty()).also { calls += it }
    } as ApiInterface
    @Suppress("UNCHECKED_CAST")
    fun <T> register(call: Call<ResponseBody>, complete: (ApiResult<T>) -> Unit) {
        callbacks[call] = complete as (ApiResult<Any?>) -> Unit
    }
    fun complete(call: TestListCall, result: ApiResult<Any?>) = callbacks.getValue(call)(result)
    fun last(name: String) = calls.last { it.name == name }
}

internal class TestListCall(val name: String, val args: List<Any?>) : Call<ResponseBody> {
    var cancelled = false
        private set
    override fun cancel() { cancelled = true }
    override fun isCanceled() = cancelled
    override fun isExecuted() = false
    override fun clone(): Call<ResponseBody> = TestListCall(name, args)
    override fun request(): Request = Request.Builder().url("https://example.invalid/").build()
    override fun timeout(): Timeout = Timeout.NONE
    override fun enqueue(callback: Callback<ResponseBody>) { error("Unexpected real dispatch") }
    override fun execute(): Response<ResponseBody> = error("Unexpected real dispatch")
}

open class ListScreenTestSupport {
    private val store = ViewModelStore()
    private var nextKey = 0
    @Before fun installMainExecutor() {
        ArchTaskExecutor.getInstance().setDelegate(object : TaskExecutor() {
            override fun executeOnDiskIO(runnable: Runnable) = runnable.run()
            override fun postToMainThread(runnable: Runnable) = runnable.run()
            override fun isMainThread() = true
        })
    }
    @After fun releaseModels() { store.clear(); ArchTaskExecutor.getInstance().setDelegate(null) }
    protected fun <T : ViewModel> retain(model: T): T = model.also { store.put("list-${++nextKey}", it) }
    protected fun clearModels() = store.clear()
}
