package com.axelliant.hris.repos

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.axelliant.hris.config.AppConst
import com.axelliant.hris.model.todayTeam.TodayTeamResponse
import com.axelliant.hris.model.base.BaseApiModel
import com.axelliant.hris.model.base.BaseModel
import com.axelliant.hris.model.base.Meta
import com.axelliant.hris.model.dashboard.DashboardResponse
import com.axelliant.hris.model.login.CheckInRequest
import com.axelliant.hris.model.login.CheckInResponse
import com.axelliant.hris.model.post.TeamListRequest
import com.axelliant.hris.model.todayTeam.EmployProfileListModel
import com.axelliant.hris.network.ApiInterface
import com.axelliant.hris.network.BaseCallBack
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Response
import java.lang.reflect.Type

class HomeRepo(private var apiInterface: ApiInterface) {

    fun checkInAttendance(checkInRequest: CheckInRequest): MutableLiveData<BaseApiModel<CheckInResponse>> {
        val serverResponse = MutableLiveData<BaseApiModel<CheckInResponse>>()

        val call = apiInterface.callCheckIn("token ${AppConst.TOKEN}", checkInRequest)
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
        val call = apiInterface.callDashBoard("token ${AppConst.TOKEN}")

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
                    BaseApiModel(
                        BaseModel(
                            DashboardResponse(
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

    fun getTeamAttendData(): MutableLiveData<BaseApiModel<TodayTeamResponse>> {
        val serverResponse = MutableLiveData<BaseApiModel<TodayTeamResponse>>()
        val call = apiInterface.callTeamInfo("token ${AppConst.TOKEN}")

        Log.e("HTTP Request", " " + call.request().toString())

        call.enqueue(object : BaseCallBack<ResponseBody>(call) {
            override fun onFinalSuccess(
                call: Call<ResponseBody>,
                response: Response<ResponseBody>
            ) {

                Log.e("API success", " " + response.body())

                val type: Type = object : TypeToken<BaseApiModel<TodayTeamResponse>>() {}.type
                val jsonString = response.body()?.string()
                val userModel = Gson().fromJson<BaseApiModel<TodayTeamResponse>>(jsonString, type)
                serverResponse.value = userModel
            }


            override fun onFinalFailure(
                errorString: String?
            ) {

                Log.e("API Failure", " $errorString")

                serverResponse.value =
                    BaseApiModel(
                        BaseModel(
                            TodayTeamResponse(
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

    fun getTodayEmployListData(filter: String?): MutableLiveData<BaseApiModel<EmployProfileListModel>> {
        val serverResponse = MutableLiveData<BaseApiModel<EmployProfileListModel>>()
        val call = apiInterface.callTodayTeamList(
            "token ${AppConst.TOKEN}",
            TeamListRequest().apply {
                this.filters = filter
            }
        )

        Log.e("HTTP Request", " " + call.request().toString())

        call.enqueue(object : BaseCallBack<ResponseBody>(call) {
            override fun onFinalSuccess(
                call: Call<ResponseBody>,
                response: Response<ResponseBody>
            ) {

                Log.e("API success", " " + response.body())

                val type: Type = object : TypeToken<BaseApiModel<EmployProfileListModel>>() {}.type
                val jsonString = response.body()?.string()
                val userModel =
                    Gson().fromJson<BaseApiModel<EmployProfileListModel>>(jsonString, type)
                serverResponse.value = userModel
            }


            override fun onFinalFailure(
                errorString: String?
            ) {

                Log.e("API Failure", " $errorString")

                serverResponse.value =
                    BaseApiModel(
                        BaseModel(
                            EmployProfileListModel(
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