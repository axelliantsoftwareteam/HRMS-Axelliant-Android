package com.axelliant.hrms.viewmodel

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.axelliant.hrms.event.Event
import com.axelliant.hrms.model.leave.GetAttendanceResponse
import com.axelliant.hrms.model.leave.GetLeavesResponse
import com.axelliant.hrms.model.leave.LeaveResponse
import com.axelliant.hrms.model.leave.PostResponse
import com.axelliant.hrms.model.post.AttendanceRequest
import com.axelliant.hrms.model.post.LeaveRequest
import com.axelliant.hrms.repos.RequestRepo

class RequestViewModel(private val leaveRepo: RequestRepo) : BaseViewModel() {

    val postLeaveResponse: MutableLiveData<Event<PostResponse?>> by lazy { MutableLiveData<Event<PostResponse?>>() }
    val attendanceRequestResponse: MutableLiveData<Event<PostResponse?>> by lazy { MutableLiveData<Event<PostResponse?>>() }

    val leaveTypes: MutableLiveData<Event<GetLeavesResponse?>> by lazy { MutableLiveData<Event<GetLeavesResponse?>>() }
    val attendanceRequestInfo: MutableLiveData<Event<GetAttendanceResponse?>> by lazy { MutableLiveData<Event<GetAttendanceResponse?>>() }

    fun getLeaves() {
        isLoading.value = Event(true)
        leaveRepo.getLeaves()
            .observeForever { data ->
                // Handle the login response
                data?.let { baseModel ->
                    isLoading.value = Event(false)
                    // Handle success
                    leaveTypes.value = Event(baseModel.message?.data)

                } ?: run {
                    isLoading.value = Event(false)
                    // Handle error
                    Log.d("Success VieModel->", "false")


                }
            }


    }


    fun getAttendanceRequestInfo() {
        isLoading.value = Event(true)
        leaveRepo.getAttendanceInfo()
            .observeForever { data ->
                // Handle the login response
                data?.let { baseModel ->
                    isLoading.value = Event(false)
                    // Handle success
                    attendanceRequestInfo.value = Event(baseModel.message?.data)

                } ?: run {
                    isLoading.value = Event(false)
                    // Handle error
                    Log.d("Success VieModel->", "false")


                }
            }


    }

    fun postLeaveQuest(leaveRequest: LeaveRequest) {
        isLoading.value = Event(true)
        leaveRepo.postLeaveRequest(leaveRequest)
            .observeForever { data ->
                // Handle the login response
                data?.let { baseModel ->
                    isLoading.value = Event(false)
                    // Handle success
                    postLeaveResponse.value = Event(baseModel.message?.data)

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