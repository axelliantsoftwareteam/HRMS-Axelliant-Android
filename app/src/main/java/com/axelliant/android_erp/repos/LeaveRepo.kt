package com.axelliant.android_erp.repos

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.axelliant.android_erp.enums.AttendanceFilter
import com.axelliant.android_erp.enums.AttendanceFilter.*
import com.axelliant.android_erp.model.leave.LeaveResponse
import com.axelliant.android_erp.model.base.BaseApiModel
import com.axelliant.android_erp.model.base.BaseModel
import com.axelliant.android_erp.model.base.Meta
import com.axelliant.android_erp.network.ApiInterface
import com.axelliant.android_erp.network.BaseCallBack
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Response
import java.lang.reflect.Type

class LeaveRepo(private var apiInterface: ApiInterface) {

    fun getLeaveStats(currentFilter: AttendanceFilter): MutableLiveData<BaseApiModel<LeaveResponse>> {
        val serverResponse = MutableLiveData<BaseApiModel<LeaveResponse>>()

        var call: Call<ResponseBody>? = null
        when (currentFilter) {
            WEEK -> call = apiInterface.callLeaveWeekStats()
            MONTH -> call = apiInterface.callLeaveMonthStats()
            Custom -> null
        }


        Log.e("HTTP Request", " " + call?.request().toString())

        call?.enqueue(object : BaseCallBack<ResponseBody>(call) {
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