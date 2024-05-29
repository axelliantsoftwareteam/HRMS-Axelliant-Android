package com.axelliant.android_erp.repos

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.axelliant.android_erp.di.TestModelInjection
import com.axelliant.android_erp.model.base.BaseApiModel
import com.axelliant.android_erp.model.base.BaseModel
import com.axelliant.android_erp.model.base.Meta
import com.axelliant.android_erp.model.login.LoginRequest
import com.axelliant.android_erp.model.login.UserLoginResponse
import com.axelliant.android_erp.network.ApiInterface
import com.axelliant.android_erp.network.BaseCallBack
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Response
import java.lang.reflect.Type

class LoginRepo(private var apiInterface: ApiInterface) {
    fun userLoginApiCall(loginRequest: LoginRequest): MutableLiveData<BaseApiModel<UserLoginResponse>?> {
        val userLoginResponse = MutableLiveData<BaseApiModel<UserLoginResponse>?>()
        val call = apiInterface.userLoginCall(loginRequest)
        Log.e("HTTP Request", " " + call?.request().toString())

        call?.enqueue(object : BaseCallBack<ResponseBody>(call) {
            override fun onFinalSuccess(
                call: Call<ResponseBody>,
                response: Response<ResponseBody>
            ) {

                val type: Type = object : TypeToken<BaseApiModel<UserLoginResponse>>() {}.type
                val jsonString = response.body()?.string()
                val userModel = Gson().fromJson<BaseApiModel<UserLoginResponse>>(jsonString, type)
                userLoginResponse.value = userModel
            }


            override fun onFinalFailure(
                errorString: String?
            ) {

                Log.e("API Failure", " $errorString")

                userLoginResponse.value =
                    BaseApiModel(BaseModel(UserLoginResponse(meta = Meta(errorString.toString(), false))))

            }

        })

        return userLoginResponse
    }


    fun callSampleApi(userId: Int): MutableLiveData<TestModelInjection> {
        val screenTypeResponse = MutableLiveData<TestModelInjection>()
        val call = apiInterface.getScreen(1)

        Log.e("HTTP Request", " " + call.request().toString())

        call.enqueue(object : BaseCallBack<ResponseBody>(call) {
            override fun onFinalSuccess(
                call: Call<ResponseBody>,
                response: Response<ResponseBody>
            ) {

                Log.e("API success", " " + response.body())

                /*          val type: Type = object : TypeToken<BaseApiModel<ConversationResponse>?>() {}.type
                          val jsonString = response.body()?.string()
                          val userModel = Gson().fromJson<BaseApiModel<ConversationResponse>?>(jsonString, type)
                          screenTypeResponse.value = userModel*/
            }


            override fun onFinalFailure(
                errorString: String?
            ) {

                Log.e("API Failure", " $errorString")

                /*screenTypeResponse.value =
                    BaseApiModel(BaseModel(ConversationResponse(), false, errorString))*/
            }

        })

        return screenTypeResponse
    }

}