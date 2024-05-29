package com.axelliant.hrms.viewmodel

import android.util.Log
import com.axelliant.hrms.event.Event
import com.axelliant.hrms.repos.LoginRepo

class LoginViewModel(private val loginRepo: LoginRepo) : BaseViewModel(){

    fun getConversationCall(userId: Int) {
        isLoading.value = Event(true)
        loginRepo.callSampleApi(userId)
            .observeForever { data ->
                // Handle the login response
                data?.let { conversation ->
                    isLoading.value = Event(false)
                    // Handle success

                    Log.d("Success VieModel->","true")

                } ?: run {
                    isLoading.value = Event(false)
                    // Handle error
                    Log.d("Success VieModel->","false")


                }
            }


    }




}