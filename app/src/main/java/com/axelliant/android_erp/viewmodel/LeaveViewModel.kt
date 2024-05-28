package com.axelliant.android_erp.viewmodel

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.axelliant.android_erp.enums.AttendanceFilter
import com.axelliant.android_erp.event.Event
import com.axelliant.android_erp.model.attendance.AttendanceInput
import com.axelliant.android_erp.model.leave.LeaveResponse
import com.axelliant.android_erp.model.leave.MyLeaveDetailResponse
import com.axelliant.android_erp.repos.LeaveRepo

class LeaveViewModel(private val leaveRepo: LeaveRepo) : BaseViewModel() {

     val leaveStatResponse: MutableLiveData<Event<LeaveResponse?>> by lazy { MutableLiveData<Event<LeaveResponse?>>() }
     val myLeaveDetailResponse: MutableLiveData<Event<MyLeaveDetailResponse?>> by lazy { MutableLiveData<Event<MyLeaveDetailResponse?>>() }

    fun getLeaveStats(attendanceInput: AttendanceInput) {
        isLoading.value = Event(true)
        leaveRepo.getLeaveStats(attendanceInput)
            .observeForever { data ->
                // Handle the login response
                data?.let { baseModel ->
                    isLoading.value = Event(false)
                    // Handle success
                    leaveStatResponse.value = Event(baseModel.message?.data)

                } ?: run {
                    isLoading.value = Event(false)
                    // Handle error
                    Log.d("Success VieModel->", "false")


                }
            }


    }


    fun getMyLeaveDetail(attendanceInput: AttendanceInput) {
        isLoading.value = Event(true)
        leaveRepo.getMyLeaveDetail(attendanceInput)
            .observeForever { data ->
                // Handle the login response
                data?.let { baseModel ->
                    isLoading.value = Event(false)
                    // Handle success
                    myLeaveDetailResponse.value = Event(baseModel.message?.data)

                } ?: run {
                    isLoading.value = Event(false)
                    // Handle error
                    Log.d("Success VieModel->", "false")


                }
            }


    }



}