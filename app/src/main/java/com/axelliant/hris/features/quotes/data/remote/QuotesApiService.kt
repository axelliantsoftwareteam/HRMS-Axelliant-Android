package com.axelliant.hris.features.quotes.data.remote

import com.axelliant.hris.core.network.BaseApiModel
import com.axelliant.hris.features.quotes.data.remote.dto.GetQuotationRequest
import com.axelliant.hris.features.quotes.data.remote.dto.GetQuotationResponse
import com.axelliant.hris.features.quotes.data.remote.dto.AddEditQuotationResponseDto
import com.axelliant.hris.features.quotes.data.remote.dto.QuotePreviewResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface QuotesApiService {
    @POST("Quotation/GetAll")
    suspend fun getAllQuotes(
        @Body request: GetQuotationRequest
    ): Response<BaseApiModel<GetQuotationResponse>>

    @GET("Quotation/GetPreview")
    suspend fun getQuotePreview(
        @Query("id") id: String
    ): Response<BaseApiModel<QuotePreviewResponse>>

    @GET("Quotation/GetSingle")
    suspend fun getQuoteSingle(
        @Query("id") id: String
    ): Response<BaseApiModel<QuotePreviewResponse>>

    @POST("Quotation/SubmitQuote")
    suspend fun submitQuote(
        @Query("id") id: String
    ): Response<BaseApiModel<List<AddEditQuotationResponseDto>>>

    @GET("Quotation/Cancel")
    suspend fun cancelQuote(
        @Query("id") id: String
    ): Response<BaseApiModel<Unit>>
}
