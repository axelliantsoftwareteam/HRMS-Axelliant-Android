package com.axelliant.hrms.repos

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.axelliant.hrms.enums.AttendanceFilter
import com.axelliant.hrms.enums.AttendanceFilter.*
import com.axelliant.hrms.model.attendance.AttendanceInput
import com.axelliant.hrms.model.leave.LeaveResponse
import com.axelliant.hrms.model.base.BaseApiModel
import com.axelliant.hrms.model.base.BaseModel
import com.axelliant.hrms.model.base.Meta
import com.axelliant.hrms.model.leave.GetAttendanceResponse
import com.axelliant.hrms.model.leave.GetLeavesResponse
import com.axelliant.hrms.model.leave.MyLeaveDetailResponse
import com.axelliant.hrms.model.leave.PostResponse
import com.axelliant.hrms.model.post.AttendanceRequest
import com.axelliant.hrms.model.post.LeaveRequest
import com.axelliant.hrms.network.ApiInterface
import com.axelliant.hrms.network.BaseCallBack
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Response
import java.lang.reflect.Type

class RequestRepo(private var apiInterface: ApiInterface) {


    fun getLeaves(): MutableLiveData<BaseApiModel<GetLeavesResponse>> {
        val serverResponse = MutableLiveData<BaseApiModel<GetLeavesResponse>>()

        val call: Call<ResponseBody>  = apiInterface.getLeaveTypes()

        Log.e("HTTP Request", " " + call?.request().toString())

        call.enqueue(object : BaseCallBack<ResponseBody>(call) {
            override fun onFinalSuccess(
                call: Call<ResponseBody>,
                response: Response<ResponseBody>
            ) {

                Log.e("API success", " " + response.body())

                val type: Type = object : TypeToken<BaseApiModel<GetLeavesResponse>>() {}.type
                val jsonString = response.body()?.string()
                val userModel = Gson().fromJson<BaseApiModel<GetLeavesResponse>>(jsonString, type)
                serverResponse.value = userModel
            }


            override fun onFinalFailure(
                errorString: String?
            ) {

                Log.e("API Failure", " $errorString")

                serverResponse.value =
                    BaseApiModel(BaseModel(GetLeavesResponse(meta = Meta(errorString.toString(), false))))

            }

        })

        return serverResponse
    }

    fun getAttendanceInfo(): MutableLiveData<BaseApiModel<GetAttendanceResponse>> {
        val serverResponse = MutableLiveData<BaseApiModel<GetAttendanceResponse>>()

        val call: Call<ResponseBody>  = apiInterface.getAttendanceRequestInformation()

        Log.e("HTTP Request", " " + call?.request().toString())

        call.enqueue(object : BaseCallBack<ResponseBody>(call) {
            override fun onFinalSuccess(
                call: Call<ResponseBody>,
                response: Response<ResponseBody>
            ) {

                Log.e("API success", " " + response.body())

                val type: Type = object : TypeToken<BaseApiModel<GetAttendanceResponse>>() {}.type
                val jsonString = response.body()?.string()
                val userModel = Gson().fromJson<BaseApiModel<GetAttendanceResponse>>(jsonString, type)
                serverResponse.value = userModel
            }


            override fun onFinalFailure(
                errorString: String?
            ) {

                Log.e("API Failure", " $errorString")

                serverResponse.value =
                    BaseApiModel(BaseModel(GetAttendanceResponse(meta = Meta(errorString.toString(), false))))

            }

        })

        return serverResponse
    }

    fun updateLeaveRequest(leaveRequest: LeaveRequest): MutableLiveData<BaseApiModel<PostResponse>> {
        val serverResponse = MutableLiveData<BaseApiModel<PostResponse>>()

        val call: Call<ResponseBody>  = apiInterface.updateLeaveRequest(leaveRequest)

        Log.e("HTTP Request", " " + call?.request().toString())

        call.enqueue(object : BaseCallBack<ResponseBody>(call) {
            override fun onFinalSuccess(
                call: Call<ResponseBody>,
                response: Response<ResponseBody>
            ) {

                Log.e("API success", " " + response.body())

                val type: Type = object : TypeToken<BaseApiModel<PostResponse>>() {}.type
                val jsonString = response.body()?.string()
                val userModel = Gson().fromJson<BaseApiModel<PostResponse>>(jsonString, type)
                serverResponse.value = userModel
            }


            override fun onFinalFailure(
                errorString: String?
            ) {

                Log.e("API Failure", " $errorString")

                serverResponse.value =
                    BaseApiModel(BaseModel(PostResponse(meta = Meta(errorString.toString(), false))))

            }

        })

        return serverResponse
    }

