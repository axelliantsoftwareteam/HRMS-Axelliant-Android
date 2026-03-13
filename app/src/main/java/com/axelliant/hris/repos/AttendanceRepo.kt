package com.axelliant.hris.repos

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.axelliant.hris.config.AppConst
import com.axelliant.hris.enums.AttendanceFilter.*
import com.axelliant.hris.model.approval.ApprovalActionRequest
import com.axelliant.hris.model.approval.ApprovalActionItem
import com.axelliant.hris.model.approval.BulkApprovalActionRequest
import com.axelliant.hris.model.attendance.AttRequest
import com.axelliant.hris.model.attendance.AttendanceApproval
import com.axelliant.hris.model.attendance.AttendanceInput
import com.axelliant.hris.model.attendance.AttendanceResponse
import com.axelliant.hris.model.attendance.AttendanceStatsResponse
import com.axelliant.hris.model.attendance.LeaveCountInput
import com.axelliant.hris.model.attendance.LeaveCountRequest
import com.axelliant.hris.model.attendance.TeamAttendanceResponse
import com.axelliant.hris.model.base.BaseApiModel
import com.axelliant.hris.model.base.BaseModel
import com.axelliant.hris.model.base.Meta
import com.axelliant.hris.model.checkin.CheckInListResponse
import com.axelliant.hris.model.leave.LeaveApproval
import com.axelliant.hris.model.leave.PostResponse
import com.axelliant.hris.network.ApiInterface
import com.axelliant.hris.network.BaseCallBack
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Response
import java.lang.reflect.Type

class AttendanceRepo(private var apiInterface: ApiInterface) {

    fun getAttendanceStats(attendanceInput: AttendanceInput): MutableLiveData<BaseApiModel<AttendanceStatsResponse>> {
        val serverResponse = MutableLiveData<BaseApiModel<AttendanceStatsResponse>>()

        var call: Call<ResponseBody>? = null
        when (attendanceInput.filter) {
            WEEK -> call = apiInterface.callAttendanceWeekStats("token ${AppConst.TOKEN}")
            MONTH -> call = apiInterface.callAttendanceMonthStats(
                "token ${AppConst.TOKEN}",
                AttRequest().apply {
                    this.start_date = attendanceInput.startDate
                    this.end_date = attendanceInput.endDate
                }

            )

            Custom -> null
        }


        Log.e("HTTP Request", " " + call?.request().toString())

        call?.enqueue(object : BaseCallBack<ResponseBody>(call) {
            override fun onFinalSuccess(
                call: Call<ResponseBody>,
                response: Response<ResponseBody>
            ) {

                Log.e("API success", " " + response.body())

                val type: Type = object : TypeToken<BaseApiModel<AttendanceStatsResponse>>() {}.type
                val jsonString = response.body()?.string()
                val userModel =
                    Gson().fromJson<BaseApiModel<AttendanceStatsResponse>>(jsonString, type)
                serverResponse.value = userModel
            }


            override fun onFinalFailure(
                errorString: String?
            ) {

                Log.e("API Failure", " $errorString")

                serverResponse.value =
                    BaseApiModel(
                        BaseModel(
                            AttendanceStatsResponse(
                                meta = Meta(
                                    errorString.toString(),
                                    false
                                )
                            )
                        )
                    )

            }

        })

        return serverResponse
    }


    fun getAttendanceDetail(
        inputObject: AttendanceInput
    ): MutableLiveData<BaseApiModel<AttendanceResponse>> {
        val serverResponse = MutableLiveData<BaseApiModel<AttendanceResponse>>()

        val call = apiInterface.callAttendanceDetail("token ${AppConst.TOKEN}",
            AttRequest().apply {
                this.start_date = inputObject.startDate
                this.end_date = inputObject.endDate
                this.employee_list = inputObject.employeeId
                this.filters = inputObject.filters
            }
        )


        Log.e("HTTP Request", " " + call.request().toString())

        call.enqueue(object : BaseCallBack<ResponseBody>(call) {
            override fun onFinalSuccess(
                call: Call<ResponseBody>,
                response: Response<ResponseBody>
            ) {

                Log.e("API success", " " + response.body())

                val type: Type = object : TypeToken<BaseApiModel<AttendanceResponse>>() {}.type
                val jsonString = response.body()?.string()
                val userModel = Gson().fromJson<BaseApiModel<AttendanceResponse>>(jsonString, type)
                serverResponse.value = userModel
            }


            override fun onFinalFailure(
                errorString: String?
            ) {

                Log.e("API Failure", " $errorString")

                serverResponse.value =
                    BaseApiModel(
                        BaseModel(
                            AttendanceResponse(
                                meta = Meta(
                                    errorString.toString(),
                                    false
                                )
                            )
                        )
                    )

            }

        })

        return serverResponse
    }


