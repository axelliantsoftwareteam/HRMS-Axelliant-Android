package com.axelliant.hrms.repos

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.axelliant.hrms.enums.AttendanceFilter
import com.axelliant.hrms.model.EmptyModel
import com.axelliant.hrms.model.attendance.AttRequest
import com.axelliant.hrms.model.attendance.AttendanceInput
import com.axelliant.hrms.model.attendance.AttendanceStatsResponse
import com.axelliant.hrms.model.base.BaseApiModel
import com.axelliant.hrms.model.base.BaseModel
import com.axelliant.hrms.model.base.Meta
import com.axelliant.hrms.model.dashboard.DashboardResponse
import com.axelliant.hrms.model.login.CheckInRequest
import com.axelliant.hrms.model.login.CheckInResponse
import com.axelliant.hrms.network.ApiInterface
import com.axelliant.hrms.network.BaseCallBack
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Response
import java.lang.reflect.Type

class HomeRepo(private var apiInterface: ApiInterface) {

    fun checkInAttendance(checkInRequest: CheckInRequest): MutableLiveData<BaseApiModel<CheckInResponse>> {
        val serverResponse = MutableLiveData<BaseApiModel<CheckInResponse>>()

        val call = apiInterface.callCheckIn(CheckInRequest())
        Log.e("HTTP Request", " ${call.request().toString()}")
        call.enqueue(object : BaseCallBack<ResponseBody>(call) {
            override fun onFinalSuccess(
                call: Call<ResponseBody>,
                response: Response<ResponseBody>
            ) {

                Log.e("API success", " " + response.body())

                val type: Type = object : TypeToken<BaseApiModel<CheckInResponse>>() {}.type
                val jsonString = response.body()?.string()
                val userModel =
                    Gson().fromJson<BaseApiModel<CheckInResponse>>(jsonString, type)
                serverResponse.value = userModel
            }


            override fun onFinalFailure(
                errorString: String?
            ) {

                Log.e("API Failure", " $errorString")

                serverResponse.value =
                    BaseApiModel(
                        BaseModel(
                            CheckInResponse(
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

    fun getDashboardData(): MutableLiveData<BaseApiModel<DashboardResponse>> {
        val serverResponse = MutableLiveData<BaseApiModel<DashboardResponse>>()
        val call = apiInterface.callDashBoard()

        Log.e("HTTP Request", " " + call.request().toString())

        call.enqueue(object : BaseCallBack<ResponseBody>(call) {
            override fun onFinalSuccess(
                call: Call<ResponseBody>,
                response: Response<ResponseBody>
            ) {

                Log.e("API success", " " + response.body())

                val type: Type = object : TypeToken<BaseApiModel<DashboardResponse>>() {}.type
                val jsonString = response.body()?.string()
                val userModel = Gson().fromJson<BaseApiModel<DashboardResponse>>(jsonString, type)
                serverResponse.value = userModel
            }


            override fun onFinalFailure(
                errorString: String?
            ) {

                Log.e("API Failure", " $errorString")

                serverResponse.value =
                    BaseApiModel(BaseModel(DashboardResponse(meta = Meta(errorString.toString(), false))))

            }

        })

        return serverResponse
    }

}