package com.axelliant.android_erp.network

import com.axelliant.android_erp.di.TestModelInjection
import okhttp3.ResponseBody;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Path;

interface ApiInterface {

    // Screen Request
    @Headers("Content-Type: application/json")
    @GET("screen-text/{id}/")
    fun getScreen(@Path("id") id: Int): retrofit2.Call<ResponseBody>

    // User Registration
    @Headers("Content-Type: application/json")
    @POST("register/")
    fun userRegCall(@Body userRegistration: TestModelInjection?): retrofit2.Call<ResponseBody>
}