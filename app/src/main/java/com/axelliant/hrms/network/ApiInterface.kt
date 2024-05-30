package com.axelliant.hrms.network

import com.axelliant.hrms.di.TestModelInjection
import com.axelliant.hrms.model.login.LoginRequest
import com.axelliant.hrms.model.post.AttendanceRequest
import com.axelliant.hrms.model.post.LeaveRequest
import okhttp3.ResponseBody;
import retrofit2.Call
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query

interface ApiInterface {

    @Headers("Content-Type: application/json")
    @GET("hrms.hr.doctype.employee.mobile_api.mobile_dashboard")  // dashboard
    fun callDashBoard(): Call<ResponseBody>

    // get attendance stats weekly
    @Headers("Content-Type: application/json")
    @GET("hrms.hr.doctype.employee.mobile_api.get_week_attendance_stats")
    fun callAttendanceWeekStats(): Call<ResponseBody> // week attendance stats

    // get attendance stats monthly
    @Headers("Content-Type: application/json")
    @GET("hrms.hr.doctype.employee.mobile_api.get_month_attendance_stats")
    fun callAttendanceMonthStats(): Call<ResponseBody> // month attendance stats


    @Headers("Content-Type: application/json")
    @GET("hrms.hr.doctype.employee.mobile_api.get_filtered_attendance") // my attendance detail
    fun callAttendanceDetail(@Query("start_date") start_date: String="2024-01-01",
                             @Query("end_date") end_date: String = "2024-05-20",
                             @Query("employee_list") employee_list:List<String> = listOf("HR-EMP-00744"),
                             @Query("filters") filters:String = ""

    ): Call<ResponseBody>


    @Headers("Content-Type: application/json")
    @GET("hrms.hr.doctype.employee.mobile_api.get_team_attendance")  // team attendance detail
    fun callTeamAttendanceDetail(@Query("start_date") start_date: String="2024-01-01",
                                 @Query("end_date") end_date: String = "2024-05-20"
    ): Call<ResponseBody>


    @Headers("Content-Type: application/json")
    @GET("hrms.hr.doctype.employee.leaves_mobile.get_leave_details_self") // leave stats weekly and monthly
    fun callLeaveStats(@Query("start_date") start_date: String="2024-01-01",
                       @Query("end_date") end_date: String = "2024-05-20"): Call<ResponseBody>



    @Headers("Content-Type: application/json")
    @GET("hrms.hr.doctype.employee.leaves_mobile.get_leave_application_requests") // my leave detail
    fun callMyLeaveDetail(@Query("start_date") start_date: String="2024-01-01",
                          @Query("end_date") end_date: String = "2024-05-20",
                          @Query("filters") filters:String = ""): Call<ResponseBody>


    // required APIS


    @Headers("Content-Type: application/json")
    @GET("team_leave_detail_fragment") // team leave detail fragment
    fun callTeamLeaveDetail(@Query("start_date") start_date: String="2024-01-01",
                            @Query("end_date") end_date: String = "2024-05-20"): Call<ResponseBody>


    @Headers("Content-Type: application/json")
    @POST("leave_request/")                              // post leave request
    fun postLeaveRequest(@Body leaveRequest: LeaveRequest?): Call<ResponseBody>


    @Headers("Content-Type: application/json")
    @POST("attendance_request/")                             // attendance leave request
    fun postAttendanceRequest(@Body attendanceRequest: AttendanceRequest?): Call<ResponseBody>



    // User Google Sign-In Registration
    @Headers("Content-Type: application/json")
    @POST("login_microsoft/")                               // microsoft login
    fun userLoginCall(@Body loginRequest: LoginRequest?): Call<ResponseBody>



}