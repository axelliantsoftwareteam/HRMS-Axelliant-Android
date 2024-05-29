package com.axelliant.android_erp.viewmodel

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.axelliant.android_erp.event.Event
import com.axelliant.android_erp.model.base.BaseApiModel
import com.axelliant.android_erp.model.dashboard.DashboardResponse
import com.axelliant.android_erp.model.login.LoginRequest
import com.axelliant.android_erp.model.login.UserLoginResponse
import com.axelliant.android_erp.repos.LoginRepo

class LoginViewModel(private val loginRepo: LoginRepo) : BaseViewModel() {
    val userLoginResponse: MutableLiveData<Event<UserLoginResponse?>> by lazy { MutableLiveData<Event<UserLoginResponse?>>() }

    fun getConversationCall(userId: Int) {
        isLoading.value = Event(true)
        loginRepo.callSampleApi(userId)
            .observeForever { data ->
                // Handle the login response
                data?.let { conversation ->
                    isLoading.value = Event(false)
                    // Handle success

                    Log.d("Success VieModel->", "true")

                } ?: run {
                    isLoading.value = Event(false)
                    // Handle error
                    Log.d("Success VieModel->", "false")


                }
            }


    }

    fun postMicToken(tokenString: String) {
        val loginRequest = LoginRequest(
            access_token = tokenString
        )
        isLoading.value = Event(true)
        loginRepo.userLoginApiCall(loginRequest)
            .observeForever { data ->
                // Handle the login response
                data?.let { baseModel ->
                    isLoading.value = Event(false)
                    userLoginResponse.value = Event(baseModel.message?.data)

                } ?: run {
                    isLoading.value = Event(false)
                }
            }

    }


}