    fun getTeamAttendanceDetail(
        inputObject: AttendanceInput
    ): MutableLiveData<BaseApiModel<TeamAttendanceResponse>> {
        val serverResponse = MutableLiveData<BaseApiModel<TeamAttendanceResponse>>()


        /*   val call = apiInterface.callTeamAttendanceDetail(

               start_date = inputObject.startDate,
               end_date = inputObject.endDate,
               employee_list = inputObject.employeeId
           )*/

        val call = apiInterface.callTeamAttendanceDetail("token ${AppConst.TOKEN}",
            AttRequest().apply {
                this.start_date = inputObject.startDate
                this.end_date = inputObject.endDate
                this.employee_list = inputObject.employeeId
            }
        )



        Log.e("HTTP Request", " " + call.request().toString())

        call.enqueue(object : BaseCallBack<ResponseBody>(call) {
            override fun onFinalSuccess(
                call: Call<ResponseBody>,
                response: Response<ResponseBody>
            ) {

                Log.e("API success", " " + response.body())

                val type: Type = object : TypeToken<BaseApiModel<TeamAttendanceResponse>>() {}.type
                val jsonString = response.body()?.string()
                val userModel =
                    Gson().fromJson<BaseApiModel<TeamAttendanceResponse>>(jsonString, type)
                serverResponse.value = userModel
            }


            override fun onFinalFailure(
                errorString: String?
            ) {

                Log.e("API Failure", " $errorString")

                serverResponse.value =
                    BaseApiModel(
                        BaseModel(
                            TeamAttendanceResponse(
                                meta = Meta(
                                    errorString.toString(),
                                    false
                                )
                            )
                        )
                    )

            }

        })

        return serverResponse
    }

    fun attendanceApproval(
        inputObject: AttendanceInput
    ): MutableLiveData<BaseApiModel<AttendanceApproval>> {
        val serverResponse = MutableLiveData<BaseApiModel<AttendanceApproval>>()

        val call = apiInterface.callAttendanceApproval(
            "token ${AppConst.TOKEN}",AttRequest().apply {
                this.start_date = inputObject.startDate
                this.end_date = inputObject.endDate
                this.employee_list = inputObject.employeeId
            }
        )



        Log.e("HTTP Request", " " + call.request().toString())

        call.enqueue(object : BaseCallBack<ResponseBody>(call) {
            override fun onFinalSuccess(
                call: Call<ResponseBody>,
                response: Response<ResponseBody>
            ) {

                Log.e("API success", " " + response.body())

                val type: Type = object : TypeToken<BaseApiModel<AttendanceApproval>>() {}.type
                val jsonString = response.body()?.string()
                val userModel =
                    Gson().fromJson<BaseApiModel<AttendanceApproval>>(jsonString, type)
                serverResponse.value = userModel
            }


            override fun onFinalFailure(
                errorString: String?
            ) {

                Log.e("API Failure", " $errorString")

                serverResponse.value =
                    BaseApiModel(
                        BaseModel(
                            AttendanceApproval(
                                meta = Meta(
                                    errorString.toString(),
                                    false
                                )
                            )
                        )
                    )

            }

        })

        return serverResponse
    }


