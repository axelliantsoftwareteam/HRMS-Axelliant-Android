package com.axelliant.hris.repos

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.axelliant.hris.config.AppConst
import com.axelliant.hris.model.approval.ApprovalActionRequest
import com.axelliant.hris.model.approval.ApprovalActionItem
import com.axelliant.hris.model.approval.BulkApprovalActionRequest
import com.axelliant.hris.model.attendance.AttRequest
import com.axelliant.hris.model.attendance.AttendanceInput
import com.axelliant.hris.model.leave.LeaveResponse
import com.axelliant.hris.model.base.BaseApiModel
import com.axelliant.hris.model.base.BaseModel
import com.axelliant.hris.model.base.Meta
import com.axelliant.hris.model.leave.LeaveApproval
import com.axelliant.hris.model.leave.MyLeaveDetailResponse
import com.axelliant.hris.model.leave.MyUpcomingLeaveDetailResponse
import com.axelliant.hris.model.leave.PostResponse
import com.axelliant.hris.model.leave.TeamLeaveDetailResponse
import com.axelliant.hris.model.leave.UpcomingLeaveInput
import com.axelliant.hris.network.ApiInterface
import com.axelliant.hris.network.BaseCallBack
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Response
import java.lang.reflect.Type

class LeaveRepo(private var apiInterface: ApiInterface) {

