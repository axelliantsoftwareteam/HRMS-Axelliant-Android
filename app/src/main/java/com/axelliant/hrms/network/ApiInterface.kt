package com.axelliant.hrms.network

import com.axelliant.hrms.model.attendance.AttRequest
import com.axelliant.hrms.model.login.CheckInRequest
import com.axelliant.hrms.model.login.LoginRequest
import com.axelliant.hrms.model.post.AttendanceRequest
import com.axelliant.hrms.model.post.LeaveRequest
import okhttp3.ResponseBody;
import retrofit2.Call
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header
import retrofit2.http.Headers;
import retrofit2.http.POST;

interface ApiInterface {

    @Headers("Content-Type: application/json")
    @GET("hrms.hr.doctype.employee.mobile_api.mobile_dashboard")  // dashboard
    fun callDashBoard(@Header("Authorization") auth: String?): Call<ResponseBody>

    // get attendance stats weekly
    @Headers("Content-Type: application/json")
    @GET("hrms.hr.doctype.employee.mobile_api.get_week_attendance_stats")
    fun callAttendanceWeekStats(@Header("Authorization") auth: String?): Call<ResponseBody> // week attendance stats

    // get attendance stats monthly
    @Headers("Content-Type: application/json")
    @POST("hrms.hr.doctype.employee.mobile_api.get_month_attendance_stats")
    fun callAttendanceMonthStats(@Header("Authorization") auth: String?,@Body attendanceRequest: AttRequest): Call<ResponseBody> // month attendance stats


    // Check-In Request
    @Headers("Content-Type: application/json")
    @POST("hrms.hr.doctype.employee.checkin.create_checkin")
    fun callCheckIn(@Header("Authorization") auth: String?,@Body checkInRequest: CheckInRequest?): Call<ResponseBody> // month attendance stats



    @Headers("Content-Type: application/json")
    @POST("hrms.hr.doctype.employee.mobile_api.get_filtered_attendance") // my attendance detail
    fun callAttendanceDetail(@Header("Authorization") auth: String?,@Body attendanceRequest:AttRequest

    ): Call<ResponseBody>


    @Headers("Content-Type: application/json")
    @POST("hrms.hr.doctype.employee.mobile_api.get_team_attendance")  // team attendance detail
    fun callTeamAttendanceDetail(@Header("Authorization") auth: String?,@Body attendanceRequest:AttRequest
    ): Call<ResponseBody>


    @Headers("Content-Type: application/json")
    @POST("hrms.hr.doctype.employee.leaves_mobile.get_leave_details_self") // leave stats weekly and monthly
    fun callLeaveStats(@Header("Authorization") auth: String?,@Body attRequest: AttRequest): Call<ResponseBody>



    @Headers("Content-Type: application/json")
    @POST("hrms.hr.doctype.employee.leaves_mobile.get_leave_application_requests") // my leave detail
    fun callMyLeaveDetail(@Header("Authorization") auth: String?,@Body attRequest: AttRequest): Call<ResponseBody>


    @Headers("Content-Type: application/json")
    @POST("hrms.hr.doctype.employee.leaves_mobile.get_leave_application_requests_team") // team leave detail fragment
    fun callTeamLeaveDetail(@Header("Authorization") auth: String?,@Body attRequest: AttRequest): Call<ResponseBody>

    // required APIS


    @Headers("Content-Type: application/json")
    @GET("hrms.hr.doctype.employee.leaves_mobile.get_leave_type")  // Leave types for request section
    fun getLeaveTypes(@Header("Authorization") auth: String?): Call<ResponseBody>




    @Headers("Content-Type: application/json")
    @GET("hrms.hr.doctype.employee.checkin.get_checkin_select_field")  // Attendance types for request section
    fun getAttendanceRequestInformation(@Header("Authorization") auth: String?): Call<ResponseBody>


    @Headers("Content-Type: application/json")
    @POST("hrms.hr.doctype.employee.leaves_mobile.create_leave_application")                              // post leave request
    fun postLeaveRequest(@Header("Authorization") auth: String?,@Body leaveRequest: LeaveRequest?): Call<ResponseBody>


    @Headers("Content-Type: application/json")
    @POST("hrms.hr.doctype.employee.checkin.create_checkin")                             // attendance leave request
    fun postAttendanceRequest(@Header("Authorization") auth: String?,@Body attendanceRequest: AttendanceRequest?): Call<ResponseBody>



    //microsoft login
    @Headers("Content-Type: application/json")
    @POST("hrms.hr.doctype.employee.mobile_api.get_set_user_token")
    fun userLoginCall(@Body loginRequest: LoginRequest?): Call<ResponseBody>



}