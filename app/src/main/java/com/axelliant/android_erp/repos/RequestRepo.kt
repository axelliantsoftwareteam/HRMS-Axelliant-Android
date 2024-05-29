package com.axelliant.android_erp.repos

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.axelliant.android_erp.enums.AttendanceFilter
import com.axelliant.android_erp.enums.AttendanceFilter.*
import com.axelliant.android_erp.model.attendance.AttendanceInput
import com.axelliant.android_erp.model.leave.LeaveResponse
import com.axelliant.android_erp.model.base.BaseApiModel
import com.axelliant.android_erp.model.base.BaseModel
import com.axelliant.android_erp.model.base.Meta
import com.axelliant.android_erp.model.leave.MyLeaveDetailResponse
import com.axelliant.android_erp.model.post.AttendanceRequest
import com.axelliant.android_erp.model.post.LeaveRequest
import com.axelliant.android_erp.network.ApiInterface
import com.axelliant.android_erp.network.BaseCallBack
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Response
import java.lang.reflect.Type

class RequestRepo(private var apiInterface: ApiInterface) {

    fun postLeaveRequest(leaveRequest: LeaveRequest): MutableLiveData<BaseApiModel<LeaveResponse>> {
        val serverResponse = MutableLiveData<BaseApiModel<LeaveResponse>>()

        val call: Call<ResponseBody>  = apiInterface.postLeaveRequest(leaveRequest)

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
                    BaseApiModel(BaseModel(LeaveResponse(meta = Meta("", false))))

            }

        })

        return serverResponse
    }
    fun postAttendanceRequest(attendanceRequest: AttendanceRequest): MutableLiveData<BaseApiModel<LeaveResponse>> {
        val serverResponse = MutableLiveData<BaseApiModel<LeaveResponse>>()

        val call: Call<ResponseBody>  = apiInterface.postAttendanceRequest(attendanceRequest)

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
                    BaseApiModel(BaseModel(LeaveResponse(meta = Meta("", false))))

            }

        })

        return serverResponse
    }


}