package com.axelliant.android_erp.viewmodel

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.axelliant.android_erp.event.Event
import com.axelliant.android_erp.model.dashboard.DashboardResponse
import com.axelliant.android_erp.repos.HomeRepo

class HomeViewModel(private val homeRepo: HomeRepo) : BaseViewModel() {

     val birthdayResponse: MutableLiveData<Event<DashboardResponse?>> by lazy { MutableLiveData<Event<DashboardResponse?>>() }

    fun getBirthdayList() {
        isLoading.value = Event(true)
        homeRepo.getDashboardData()
            .observeForever { data ->
                // Handle the login response
                data?.let { baseModel ->
                    isLoading.value = Event(false)
                    // Handle success
                    birthdayResponse.value = Event(baseModel.message?.data)

                } ?: run {
                    isLoading.value = Event(false)
                    // Handle error
                    Log.d("Success VieModel->", "false")


                }
            }


    }


}