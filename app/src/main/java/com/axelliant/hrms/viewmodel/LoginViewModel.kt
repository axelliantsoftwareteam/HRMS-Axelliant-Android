package com.axelliant.hrms.viewmodel

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.axelliant.hrms.event.Event
import com.axelliant.hrms.model.base.BaseApiModel
import com.axelliant.hrms.model.dashboard.DashboardResponse
import com.axelliant.hrms.model.login.LoginRequest
import com.axelliant.hrms.model.login.UserLoginResponse
import com.axelliant.hrms.repos.LoginRepo

class LoginViewModel(private val loginRepo: LoginRepo) : BaseViewModel() {
    val userLoginResponse: MutableLiveData<Event<UserLoginResponse?>> by lazy { MutableLiveData<Event<UserLoginResponse?>>() }


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