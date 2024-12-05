package com.axelliant.hris.network

import com.axelliant.hris.model.attendance.AttRequest
import com.axelliant.hris.model.attendance.LeaveCountRequest
import com.axelliant.hris.model.expense.CreateExpense
import com.axelliant.hris.model.expense.DeleteAttachment
import com.axelliant.hris.model.leave.ExpenseApprovalStatus
import com.axelliant.hris.model.leave.LeaveApproval
import com.axelliant.hris.model.leave.UpcomingLeaveInput
import com.axelliant.hris.model.leave.leaveCount.LeaveCountByDaysRequest
import com.axelliant.hris.model.login.CheckInRequest
import com.axelliant.hris.model.login.LoginRequest
import com.axelliant.hris.model.post.AttendanceRequest
import com.axelliant.hris.model.post.LeaveRequest
import com.axelliant.hris.model.post.TeamListRequest
import okhttp3.MultipartBody
import okhttp3.ResponseBody;
import retrofit2.Call
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header
import retrofit2.http.Headers;
import retrofit2.http.Multipart
import retrofit2.http.POST;
import retrofit2.http.Part

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
    fun callAttendanceMonthStats(
        @Header("Authorization") auth: String?,
        @Body attendanceRequest: AttRequest
    ): Call<ResponseBody> // month attendance stats


    // Check-In Request
    @Headers("Content-Type: application/json")
    @POST("hrms.hr.doctype.employee.checkin.create_checkin")
    fun callCheckIn(
        @Header("Authorization") auth: String?,
        @Body checkInRequest: CheckInRequest?
    ): Call<ResponseBody> // month attendance stats


    @Headers("Content-Type: application/json")
    @POST("hrms.hr.doctype.employee.mobile_api.get_filtered_attendance") // my attendance detail
    fun callAttendanceDetail(
        @Header("Authorization") auth: String?, @Body attendanceRequest: AttRequest

    ): Call<ResponseBody>


    @Headers("Content-Type: application/json")
    @POST("hrms.hr.doctype.employee.mobile_api.get_team_attendance")  // team attendance detail
    fun callTeamAttendanceDetail(
        @Header("Authorization") auth: String?, @Body attendanceRequest: AttRequest
    ): Call<ResponseBody>


    @Headers("Content-Type: application/json")
    @POST("hrms.hr.doctype.employee.leaves_mobile.get_leave_details_self") // leave stats weekly and monthly
    fun callLeaveStats(
        @Header("Authorization") auth: String?,
        @Body attRequest: AttRequest
    ): Call<ResponseBody>


    @Headers("Content-Type: application/json")
    @POST("hrms.hr.doctype.employee.leaves_mobile.get_leave_application_requests") // my leave detail
    fun callMyLeaveDetail(
        @Header("Authorization") auth: String?,
        @Body attRequest: AttRequest
    ): Call<ResponseBody>


    @Headers("Content-Type: application/json")
    @POST("hrms.hr.doctype.employee.expense.get_expense_claim_requests") // my leave detail
    fun callMyExpenseDetail(
        @Header("Authorization") auth: String?,
        @Body attRequest: AttRequest
    ): Call<ResponseBody>



    @Multipart
    @POST("hrms.hr.doctype.employee.expense.upload_file_attachment")
    fun callMyExpensefile(
        @Header("Authorization") auth: String?,
        @Part file: MultipartBody.Part,
        @Part docname: MultipartBody.Part,
        @Part is_private: MultipartBody.Part,
        @Part folder: MultipartBody.Part,
        @Part doctype: MultipartBody.Part
    ): Call<ResponseBody>


    @Headers("Content-Type: application/json")
    @POST("hrms.hr.doctype.employee.leaves_mobile.get_leave_application_requests_team") // team leave detail fragment
    fun callTeamLeaveDetail(
        @Header("Authorization") auth: String?,
        @Body attRequest: AttRequest
    ): Call<ResponseBody>

    @Headers("Content-Type: application/json")
    @POST("hrms.hr.doctype.employee.leaves_mobile.upcoming_leave_self") // team leave detail fragment
    fun callUpcomingLeaveDetail(
        @Header("Authorization") auth: String?,
        @Body upcomingLeaveInput: UpcomingLeaveInput
    ): Call<ResponseBody>

    // required APIS


    @Headers("Content-Type: application/json")
    @GET("hrms.hr.doctype.employee.leaves_mobile.get_leave_type")  // Leave types for request section
    fun getLeaveTypes(@Header("Authorization") auth: String?): Call<ResponseBody>


    @Headers("Content-Type: application/json")
    @GET("hrms.hr.doctype.employee.leaves_mobile.get_leaves_detail")  // Leave types for request section
    fun getLeaveTypesWithCount(@Header("Authorization") auth: String?): Call<ResponseBody>


    @Headers("Content-Type: application/json")
    @GET("hrms.hr.doctype.employee.checkin.get_checkin_select_field")  // Attendance types for request section
    fun getAttendanceRequestInformation(@Header("Authorization") auth: String?): Call<ResponseBody>


    @Headers("Content-Type: application/json")
    @POST("hrms.hr.doctype.employee.leaves_mobile.create_leave_application")                              // post leave request
    fun postLeaveRequest(
        @Header("Authorization") auth: String?,
        @Body leaveRequest: LeaveRequest?
    ): Call<ResponseBody>


    @Headers("Content-Type: application/json")
    @POST("hrms.hr.doctype.employee.checkin.create_checkin")                             // attendance leave request
    fun postAttendanceRequest(
        @Header("Authorization") auth: String?,
        @Body attendanceRequest: AttendanceRequest?
    ): Call<ResponseBody>


    //microsoft login
    @Headers("Content-Type: application/json")
    @POST("hrms.hr.doctype.employee.mobile_api.get_set_user_token")
    fun userLoginCall(@Body loginRequest: LoginRequest?): Call<ResponseBody>


    @Headers("Content-Type: application/json")
    @POST("hrms.hr.doctype.employee.leaves_mobile.update_leave_application")                              // update leave request
    fun updateLeaveRequest(
        @Header("Authorization") auth: String?,
        @Body leaveRequest: LeaveRequest?
    ): Call<ResponseBody>

    @Headers("Content-Type: application/json")
    @POST("hrms.hr.doctype.employee.leaves_mobile.removed_leave_application")                              // update leave request
    fun deleteLeaveRequest(
        @Header("Authorization") auth: String?,
        @Body leaveRequest: LeaveRequest?
    ): Call<ResponseBody>


    @Headers("Content-Type: application/json")
    @POST("hrms.hr.doctype.employee.leaves_mobile.get_leave_days")
    fun leaveCountRequest(
        @Header("Authorization") auth: String?,
        @Body leaveCountByDaysRequest: LeaveCountByDaysRequest?
    ): Call<ResponseBody>


    @Headers("Content-Type: application/json")
    @POST("hrms.hr.doctype.employee.leaves_mobile.update_leave_status") // leave approval
    fun callLeaveApproval(
        @Header("Authorization") auth: String?,
        @Body attRequest: LeaveApproval
    ): Call<ResponseBody>


    @Headers("Content-Type: application/json")
    @POST("hrms.hr.doctype.employee.expense.create_expenses") // create expense
    fun callCreateExp(
        @Header("Authorization") auth: String?,
        @Body createExpense: CreateExpense
    ): Call<ResponseBody>

    @Headers("Content-Type: application/json")
    @POST("hrms.hr.doctype.employee.expense.update_expense") // update expense
    fun callUpdateExp(
        @Header("Authorization") auth: String?,
        @Body createExpense: CreateExpense
    ): Call<ResponseBody>

    @Headers("Content-Type: application/json")
    @POST("hrms.hr.doctype.employee.expense.delete_expense_claim") // update expense
    fun deleteExpenseCall(
        @Header("Authorization") auth: String?,
        @Body createExpense: CreateExpense
    ): Call<ResponseBody>

    @Headers("Content-Type: application/json")
    @POST("hrms.hr.doctype.employee.expense.delete_attachment") // update expense
    fun deleteExpenseAttachmentCall(
        @Header("Authorization") auth: String?,
        @Body createExpense: DeleteAttachment
    ): Call<ResponseBody>


    @Headers("Content-Type: application/json")
    @POST("hrms.hr.doctype.employee.checkin.get_checkin_approvals")  // attendance approval
    fun callAttendanceApproval(
        @Header("Authorization") auth: String?, @Body attendanceRequest: AttRequest
    ): Call<ResponseBody>

    @Headers("Content-Type: application/json")
    @POST("hrms.hr.doctype.employee.expense.get_expense_approvals")  // attendance approval
    fun callExpenseApproval(
        @Header("Authorization") auth: String?, @Body attendanceRequest: AttRequest
    ): Call<ResponseBody>


    @Headers("Content-Type: application/json")
    @POST("hrms.hr.doctype.employee.checkin.update_checkin_approval_status")  // attendance approval rejection
    fun callAttendanceApprovalStatus(
        @Header("Authorization") auth: String?, @Body attendanceRequest: LeaveApproval
    ): Call<ResponseBody>


    @Headers("Content-Type: application/json")
    @POST("hrms.hr.doctype.employee.expense.change_status_of_expense")  // attendance approval rejection
    fun callExpenseApprovalStatus(
        @Header("Authorization") auth: String?, @Body attendanceRequest: ExpenseApprovalStatus
    ): Call<ResponseBody>


    @Headers("Content-Type: application/json")
    @POST("hrms.hr.doctype.employee.checkin.get_checkin_request") // get check in list
    fun callCheckInList(
        @Header("Authorization") auth: String?, @Body attendanceRequest: LeaveCountRequest

    ): Call<ResponseBody>


    @Headers("Content-Type: application/json")
    @POST("hrms.hr.doctype.employee.checkin.update_checkin_request")                              // update attendance request
    fun updateAttendanceRequest(
        @Header("Authorization") auth: String?,
        @Body leaveRequest: AttendanceRequest?
    ): Call<ResponseBody>


    @Headers("Content-Type: application/json")
    @POST("hrms.hr.doctype.employee.checkin.removed_checkin")                              // delete attendance request
    fun deleteAttendanceRequest(
        @Header("Authorization") auth: String?,
        @Body leaveRequest: LeaveRequest?
    ): Call<ResponseBody>


    @Headers("Content-Type: application/json")
    @GET("hrms.hr.doctype.employee.expense.get_expense_type")  // Leave types for request section
    fun getExpenseType(@Header("Authorization") auth: String?): Call<ResponseBody>


    @Headers("Content-Type: application/json")
    @GET("hrms.hr.doctype.employee.mobile_api.get_team_checkin_detail")  // dashboard
    fun callTeamInfo(@Header("Authorization") auth: String?): Call<ResponseBody>


    @Headers("Content-Type: application/json")
    @POST("hrms.hr.doctype.employee.mobile_api.get_team_checkin_detail_list")  // dashboard
    fun callTodayTeamList(
        @Header("Authorization") auth: String?,
        @Body teamListRequest: TeamListRequest?
    ): Call<ResponseBody>

}