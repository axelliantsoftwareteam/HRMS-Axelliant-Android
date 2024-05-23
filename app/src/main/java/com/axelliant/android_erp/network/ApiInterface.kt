package com.axelliant.android_erp.network

import com.axelliant.android_erp.di.TestModelInjection
import okhttp3.ResponseBody;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query

interface ApiInterface {

    // Screen Request
    @Headers("Content-Type: application/json")
    @GET("screen-text/{id}/")
    fun getScreen(@Path("id") id: Int): retrofit2.Call<ResponseBody>

    // User Registration
    @Headers("Content-Type: application/json")
    @POST("register/")
    fun userRegCall(@Body userRegistration: TestModelInjection?): retrofit2.Call<ResponseBody>

    @Headers("Content-Type: application/json")
    @GET("hrms.hr.doctype.employee.mobile_api.mobile_dashboard")
    fun callDashBoard(): retrofit2.Call<ResponseBody>

    // get attendance stats weekly
    @Headers("Content-Type: application/json")
    @GET("hrms.hr.doctype.employee.mobile_api.get_week_attendance_stats")
    fun callAttendanceWeekStats(): retrofit2.Call<ResponseBody>

    // get attendance stats monthly
    @Headers("Content-Type: application/json")
    @GET("hrms.hr.doctype.employee.mobile_api.get_month_attendance_stats")
    fun callAttendanceMonthStats(): retrofit2.Call<ResponseBody>


    // get attendance stats weekly
    @Headers("Content-Type: application/json")
    @GET("hrms.hr.doctype.employee.mobile_api.get_week_attendance_stats")
    fun callLeaveWeekStats(): retrofit2.Call<ResponseBody>


    // get attendance stats weekly
    @Headers("Content-Type: application/json")
    @GET("hrms.hr.doctype.employee.mobile_api.get_week_attendance_stats")
    fun callLeaveMonthStats(): retrofit2.Call<ResponseBody>


    @Headers("Content-Type: application/json")
    @GET("hrms.hr.doctype.employee.mobile_api.get_filtered_attendance")
    fun callAttendanceDetail(@Query("start_date") start_date: String="2024-01-01",
                             @Query("end_date") end_date: String = "2024-05-20",
                             @Query("employee_list") employee_list:List<String> = listOf("HR-EMP-00744")
    ): retrofit2.Call<ResponseBody>



//    "employee_list": ["HR-EMP-00744"]



}