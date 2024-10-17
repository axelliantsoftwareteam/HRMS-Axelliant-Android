package com.axelliant.hris.repos

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.axelliant.hris.config.AppConst
import com.axelliant.hris.model.base.BaseApiModel
import com.axelliant.hris.model.base.BaseModel
import com.axelliant.hris.model.base.Meta
import com.axelliant.hris.model.leave.GetAttendanceResponse
import com.axelliant.hris.model.leave.GetLeavesResponse
import com.axelliant.hris.model.leave.PostResponse
import com.axelliant.hris.model.leave.leaveCount.GetLeaveCount
import com.axelliant.hris.model.leave.leaveCount.LeaveCountByDate
import com.axelliant.hris.model.leave.leaveCount.LeaveCountByDaysRequest
import com.axelliant.hris.model.post.AttendanceRequest
import com.axelliant.hris.model.post.LeaveRequest
import com.axelliant.hris.network.ApiInterface
import com.axelliant.hris.network.BaseCallBack
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Response
import java.lang.reflect.Type

class RequestRepo(private var apiInterface: ApiInterface) {


    fun getLeaves(): MutableLiveData<BaseApiModel<GetLeavesResponse>> {
        val serverResponse = MutableLiveData<BaseApiModel<GetLeavesResponse>>()

        val call: Call<ResponseBody>  = apiInterface.getLeaveTypes("token ${AppConst.TOKEN}")

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

    fun getLeavesTypeWithCount(): MutableLiveData<BaseApiModel<GetLeaveCount>> {
        val serverResponse = MutableLiveData<BaseApiModel<GetLeaveCount>>()

        val call: Call<ResponseBody>  = apiInterface.getLeaveTypesWithCount("token ${AppConst.TOKEN}")

        Log.e("HTTP Request", " " + call?.request().toString())

        call.enqueue(object : BaseCallBack<ResponseBody>(call) {
            override fun onFinalSuccess(
                call: Call<ResponseBody>,
                response: Response<ResponseBody>
            ) {

                Log.e("API success", " " + response.body())

                val type: Type = object : TypeToken<BaseApiModel<GetLeaveCount>>() {}.type
                val jsonString = response.body()?.string()
                val userModel = Gson().fromJson<BaseApiModel<GetLeaveCount>>(jsonString, type)
                serverResponse.value = userModel
            }


            override fun onFinalFailure(
                errorString: String?
            ) {

                Log.e("API Failure", " $errorString")

                serverResponse.value =
                    BaseApiModel(BaseModel(GetLeaveCount(null, meta = Meta(errorString.toString(), false) )))
            }

        })

        return serverResponse
    }

    fun getAttendanceInfo(): MutableLiveData<BaseApiModel<GetAttendanceResponse>> {
        val serverResponse = MutableLiveData<BaseApiModel<GetAttendanceResponse>>()

        val call: Call<ResponseBody>  = apiInterface.getAttendanceRequestInformation("token ${AppConst.TOKEN}")

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

        val call: Call<ResponseBody>  = apiInterface.updateLeaveRequest("token ${AppConst.TOKEN}",leaveRequest)

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

        val call: Call<ResponseBody>  = apiInterface.deleteLeaveRequest("token ${AppConst.TOKEN}",leaveRequest)

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

    fun getLeaveCountRequest(leaveCountByDaysRequest: LeaveCountByDaysRequest): MutableLiveData<BaseApiModel<LeaveCountByDate>> {
        val serverResponse = MutableLiveData<BaseApiModel<LeaveCountByDate>>()

        val call: Call<ResponseBody>  = apiInterface.leaveCountRequest("token ${AppConst.TOKEN}",leaveCountByDaysRequest)

        Log.e("HTTP Request", " " + call?.request().toString())

        call.enqueue(object : BaseCallBack<ResponseBody>(call) {
            override fun onFinalSuccess(
                call: Call<ResponseBody>,
                response: Response<ResponseBody>
            ) {

                Log.e("API success", " " + response.body())

                val type: Type = object : TypeToken<BaseApiModel<LeaveCountByDate>>() {}.type
                val jsonString = response.body()?.string()
                val userModel = Gson().fromJson<BaseApiModel<LeaveCountByDate>>(jsonString, type)
                serverResponse.value = userModel
            }


            override fun onFinalFailure(
                errorString: String?
            ) {

                Log.e("API Failure", " $errorString")

                serverResponse.value =
                    BaseApiModel(BaseModel(LeaveCountByDate(null,meta = Meta(errorString.toString(), false))))

            }

        })

        return serverResponse
    }


    fun postLeaveRequest(leaveRequest: LeaveRequest): MutableLiveData<BaseApiModel<PostResponse>> {
        val serverResponse = MutableLiveData<BaseApiModel<PostResponse>>()

        val call: Call<ResponseBody>  = apiInterface.postLeaveRequest("token ${AppConst.TOKEN}",leaveRequest)

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

        val call: Call<ResponseBody>  = apiInterface.postAttendanceRequest("token ${AppConst.TOKEN}",attendanceRequest)

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



    fun updateAttendanceRequest(leaveRequest: AttendanceRequest): MutableLiveData<BaseApiModel<PostResponse>> {
        val serverResponse = MutableLiveData<BaseApiModel<PostResponse>>()

        val call: Call<ResponseBody>  = apiInterface.updateAttendanceRequest("token ${AppConst.TOKEN}",leaveRequest)

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

    fun deleteAttendanceRequest(leaveRequest: LeaveRequest): MutableLiveData<BaseApiModel<PostResponse>> {
        val serverResponse = MutableLiveData<BaseApiModel<PostResponse>>()

        val call: Call<ResponseBody>  = apiInterface.deleteAttendanceRequest("token ${AppConst.TOKEN}",leaveRequest)

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