    fun deleteLeaveRequest(leaveRequest: LeaveRequest): MutableLiveData<BaseApiModel<PostResponse>> {
        val serverResponse = MutableLiveData<BaseApiModel<PostResponse>>()

        val call: Call<ResponseBody>  = apiInterface.deleteLeaveRequest(leaveRequest)

        Log.e("HTTP Request", " " + call?.request().toString())

        call.enqueue(object : BaseCallBack<ResponseBody>(call) {
            override fun onFinalSuccess(
                call: Call<ResponseBody>,
                response: Response<ResponseBody>
            ) {

                Log.e("API success", " " + response.body())

                val type: Type = object : TypeToken<BaseApiModel<PostResponse>>() {}.type
                val jsonString = response.body()?.string()
                val userModel = Gson().fromJson<BaseApiModel<PostResponse>>(jsonString, type)
                serverResponse.value = userModel
            }


            override fun onFinalFailure(
                errorString: String?
            ) {

                Log.e("API Failure", " $errorString")

                serverResponse.value =
                    BaseApiModel(BaseModel(PostResponse(meta = Meta(errorString.toString(), false))))

            }

        })

        return serverResponse
    }


    fun postLeaveRequest(leaveRequest: LeaveRequest): MutableLiveData<BaseApiModel<PostResponse>> {
        val serverResponse = MutableLiveData<BaseApiModel<PostResponse>>()

        val call: Call<ResponseBody>  = apiInterface.postLeaveRequest(leaveRequest)

        Log.e("HTTP Request", " " + call?.request().toString())

        call.enqueue(object : BaseCallBack<ResponseBody>(call) {
            override fun onFinalSuccess(
                call: Call<ResponseBody>,
                response: Response<ResponseBody>
            ) {

                Log.e("API success", " " + response.body())

                val type: Type = object : TypeToken<BaseApiModel<PostResponse>>() {}.type
                val jsonString = response.body()?.string()
                val userModel = Gson().fromJson<BaseApiModel<PostResponse>>(jsonString, type)
                serverResponse.value = userModel
            }


            override fun onFinalFailure(
                errorString: String?
            ) {

                Log.e("API Failure", " $errorString")

                serverResponse.value =
                    BaseApiModel(BaseModel(PostResponse(meta = Meta(errorString.toString(), false))))

            }

        })

        return serverResponse
    }
    fun postAttendanceRequest(attendanceRequest: AttendanceRequest): MutableLiveData<BaseApiModel<PostResponse>> {
        val serverResponse = MutableLiveData<BaseApiModel<PostResponse>>()

        val call: Call<ResponseBody>  = apiInterface.postAttendanceRequest(attendanceRequest)

        Log.e("HTTP Request", " " + call?.request().toString())

        call.enqueue(object : BaseCallBack<ResponseBody>(call) {
            override fun onFinalSuccess(
                call: Call<ResponseBody>,
                response: Response<ResponseBody>
            ) {

                Log.e("API success", " " + response.body())

                val type: Type = object : TypeToken<BaseApiModel<PostResponse>>() {}.type
                val jsonString = response.body()?.string()
                val userModel = Gson().fromJson<BaseApiModel<PostResponse>>(jsonString, type)
                serverResponse.value = userModel
            }


            override fun onFinalFailure(
                errorString: String?
            ) {

                Log.e("API Failure", " $errorString")

                serverResponse.value =
                    BaseApiModel(BaseModel(PostResponse(meta = Meta(errorString.toString(), false))))

            }

        })

        return serverResponse
    }


}