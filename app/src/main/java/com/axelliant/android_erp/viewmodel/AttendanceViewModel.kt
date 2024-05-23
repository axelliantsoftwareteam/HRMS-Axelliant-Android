package com.axelliant.android_erp.viewmodel

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.axelliant.android_erp.enums.AttendanceFilter
import com.axelliant.android_erp.event.Event
import com.axelliant.android_erp.model.attendance.AttendanceInput
import com.axelliant.android_erp.model.attendance.AttendanceResponse
import com.axelliant.android_erp.model.attendance.AttendanceStatsResponse
import com.axelliant.android_erp.repos.AttendanceRepo

class AttendanceViewModel(private val attendanceRepo: AttendanceRepo) : BaseViewModel() {

     val attendanceResponse: MutableLiveData<Event<AttendanceStatsResponse?>> by lazy { MutableLiveData<Event<AttendanceStatsResponse?>>() }
     val attendanceDetailResponse: MutableLiveData<Event<AttendanceResponse?>> by lazy { MutableLiveData<Event<AttendanceResponse?>>() }

    fun getAttendanceStats(currentFilter: AttendanceFilter) {
        isLoading.value = Event(true)
        attendanceRepo.getAttendanceStats(currentFilter)
            .observeForever { data ->
                // Handle the login response
                data?.let { baseModel ->
                    isLoading.value = Event(false)
                    // Handle success
                    attendanceResponse.value = Event(baseModel.message?.data)

                } ?: run {
                    isLoading.value = Event(false)
                    // Handle error
                    Log.d("Success VieModel->", "false")


                }
            }


    }

    fun getAttendanceDetail(inputObject: AttendanceInput) {
        isLoading.value = Event(true)
        attendanceRepo.getAttendanceDetail(inputObject)
            .observeForever { data ->
                // Handle the login response
                data?.let { baseModel ->
                    isLoading.value = Event(false)
                    // Handle success
                    attendanceDetailResponse.value = Event(baseModel.message?.data)

                } ?: run {
                    isLoading.value = Event(false)
                    // Handle error
                    Log.d("Success VieModel->", "false")


                }
            }


    }



}