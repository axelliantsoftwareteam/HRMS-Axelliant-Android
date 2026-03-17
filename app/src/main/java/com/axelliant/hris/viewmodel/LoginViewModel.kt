package com.axelliant.hris.viewmodel

import androidx.lifecycle.MutableLiveData
import com.axelliant.hris.event.Event
import com.axelliant.hris.model.login.LoginRequest
import com.axelliant.hris.model.login.UserLoginResponse
import com.axelliant.hris.repos.LoginRepo

class LoginViewModel(private val loginRepo: LoginRepo) : BaseViewModel() {
    val userLoginResponse: MutableLiveData<Event<UserLoginResponse?>> by lazy { MutableLiveData<Event<UserLoginResponse?>>() }


    fun postMicToken(tokenString: String) {
        val loginRequest = LoginRequest(
            microsoft_token = tokenString
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