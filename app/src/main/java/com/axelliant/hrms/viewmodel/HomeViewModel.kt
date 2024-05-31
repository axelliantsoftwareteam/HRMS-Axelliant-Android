package com.axelliant.hrms.viewmodel

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.axelliant.hrms.event.Event
import com.axelliant.hrms.model.EmptyModel
import com.axelliant.hrms.model.attendance.AttendanceInput
import com.axelliant.hrms.model.dashboard.DashboardResponse
import com.axelliant.hrms.model.login.CheckInRequest
import com.axelliant.hrms.model.login.CheckInResponse
import com.axelliant.hrms.repos.HomeRepo

class HomeViewModel(private val homeRepo: HomeRepo) : BaseViewModel() {

     val dashboardResponse: MutableLiveData<Event<DashboardResponse?>> by lazy { MutableLiveData<Event<DashboardResponse?>>() }
     val checkInResponse: MutableLiveData<Event<CheckInResponse?>> by lazy { MutableLiveData<Event<CheckInResponse?>>() }

    fun getDashboardInformation() {
        isLoading.value = Event(true)
        homeRepo.getDashboardData()
            .observeForever { data ->
                // Handle the login response
                data?.let { baseModel ->
                    isLoading.value = Event(false)
                    // Handle success
                    dashboardResponse.value = Event(baseModel.message?.data)

                } ?: run {
                    isLoading.value = Event(false)
                    // Handle error
                    Log.d("Success VieModel->", "false")


                }
            }


    }

    fun postCheckIn(checkInRequest: CheckInRequest) {
        isLoading.value = Event(true)
        homeRepo.checkInAttendance(checkInRequest)
            .observeForever { data ->
                // Handle the login response
                data?.let { baseModel ->
                    isLoading.value = Event(false)
                    // Handle success
                    checkInResponse.value = Event(baseModel.message?.data)

                } ?: run {
                    isLoading.value = Event(false)
                    // Handle error
                    checkInResponse.value = null
                    Log.d("Success VieModel->", "false")


                }
            }


    }


}