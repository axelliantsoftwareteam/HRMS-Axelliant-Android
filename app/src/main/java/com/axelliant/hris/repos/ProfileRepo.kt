package com.axelliant.hris.repos

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.axelliant.hris.config.AppConst
import com.axelliant.hris.model.base.BaseApiModel
import com.axelliant.hris.model.base.BaseModel
import com.axelliant.hris.model.base.Meta
import com.axelliant.hris.model.profile.CertificationCreateRequest
import com.axelliant.hris.model.profile.CertificationCreateResponse
import com.axelliant.hris.model.profile.CertificationListResponse
import com.axelliant.hris.model.profile.ProfileResponse
import com.axelliant.hris.network.ApiInterface
import com.axelliant.hris.network.BaseCallBack
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Response
import java.lang.reflect.Type

class ProfileRepo(private val apiInterface: ApiInterface) {

    fun getProfile(): MutableLiveData<BaseApiModel<ProfileResponse>> {
        val responseLiveData = MutableLiveData<BaseApiModel<ProfileResponse>>()
        val call = apiInterface.getProfileOfEmployee("token ${AppConst.TOKEN}")
        call.enqueue(object : BaseCallBack<ResponseBody>(call) {
            override fun onFinalSuccess(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                val type: Type = object : TypeToken<BaseApiModel<ProfileResponse>>() {}.type
                val jsonString = response.body()?.string()
                responseLiveData.value = Gson().fromJson(jsonString, type)
            }

            override fun onFinalFailure(errorString: String?) {
                Log.e("ProfileRepo", "getProfile failure $errorString")
                responseLiveData.value = BaseApiModel(
                    BaseModel(
                        ProfileResponse(
                            meta = Meta(errorString.toString(), false)
                        )
                    )
                )
            }
        })
        return responseLiveData
    }

    fun getCertifications(): MutableLiveData<BaseApiModel<CertificationListResponse>> {
        val responseLiveData = MutableLiveData<BaseApiModel<CertificationListResponse>>()
        val call = apiInterface.getEmployeeCertifications("token ${AppConst.TOKEN}")
        call.enqueue(object : BaseCallBack<ResponseBody>(call) {
            override fun onFinalSuccess(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                val type: Type = object : TypeToken<BaseApiModel<CertificationListResponse>>() {}.type
                val jsonString = response.body()?.string()
                responseLiveData.value = Gson().fromJson(jsonString, type)
            }

            override fun onFinalFailure(errorString: String?) {
                Log.e("ProfileRepo", "getCertifications failure $errorString")
                responseLiveData.value = BaseApiModel(
                    BaseModel(
                        CertificationListResponse(
                            meta = Meta(errorString.toString(), false)
                        )
                    )
                )
            }
        })
        return responseLiveData
    }

    fun createCertification(request: CertificationCreateRequest): MutableLiveData<BaseApiModel<CertificationCreateResponse>> {
        val responseLiveData = MutableLiveData<BaseApiModel<CertificationCreateResponse>>()
        val call = apiInterface.createEmployeeCertification("token ${AppConst.TOKEN}", request)
        call.enqueue(object : BaseCallBack<ResponseBody>(call) {
            override fun onFinalSuccess(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                val type: Type = object : TypeToken<BaseApiModel<CertificationCreateResponse>>() {}.type
                val jsonString = response.body()?.string()
                responseLiveData.value = Gson().fromJson(jsonString, type)
            }

            override fun onFinalFailure(errorString: String?) {
                Log.e("ProfileRepo", "createCertification failure $errorString")
                responseLiveData.value = BaseApiModel(
                    BaseModel(
                        CertificationCreateResponse(
                            status_message = errorString,
                            meta = Meta(errorString.toString(), false)
                        )
                    )
                )
            }
        })
        return responseLiveData
    }
}
