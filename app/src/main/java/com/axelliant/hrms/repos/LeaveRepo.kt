package com.axelliant.hrms.repos

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.axelliant.hrms.config.AppConst
import com.axelliant.hrms.model.attendance.AttRequest
import com.axelliant.hrms.model.attendance.AttendanceInput
import com.axelliant.hrms.model.leave.LeaveResponse
import com.axelliant.hrms.model.base.BaseApiModel
import com.axelliant.hrms.model.base.BaseModel
import com.axelliant.hrms.model.base.Meta
import com.axelliant.hrms.model.leave.MyLeaveDetailResponse
import com.axelliant.hrms.model.leave.TeamLeaveDetailResponse
import com.axelliant.hrms.network.ApiInterface
import com.axelliant.hrms.network.BaseCallBack
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

}