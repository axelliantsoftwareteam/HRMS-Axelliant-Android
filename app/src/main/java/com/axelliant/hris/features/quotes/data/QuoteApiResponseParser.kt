package com.axelliant.hris.features.quotes.data

import com.axelliant.hris.core.network.ApiMessage
import com.axelliant.hris.core.network.ApiResult
import com.axelliant.hris.core.network.BaseApiModel
import com.axelliant.hris.features.quotes.data.remote.dto.AddEditQuotationResponseDto
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.reflect.TypeToken

object QuoteApiResponseParser {

    fun <T> unwrapSuccess(payload: BaseApiModel<T>): T? {
        return if (payload.data?.success == true) payload.data.data else null
    }

    fun resolveErrorMessage(result: ApiResult<*>): String {
        return when (result) {
            is ApiResult.HttpError -> parseHttpErrorMessage(result)
            is ApiResult.NetworkError -> result.message
            is ApiResult.UnknownError -> result.message
            ApiResult.Unauthorized -> "Session expired. Please sign in again."
            ApiResult.Empty -> "No data found."
            is ApiResult.Success -> ""
        }
    }

    private fun parseHttpErrorMessage(error: ApiResult.HttpError): String {
        val body = error.errorBody.orEmpty()
        if (body.isNotBlank()) {
            runCatching {
                val gson = Gson()
                val typed = gson.fromJson<BaseApiModel<List<AddEditQuotationResponseDto>>>(
                    body,
                    object : TypeToken<BaseApiModel<List<AddEditQuotationResponseDto>>>() {}.type
                )
                extractApiMessage(typed)?.let { return it }

                val wrapped = gson.fromJson(body, BaseApiModel::class.java)
                extractApiMessage(wrapped)?.let { return it }
            }
        }
        return error.message.ifBlank { "Unable to complete request." }
    }

    fun extractApiMessage(payload: BaseApiModel<*>?): String? {
        if (payload == null) return null

        payload.data?.message
            ?.takeIf { it.isNotBlank() }
            ?.let { return it }

        (payload.data?.data as? AddEditQuotationResponseDto)
            ?.message
            ?.takeIf { it.isNotBlank() }
            ?.let { return it }

        (payload.data?.data as? List<*>)
            ?.asSequence()
            ?.filterIsInstance<AddEditQuotationResponseDto>()
            ?.mapNotNull { it.message?.takeIf(String::isNotBlank) }
            ?.firstOrNull()
            ?.let { return it }

        return payload.message?.displayText()
    }

    fun extractAddEditQuotationResponse(payload: BaseApiModel<*>): AddEditQuotationResponseDto? {
        val data = payload.data?.data ?: return null
        return when (data) {
            is AddEditQuotationResponseDto -> data
            is String -> AddEditQuotationResponseDto(id = data)
            is JsonElement -> data.toAddEditQuotationResponse()
            is List<*> -> data.filterIsInstance<AddEditQuotationResponseDto>().firstOrNull()
            else -> null
        }
    }

    private fun JsonElement.toAddEditQuotationResponse(): AddEditQuotationResponseDto? {
        if (isJsonPrimitive && asJsonPrimitive.isString) {
            return AddEditQuotationResponseDto(id = asString)
        }

        if (isJsonObject) {
            return runCatching {
                Gson().fromJson(this, AddEditQuotationResponseDto::class.java)
            }.getOrNull()
        }

        if (isJsonArray) {
            return asJsonArray.firstNotNullOfOrNull { element ->
                element.toAddEditQuotationResponse()
            }
        }

        return null
    }

    private fun ApiMessage.displayText(): String? {
        return text?.takeIf { it.isNotBlank() }
            ?: title?.takeIf { it.isNotBlank() }
    }
}
