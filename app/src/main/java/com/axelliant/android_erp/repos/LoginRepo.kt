package com.axelliant.android_erp.repos

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.axelliant.android_erp.di.TestModelInjection
import com.axelliant.android_erp.network.ApiInterface
import com.axelliant.android_erp.network.BaseCallBack
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Response

class LoginRepo(private var apiInterface: ApiInterface) {

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