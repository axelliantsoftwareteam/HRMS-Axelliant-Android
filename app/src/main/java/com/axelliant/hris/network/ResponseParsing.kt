package com.axelliant.hris.network

import android.os.Handler
import android.os.Looper
import com.axelliant.hris.config.AppConst
import com.axelliant.hris.config.SessionVersion
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Response
import java.lang.reflect.Type
import java.util.concurrent.Executors
import java.util.concurrent.Executor

/** Fixed concurrency for body reads/JSON decoding; UI/session events stay on main. */
internal object ResponseParsing {
    private val workers = Executors.newFixedThreadPool(2)
    private val main by lazy { Handler(Looper.getMainLooper()) }
    private val parser = BackgroundResponseParser(workers, Executor { main.post(it) }) {
        AppConst.observableCode.set(401)
    }

    fun <T> execute(call: Call<ResponseBody>, response: Response<ResponseBody>, type: Type,
                    session: String?,
                    version: Long,
                    deliver: (ApiResult<T>) -> Unit) {
        parser.parse({ response.body()?.use { if (call.isCanceled) null else it.string() } }, type,
            { call.isCanceled }, { session == AppConst.TOKEN && version == SessionVersion.current }, deliver)
    }
}

/** Testable scheduling boundary: all decoding happens on [worker], all side effects on [main]. */
internal class BackgroundResponseParser(
    private val worker: Executor,
    private val main: Executor,
    private val unauthorized: () -> Unit
) {
    fun <T> parse(read: () -> String?, type: Type, cancelled: () -> Boolean,
                  isCurrent: () -> Boolean, deliver: (ApiResult<T>) -> Unit) {
        worker.execute {
            val result: ApiResult<T> = try { ApiParser.parse(read(), type) }
            catch (_: Exception) { ApiResult.Failure(ErrorMessages.UnknownError.errorString) }
            main.execute {
                if (!cancelled() && isCurrent()) {
                    if (result is ApiResult.Failure && result.statusCode == 401) unauthorized()
                    deliver(result)
                }
            }
        }
    }
}
