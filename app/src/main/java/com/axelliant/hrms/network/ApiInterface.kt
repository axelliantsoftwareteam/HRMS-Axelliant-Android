package com.axelliant.hrms.network

import com.axelliant.hrms.di.TestModelInjection
import com.axelliant.hrms.model.attendance.AttRequest
import com.axelliant.hrms.model.leave.LeaveApproval
import com.axelliant.hrms.model.login.CheckInRequest
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
    @POST("hrms.hr.doctype.employee.mobile_api.get_month_attendance_stats")
    fun callAttendanceMonthStats(@Body attendanceRequest: AttRequest): Call<ResponseBody> // month attendance stats


    // Check-In Request
    @Headers("Content-Type: application/json")
    @POST("hrms.hr.doctype.employee.checkin.create_checkin")
    fun callCheckIn(@Body checkInRequest: CheckInRequest?): Call<ResponseBody> // month attendance stats



    @Headers("Content-Type: application/json")
    @POST("hrms.hr.doctype.employee.mobile_api.get_filtered_attendance") // my attendance detail
    fun callAttendanceDetail(@Body attendanceRequest:AttRequest

    ): Call<ResponseBody>


    @Headers("Content-Type: application/json")
    @POST("hrms.hr.doctype.employee.mobile_api.get_team_attendance")  // team attendance detail
    fun callTeamAttendanceDetail(@Body attendanceRequest:AttRequest
    ): Call<ResponseBody>


    @Headers("Content-Type: application/json")
    @POST("hrms.hr.doctype.employee.leaves_mobile.get_leave_details_self") // leave stats weekly and monthly
    fun callLeaveStats(@Body attRequest: AttRequest): Call<ResponseBody>



    @Headers("Content-Type: application/json")
    @POST("hrms.hr.doctype.employee.leaves_mobile.get_leave_application_requests") // my leave detail
    fun callMyLeaveDetail(@Body attRequest: AttRequest): Call<ResponseBody>


    @Headers("Content-Type: application/json")
    @POST("hrms.hr.doctype.employee.leaves_mobile.get_leave_application_requests_team") // team leave detail fragment
    fun callTeamLeaveDetail(@Body attRequest: AttRequest): Call<ResponseBody>

    // required APIS


    @Headers("Content-Type: application/json")
    @GET("hrms.hr.doctype.employee.leaves_mobile.get_leave_type")  // Leave types for request section
    fun getLeaveTypes(): Call<ResponseBody>




    @Headers("Content-Type: application/json")
    @GET("hrms.hr.doctype.employee.checkin.get_checkin_select_field")  // Attendance types for request section
    fun getAttendanceRequestInformation(): Call<ResponseBody>


    @Headers("Content-Type: application/json")
    @POST("hrms.hr.doctype.employee.leaves_mobile.create_leave_application")                              // post leave request
    fun postLeaveRequest(@Body leaveRequest: LeaveRequest?): Call<ResponseBody>


    @Headers("Content-Type: application/json")
    @POST("hrms.hr.doctype.employee.checkin.create_checkin")                             // attendance leave request
    fun postAttendanceRequest(@Body attendanceRequest: AttendanceRequest?): Call<ResponseBody>



    // User Google Sign-In Registration
    @Headers("Content-Type: application/json")
    @POST("login_microsoft/")                               // microsoft login
    fun userLoginCall(@Body loginRequest: LoginRequest?): Call<ResponseBody>


    @Headers("Content-Type: application/json")
    @POST("hrms.hr.doctype.employee.leaves_mobile.update_leave_application")                              // update leave request
    fun updateLeaveRequest(@Body leaveRequest: LeaveRequest?): Call<ResponseBody>

    @Headers("Content-Type: application/json")
    @POST("hrms.hr.doctype.employee.leaves_mobile.removed_leave_application")                              // update leave request
    fun deleteLeaveRequest(@Body leaveRequest: LeaveRequest?): Call<ResponseBody>



    @Headers("Content-Type: application/json")
    @POST("hrms.hr.doctype.employee.leaves_mobile.update_leave_status") // leave approval
    fun callLeaveApproval(@Body attRequest: LeaveApproval): Call<ResponseBody>

    @Headers("Content-Type: application/json")
    @POST("hrms.hr.doctype.employee.checkin.get_checkin_approvals")  // attendance approval
    fun callAttendanceApproval(@Body attendanceRequest:AttRequest
    ): Call<ResponseBody>


    @Headers("Content-Type: application/json")
    @POST("hrms.hr.doctype.employee.checkin.update_checkin_approval_status")  // attendance approval rejection
    fun callAttendanceApprovalStatus(@Body attendanceRequest:LeaveApproval
    ): Call<ResponseBody>



    @Headers("Content-Type: application/json")
    @POST("hrms.hr.doctype.employee.checkin.get_checkin_request") // get check in list
    fun callCheckInList(@Body attendanceRequest:AttRequest

    ): Call<ResponseBody>



    @Headers("Content-Type: application/json")
    @POST("hrms.hr.doctype.employee.checkin.update_checkin_request")                              // update attendance request
    fun updateAttendanceRequest(@Body leaveRequest: AttendanceRequest?): Call<ResponseBody>


    @Headers("Content-Type: application/json")
    @POST("hrms.hr.doctype.employee.checkin.removed_checkin")                              // delete attendance request
    fun deleteAttendanceRequest(@Body leaveRequest: LeaveRequest?): Call<ResponseBody>

}