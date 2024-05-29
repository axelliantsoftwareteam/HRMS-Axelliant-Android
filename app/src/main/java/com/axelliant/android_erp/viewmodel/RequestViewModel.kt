package com.axelliant.android_erp.viewmodel

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.axelliant.android_erp.enums.AttendanceFilter
import com.axelliant.android_erp.event.Event
import com.axelliant.android_erp.model.attendance.AttendanceInput
import com.axelliant.android_erp.model.leave.LeaveResponse
import com.axelliant.android_erp.model.leave.MyLeaveDetailResponse
import com.axelliant.android_erp.model.post.AttendanceRequest
import com.axelliant.android_erp.model.post.LeaveRequest
import com.axelliant.android_erp.repos.LeaveRepo
import com.axelliant.android_erp.repos.RequestRepo

class RequestViewModel(private val leaveRepo: RequestRepo) : BaseViewModel() {

    val leaveRequestResponse: MutableLiveData<Event<LeaveResponse?>> by lazy { MutableLiveData<Event<LeaveResponse?>>() }
    val attendanceRequestResponse: MutableLiveData<Event<LeaveResponse?>> by lazy { MutableLiveData<Event<LeaveResponse?>>() }
    fun postLeaveQuest(leaveRequest: LeaveRequest) {
        isLoading.value = Event(true)
        leaveRepo.postLeaveRequest(leaveRequest)
            .observeForever { data ->
                // Handle the login response
                data?.let { baseModel ->
                    isLoading.value = Event(false)
                    // Handle success
                    leaveRequestResponse.value = Event(baseModel.message?.data)

                } ?: run {
                    isLoading.value = Event(false)
                    // Handle error
                    Log.d("Success VieModel->", "false")


                }
            }


    }
    fun postAttendanceQuest(attendanceRequest: AttendanceRequest) {
        isLoading.value = Event(true)
        leaveRepo.postAttendanceRequest(attendanceRequest)
            .observeForever { data ->
                // Handle the login response
                data?.let { baseModel ->
                    isLoading.value = Event(false)
                    // Handle success
                    attendanceRequestResponse.value = Event(baseModel.message?.data)

                } ?: run {
                    isLoading.value = Event(false)
                    // Handle error
                    Log.d("Success VieModel->", "false")


                }
            }


    }
}