    fun attendanceApprovalStatus(
        inputObject: LeaveApproval
    ): MutableLiveData<BaseApiModel<PostResponse>> {
        val serverResponse = MutableLiveData<BaseApiModel<PostResponse>>()

        val call = apiInterface.takeApprovalAction(
            "token ${AppConst.TOKEN}",
            ApprovalActionRequest(
                approval_type = "checkin",
                reference_name = inputObject.checkin_id,
                status = inputObject.status
            )
        )

        Log.e("HTTP Request", " " + call.request().toString())

        call.enqueue(object : BaseCallBack<ResponseBody>(call) {
            override fun onFinalSuccess(
                call: Call<ResponseBody>,
                response: Response<ResponseBody>
            ) {

                Log.e("API success", " " + response.body())

                val type: Type = object : TypeToken<BaseApiModel<PostResponse>>() {}.type
                val jsonString = response.body()?.string()
                val userModel =
                    Gson().fromJson<BaseApiModel<PostResponse>>(jsonString, type)
                serverResponse.value = userModel
            }


            override fun onFinalFailure(
                errorString: String?
            ) {

                Log.e("API Failure", " $errorString")

                serverResponse.value =
                    BaseApiModel(
                        BaseModel(
                            PostResponse(
                                meta = Meta(
                                    errorString.toString(),
                                    false
                                )
                            )
                        )
                    )

            }

        })

        return serverResponse
    }

    fun bulkAttendanceApprovalStatus(
        checkinIds: List<String>,
        status: String
    ): MutableLiveData<BaseApiModel<PostResponse>> {
        val serverResponse = MutableLiveData<BaseApiModel<PostResponse>>()

        val call = apiInterface.bulkTakeApprovalAction(
            "token ${AppConst.TOKEN}",
            BulkApprovalActionRequest(
                actions = checkinIds.filter { it.isNotBlank() }.map {
                    ApprovalActionItem(
                        approval_type = "checkin",
                        reference_name = it
                    )
                },
                status = status
            )
        )

        Log.e("HTTP Request", " " + call.request().toString())

        call.enqueue(object : BaseCallBack<ResponseBody>(call) {
            override fun onFinalSuccess(
                call: Call<ResponseBody>,
                response: Response<ResponseBody>
            ) {
                val type: Type = object : TypeToken<BaseApiModel<PostResponse>>() {}.type
                val jsonString = response.body()?.string()
                val userModel = Gson().fromJson<BaseApiModel<PostResponse>>(jsonString, type)
                serverResponse.value = userModel
            }

            override fun onFinalFailure(errorString: String?) {
                serverResponse.value =
                    BaseApiModel(
                        BaseModel(
                            PostResponse(
                                meta = Meta(
                                    errorString.toString(),
                                    false
                                )
                            )
                        )
                    )
            }
        })

        return serverResponse
    }


    fun checkInList(
        inputObject: LeaveCountInput
    ): MutableLiveData<BaseApiModel<CheckInListResponse>> {
        val serverResponse = MutableLiveData<BaseApiModel<CheckInListResponse>>()

        val call = apiInterface.callCheckInList(
            "token ${AppConst.TOKEN}",LeaveCountRequest().apply {
                this.start_date = inputObject.startDate
                this.end_date = inputObject.endDate
                this.employee_list = inputObject.employeeId
                this.filters = inputObject.filters
            }
        )


        Log.e("HTTP Request", " " + call.request().toString())

        call.enqueue(object : BaseCallBack<ResponseBody>(call) {
            override fun onFinalSuccess(
                call: Call<ResponseBody>,
                response: Response<ResponseBody>
            ) {

                Log.e("API success", " " + response.body())

                val type: Type = object : TypeToken<BaseApiModel<CheckInListResponse>>() {}.type
                val jsonString = response.body()?.string()
                val userModel = Gson().fromJson<BaseApiModel<CheckInListResponse>>(jsonString, type)
                serverResponse.value = userModel
            }


            override fun onFinalFailure(
                errorString: String?
            ) {

                Log.e("API Failure", " $errorString")

                serverResponse.value =
                    BaseApiModel(
                        BaseModel(
                            CheckInListResponse(
                                meta = Meta(
                                    errorString.toString(),
                                    false
                                )
                            )
                        )
                    )

            }

        })

        return serverResponse
    }



}
