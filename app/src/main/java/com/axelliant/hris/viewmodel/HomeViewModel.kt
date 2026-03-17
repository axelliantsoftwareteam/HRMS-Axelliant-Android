package com.axelliant.hris.viewmodel

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.axelliant.hris.event.Event
import com.axelliant.hris.model.todayTeam.TodayTeamResponse
import com.axelliant.hris.model.dashboard.DashboardResponse
import com.axelliant.hris.model.login.CheckInRequest
import com.axelliant.hris.model.login.CheckInResponse
import com.axelliant.hris.model.todayTeam.EmployProfileListModel
import com.axelliant.hris.repos.HomeRepo

class HomeViewModel(private val homeRepo: HomeRepo) : BaseViewModel() {

     val dashboardResponse: MutableLiveData<Event<DashboardResponse?>> by lazy { MutableLiveData<Event<DashboardResponse?>>() }
     val employListResponse: MutableLiveData<Event<EmployProfileListModel?>> by lazy { MutableLiveData<Event<EmployProfileListModel?>>() }
     val todayTeamResponse: MutableLiveData<Event<TodayTeamResponse?>> by lazy { MutableLiveData<Event<TodayTeamResponse?>>() }
     val checkInResponse: MutableLiveData<Event<CheckInResponse?>> by lazy { MutableLiveData<Event<CheckInResponse?>>() }

    fun getTodayTeamInfo() {
        isLoading.value = Event(true)
        homeRepo.getTeamAttendData()
            .observeForever { data ->
                // Handle the login response
                data?.let { baseModel ->
                    isLoading.value = Event(false)
                    // Handle success
                    todayTeamResponse.value = Event(baseModel.message?.data)

                } ?: run {
                    isLoading.value = Event(false)
                    todayTeamResponse.value =null
                    // Handle error
                    Log.d("Success VieModel->", "false")


                }
            }


    }

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

    fun getTodayTeamList(filter:String?) {
        isLoading.value = Event(true)
        homeRepo.getTodayEmployListData(filter)
            .observeForever { data ->
                // Handle the login response
                data?.let { baseModel ->
                    isLoading.value = Event(false)
                    // Handle success
                    employListResponse.value = Event(baseModel.message?.data)

                } ?: run {
                    isLoading.value = Event(false)
                    // Handle error
                    Log.d("Success VieModel->", "false")


                }
            }


    }



}