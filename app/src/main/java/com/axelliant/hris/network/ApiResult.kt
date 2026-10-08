package com.axelliant.hris.network

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import com.axelliant.hris.config.AppConst
import com.axelliant.hris.event.Event
import com.axelliant.hris.event.EventObserver
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Response
import java.lang.reflect.Type

/** Outcome of an HRIS call: the parsed payload or a user-readable message. */
sealed class ApiResult<out T> {
    data class Success<T>(val data: T, val message: String? = null) : ApiResult<T>()
    data class Failure(val message: String, val statusCode: Int? = null) : ApiResult<Nothing>()
}

/**
 * Reads the Frappe envelope `{ "message": { "data": { ...payload, "meta": {...} }, "meta": {...} } }`.
 * Some methods put the payload straight into `message` (no `data`), and some report a failed call as
 * HTTP 200 with `meta.status == false` / `meta.status_code == 401`; this handles all of them.
 */
object ApiParser {
    private val gson = Gson()

    fun <T> parse(raw: String?, type: Type): ApiResult<T> {
        if (raw.isNullOrBlank()) return ApiResult.Failure(ErrorMessages.UnknownError.errorString)
        val root: JsonElement = try {
            JsonParser.parseString(raw)
        } catch (e: Exception) {
            return ApiResult.Failure(ErrorMessages.UnknownError.errorString)
        }
        if (!root.isJsonObject) return ApiResult.Failure(ErrorMessages.UnknownError.errorString)
        val message = root.asJsonObject.get("message")
        val payload: JsonElement = when {
            message == null || message.isJsonNull -> root
            message.isJsonObject && message.asJsonObject.get("data")?.let { !it.isJsonNull } == true ->
                message.asJsonObject.get("data")
            else -> message
        }
        val meta = (payload as? JsonObject)?.getAsJsonObject("meta")
            ?: (message as? JsonObject)?.getAsJsonObject("meta")
        val statusCode = meta?.get("status_code")?.takeIf { it.isJsonPrimitive }?.asString?.toDoubleOrNull()?.toInt()
        val metaText = meta?.get("message")?.takeIf { it.isJsonPrimitive }?.asString
        if (statusCode == 401) {
            AppConst.observableCode.set(401)
            return ApiResult.Failure(ErrorMessages.SessionExpired401.errorString, 401)
        }
        val okFlag = meta?.get("status")?.takeIf { it.isJsonPrimitive }?.asBoolean
        if (okFlag == false) {
            return ApiResult.Failure(metaText ?: ErrorMessages.UnknownError.errorString, statusCode)
        }
        return try {
            val data: T = gson.fromJson(payload, type)
            ApiResult.Success(data, (payload as? JsonObject)?.get("status_message")?.takeIf { it.isJsonPrimitive }?.asString ?: metaText)
        } catch (e: Exception) {
            ApiResult.Failure(ErrorMessages.UnknownError.errorString)
        }
    }
}

/** Enqueues a Retrofit call and delivers an [ApiResult] on the main thread (401s feed the global session observer). */
fun <T> Call<ResponseBody>.enqueueResult(type: Type, onResult: (ApiResult<T>) -> Unit) {
    enqueue(object : BaseCallBack<ResponseBody>(this) {
        override fun onFinalSuccess(call: Call<ResponseBody>, response: Response<ResponseBody>) {
            val raw = try {
                response.body()?.string()
            } catch (e: Exception) {
                null
            }
            onResult(ApiParser.parse(raw, type))
        }

        override fun onFinalFailure(errorString: String?) {
            onResult(ApiResult.Failure(errorString ?: ErrorMessages.UnknownError.errorString))
        }
    })
}

/**
 * Observes a ViewModel result stream once per event. [onError] defaults to a no-op so screens can
 * decide whether to toast or show an inline error card.
 */
fun <T> LiveData<Event<ApiResult<T>>>.observeResult(
    owner: LifecycleOwner,
    onError: (String) -> Unit = {},
    onSuccess: (T, String?) -> Unit
) {
    observe(owner, EventObserver { result ->
        when (result) {
            is ApiResult.Success -> onSuccess(result.data, result.message)
            is ApiResult.Failure -> onError(result.message)
        }
    })
}
