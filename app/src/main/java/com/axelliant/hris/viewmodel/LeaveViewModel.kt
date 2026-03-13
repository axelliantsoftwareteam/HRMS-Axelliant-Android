package com.axelliant.hris.viewmodel

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.axelliant.hris.event.Event
import com.axelliant.hris.model.attendance.AttendanceInput
import com.axelliant.hris.model.leave.LeaveApproval
import com.axelliant.hris.model.leave.LeaveResponse
import com.axelliant.hris.model.leave.MyLeaveDetailResponse
import com.axelliant.hris.model.leave.MyUpcomingLeaveDetailResponse
import com.axelliant.hris.model.leave.PostResponse
import com.axelliant.hris.model.leave.TeamLeaveDetailResponse
import com.axelliant.hris.model.leave.UpcomingLeaveInput
import com.axelliant.hris.repos.LeaveRepo

class LeaveViewModel(private val leaveRepo: LeaveRepo) : BaseViewModel() {

     val leaveStatResponse: MutableLiveData<Event<LeaveResponse?>> by lazy { MutableLiveData<Event<LeaveResponse?>>() }
     val upcomingLeavesResponse: MutableLiveData<Event<MyUpcomingLeaveDetailResponse?>> by lazy { MutableLiveData<Event<MyUpcomingLeaveDetailResponse?>>() }
     val myLeaveDetailResponse: MutableLiveData<Event<MyLeaveDetailResponse?>> by lazy { MutableLiveData<Event<MyLeaveDetailResponse?>>() }
     val teamLeaveDetailResponse: MutableLiveData<Event<TeamLeaveDetailResponse?>> by lazy { MutableLiveData<Event<TeamLeaveDetailResponse?>>() }
     val leaveApprovalResponse: MutableLiveData<Event<PostResponse?>> by lazy { MutableLiveData<Event<PostResponse?>>() }

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


    fun getTeamLeaveDetail(attendanceInput: AttendanceInput) {
        isLoading.value = Event(true)
        leaveRepo.getTeamLeaveDetail(attendanceInput)
            .observeForever { data ->
                // Handle the login response
                data?.let { baseModel ->
                    isLoading.value = Event(false)
                    // Handle success
                    teamLeaveDetailResponse.value = Event(baseModel.message?.data)

                } ?: run {
                    isLoading.value = Event(false)
                    // Handle error
                    Log.d("Success VieModel->", "false")


                }
            }


    }
    fun getUpcomingLeaveDetail(upcomingLeaveInput: UpcomingLeaveInput) {
        isLoading.value = Event(true)
        leaveRepo.getUpcomingLeaveDetail(upcomingLeaveInput)
            .observeForever { data ->
                // Handle the login response
                data?.let { baseModel ->
                    isLoading.value = Event(false)
                    // Handle success
                    upcomingLeavesResponse.value = Event(baseModel.message?.data)

                } ?: run {
                    isLoading.value = Event(false)
                    // Handle error
                    Log.d("Success VieModel->", "false")


                }
            }


    }


    fun leaveApprovalStatus(attendanceInput: LeaveApproval) {
        isLoading.value = Event(true)
        leaveRepo.leaveApproval(attendanceInput)
            .observeForever { data ->
                // Handle the login response
                data?.let { baseModel ->
                    isLoading.value = Event(false)
                    // Handle success
                    leaveApprovalResponse.value = Event(baseModel.message?.data)

                } ?: run {
                    isLoading.value = Event(false)
                    // Handle error
                    Log.d("Success VieModel->", "false")


                }
            }


    }

    fun bulkLeaveApprovalStatus(leaveIds: List<String>, status: String) {
        isLoading.value = Event(true)
        leaveRepo.bulkLeaveApproval(leaveIds, status)
            .observeForever { data ->
                data?.let { baseModel ->
                    isLoading.value = Event(false)
                    leaveApprovalResponse.value = Event(baseModel.message?.data)
                } ?: run {
                    isLoading.value = Event(false)
                    Log.d("Success VieModel->", "false")
                }
            }
    }
}