    fun getLeaveStats(attendanceInput: AttendanceInput): MutableLiveData<BaseApiModel<LeaveResponse>> {
        val serverResponse = MutableLiveData<BaseApiModel<LeaveResponse>>()

        val call: Call<ResponseBody> = apiInterface.callLeaveStats("token ${AppConst.TOKEN}",AttRequest().apply {
            this.start_date = attendanceInput.startDate
            this.end_date = attendanceInput.endDate
        })

        Log.e("HTTP Request", " " + call?.request().toString())

        call.enqueue(object : BaseCallBack<ResponseBody>(call) {
            override fun onFinalSuccess(
                call: Call<ResponseBody>,
                response: Response<ResponseBody>
            ) {

                Log.e("API success", " " + response.body())

                val type: Type = object : TypeToken<BaseApiModel<LeaveResponse>>() {}.type
                val jsonString = response.body()?.string()
                val userModel = Gson().fromJson<BaseApiModel<LeaveResponse>>(jsonString, type)
                serverResponse.value = userModel
            }


            override fun onFinalFailure(
                errorString: String?
            ) {

                Log.e("API Failure", " $errorString")

                serverResponse.value =
                    BaseApiModel(
                        BaseModel(
                            LeaveResponse(
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


    fun getMyLeaveDetail(attendanceInput: AttendanceInput): MutableLiveData<BaseApiModel<MyLeaveDetailResponse>> {
        val serverResponse = MutableLiveData<BaseApiModel<MyLeaveDetailResponse>>()

        val call: Call<ResponseBody> = apiInterface.callMyLeaveDetail("token ${AppConst.TOKEN}",
            AttRequest().apply {
                this.start_date = attendanceInput.startDate
                this.end_date = attendanceInput.endDate
                this.filters = attendanceInput.filters
            }

        )

        Log.e("HTTP Request", " " + call?.request().toString())

        call.enqueue(object : BaseCallBack<ResponseBody>(call) {
            override fun onFinalSuccess(
                call: Call<ResponseBody>,
                response: Response<ResponseBody>
            ) {

                Log.e("API success", " " + response.body())

                val type: Type = object : TypeToken<BaseApiModel<MyLeaveDetailResponse>>() {}.type
                val jsonString = response.body()?.string()
                val userModel =
                    Gson().fromJson<BaseApiModel<MyLeaveDetailResponse>>(jsonString, type)
                serverResponse.value = userModel
            }


            override fun onFinalFailure(
                errorString: String?
            ) {

                Log.e("API Failure", " $errorString")

                serverResponse.value =
                    BaseApiModel(
                        BaseModel(
                            MyLeaveDetailResponse(
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


    fun getTeamLeaveDetail(attendanceInput: AttendanceInput): MutableLiveData<BaseApiModel<TeamLeaveDetailResponse>> {
        val serverResponse = MutableLiveData<BaseApiModel<TeamLeaveDetailResponse>>()

        val call: Call<ResponseBody> = apiInterface.callTeamLeaveDetail("token ${AppConst.TOKEN}",
            AttRequest().apply {
                this.start_date = attendanceInput.startDate
                this.end_date = attendanceInput.endDate
                this.filters = attendanceInput.filters
                this.employee_list = attendanceInput.employeeId
                this.for_approvals = attendanceInput.for_approvals
            }

        )

        Log.e("HTTP Request", " " + call?.request().toString())

        call.enqueue(object : BaseCallBack<ResponseBody>(call) {
            override fun onFinalSuccess(
                call: Call<ResponseBody>,
                response: Response<ResponseBody>
            ) {

                Log.e("API success", " " + response.body())

                val type: Type = object : TypeToken<BaseApiModel<TeamLeaveDetailResponse>>() {}.type
                val jsonString = response.body()?.string()
                val userModel =
                    Gson().fromJson<BaseApiModel<TeamLeaveDetailResponse>>(jsonString, type)
                serverResponse.value = userModel
            }


            override fun onFinalFailure(
                errorString: String?
            ) {

                Log.e("API Failure", " $errorString")

                serverResponse.value =
                    BaseApiModel(
                        BaseModel(
                            TeamLeaveDetailResponse(
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
    fun getUpcomingLeaveDetail(upcomingLeaveInput: UpcomingLeaveInput): MutableLiveData<BaseApiModel<MyUpcomingLeaveDetailResponse>> {
        val serverResponse = MutableLiveData<BaseApiModel<MyUpcomingLeaveDetailResponse>>()

        val call: Call<ResponseBody> = apiInterface.callUpcomingLeaveDetail("token ${AppConst.TOKEN}",
            upcomingLeaveInput
        )

        Log.e("HTTP Request", " " + call?.request().toString())

        call.enqueue(object : BaseCallBack<ResponseBody>(call) {
            override fun onFinalSuccess(
                call: Call<ResponseBody>,
                response: Response<ResponseBody>
            ) {

                Log.e("API success", " " + response.body())

                val type: Type = object : TypeToken<BaseApiModel<MyUpcomingLeaveDetailResponse>>() {}.type
                val jsonString = response.body()?.string()
                val userModel =
                    Gson().fromJson<BaseApiModel<MyUpcomingLeaveDetailResponse>>(jsonString, type)
                serverResponse.value = userModel
            }


            override fun onFinalFailure(
                errorString: String?
            ) {

                Log.e("API Failure", " $errorString")

                serverResponse.value =
                    BaseApiModel(
                        BaseModel(
                            MyUpcomingLeaveDetailResponse(
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


    fun leaveApproval(attendanceInput: LeaveApproval): MutableLiveData<BaseApiModel<PostResponse>> {
        val serverResponse = MutableLiveData<BaseApiModel<PostResponse>>()

        val call: Call<ResponseBody> = apiInterface.takeApprovalAction(
            "token ${AppConst.TOKEN}",
            ApprovalActionRequest(
                approval_type = "leave",
                reference_name = attendanceInput.leave_id,
                status = attendanceInput.status
            )
        )

        Log.e("HTTP Request", " " + call?.request().toString())

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

    fun bulkLeaveApproval(
        leaveIds: List<String>,
        status: String
    ): MutableLiveData<BaseApiModel<PostResponse>> {
        val serverResponse = MutableLiveData<BaseApiModel<PostResponse>>()

        val call: Call<ResponseBody> = apiInterface.bulkTakeApprovalAction(
            "token ${AppConst.TOKEN}",
            BulkApprovalActionRequest(
                actions = leaveIds.filter { it.isNotBlank() }.map {
                    ApprovalActionItem(
                        approval_type = "leave",
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



}
