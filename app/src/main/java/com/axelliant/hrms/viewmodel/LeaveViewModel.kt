package com.axelliant.hrms.viewmodel

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.axelliant.hrms.event.Event
import com.axelliant.hrms.model.attendance.AttendanceInput
import com.axelliant.hrms.model.leave.LeaveResponse
import com.axelliant.hrms.model.leave.MyLeaveDetailResponse
import com.axelliant.hrms.repos.LeaveRepo

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