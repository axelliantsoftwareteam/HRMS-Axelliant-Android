package com.axelliant.hris.repos

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.axelliant.hris.model.base.BaseApiModel
import com.axelliant.hris.model.base.BaseModel
import com.axelliant.hris.model.base.Meta
import com.axelliant.hris.model.login.LoginRequest
import com.axelliant.hris.model.login.UserLoginResponse
import com.axelliant.hris.network.ApiInterface
import com.axelliant.hris.network.BaseCallBack
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
                    BaseApiModel(BaseModel(UserLoginResponse( meta = Meta(
                        errorString.toString(),
                        false
                    ))))

            }

        })

        return userLoginResponse
    }



}