package com.axelliant.hris.features.quotes.data.remote

import com.axelliant.hris.core.network.BaseApiModel
import com.axelliant.hris.features.quotes.data.remote.dto.AccountDdlDto
import com.axelliant.hris.features.quotes.data.remote.dto.AddEditQuotationRequest
import com.axelliant.hris.features.quotes.data.remote.dto.CustomerQuotationDetailsDto
import com.axelliant.hris.features.quotes.data.remote.dto.PaymentTermDdlDto
import com.google.gson.JsonElement
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface CreateQuoteApiService {
    @GET("Accounts/GetAllAccountsDDL")
    suspend fun getAllAccountsDdl(
        @Query("search") search: String,
        @Query("quoteType") quoteType: Int
    ): Response<BaseApiModel<List<AccountDdlDto>>>

    @GET("Customer/GetDetailsForQuotation")
    suspend fun getCustomerDetailsForQuotation(
        @Query("accountId") accountId: String
    ): Response<BaseApiModel<CustomerQuotationDetailsDto>>

    @GET("PaymentTerm/GetAllForDDL")
    suspend fun getPaymentTermsForDdl(
        @Query("type") type: Int,
        @Query("accountId") accountId: String
    ): Response<BaseApiModel<List<PaymentTermDdlDto>>>

    @POST("Quotation/AddEdit")
    suspend fun addEditQuotation(
        @Body request: AddEditQuotationRequest
    ): Response<BaseApiModel<JsonElement>>
}
