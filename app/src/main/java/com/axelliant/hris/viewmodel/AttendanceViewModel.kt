package com.axelliant.hris.viewmodel

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.axelliant.hris.event.Event
import com.axelliant.hris.model.attendance.AttendanceApproval
import com.axelliant.hris.model.attendance.AttendanceInput
import com.axelliant.hris.model.attendance.AttendanceResponse
import com.axelliant.hris.model.attendance.AttendanceStatsResponse
import com.axelliant.hris.model.attendance.LeaveCountInput
import com.axelliant.hris.model.attendance.TeamAttendanceResponse
import com.axelliant.hris.model.checkin.CheckInListResponse
import com.axelliant.hris.model.leave.LeaveApproval
import com.axelliant.hris.model.leave.PostResponse
import com.axelliant.hris.repos.AttendanceRepo

class AttendanceViewModel(private val attendanceRepo: AttendanceRepo) : BaseViewModel() {

     val attendanceResponse: MutableLiveData<Event<AttendanceStatsResponse?>> by lazy { MutableLiveData<Event<AttendanceStatsResponse?>>() }
     val attendanceDetailResponse: MutableLiveData<Event<AttendanceResponse?>> by lazy { MutableLiveData<Event<AttendanceResponse?>>() }
     val teamAttendanceResponse: MutableLiveData<Event<TeamAttendanceResponse?>> by lazy { MutableLiveData<Event<TeamAttendanceResponse?>>() }
     val attendanceApproval: MutableLiveData<Event<AttendanceApproval?>> by lazy { MutableLiveData<Event<AttendanceApproval?>>() }
    val attendanceApprovalResponse: MutableLiveData<Event<PostResponse?>> by lazy { MutableLiveData<Event<PostResponse?>>() }
    val checkInListResponse: MutableLiveData<Event<CheckInListResponse?>> by lazy { MutableLiveData<Event<CheckInListResponse?>>() }

    fun getAttendanceStats(attendanceInput: AttendanceInput) {
        isLoading.value = Event(true)
        attendanceRepo.getAttendanceStats(attendanceInput)
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




    fun getTeamAttendance(inputObject: AttendanceInput) {
        isLoading.value = Event(true)
        attendanceRepo.getTeamAttendanceDetail(inputObject)
            .observeForever { data ->
                // Handle the login response
                data?.let { baseModel ->
                    isLoading.value = Event(false)
                    // Handle success
                    teamAttendanceResponse.value = Event(baseModel.message?.data)

                } ?: run {
                    isLoading.value = Event(false)
                    // Handle error
                    Log.d("Success VieModel->", "false")


                }
            }


    }


    fun getAttendanceApproval(inputObject: AttendanceInput) {
        isLoading.value = Event(true)
        attendanceRepo.attendanceApproval(inputObject)
            .observeForever { data ->
                // Handle the login response
                data?.let { baseModel ->
                    isLoading.value = Event(false)
                    // Handle success
                    attendanceApproval.value = Event(baseModel.message?.data)

                } ?: run {
                    isLoading.value = Event(false)
                    // Handle error
                    Log.d("Success VieModel->", "false")


                }
            }


    }



    fun attendanceApprovalStatus(inputObject: LeaveApproval) {
        isLoading.value = Event(true)
        attendanceRepo.attendanceApprovalStatus(inputObject)
            .observeForever { data ->
                // Handle the login response
                data?.let { baseModel ->
                    isLoading.value = Event(false)
                    // Handle success
                    attendanceApprovalResponse.value = Event(baseModel.message?.data)

                } ?: run {
                    isLoading.value = Event(false)
                    // Handle error
                    Log.d("Success VieModel->", "false")


                }
            }


    }

    fun bulkAttendanceApprovalStatus(checkinIds: List<String>, status: String) {
        isLoading.value = Event(true)
        attendanceRepo.bulkAttendanceApprovalStatus(checkinIds, status)
            .observeForever { data ->
                data?.let { baseModel ->
                    isLoading.value = Event(false)
                    attendanceApprovalResponse.value = Event(baseModel.message?.data)
                } ?: run {
                    isLoading.value = Event(false)
                    Log.d("Success VieModel->", "false")
                }
            }
    }

    fun getCheckInList(inputObject: LeaveCountInput) {
        isLoading.value = Event(true)
        attendanceRepo.checkInList(inputObject)
            .observeForever { data ->
                // Handle the login response
                data?.let { baseModel ->
                    isLoading.value = Event(false)
                    // Handle success
                    checkInListResponse.value = Event(baseModel.message?.data)

                } ?: run {
                    isLoading.value = Event(false)
                    // Handle error
                    Log.d("Success VieModel->", "false")


                }
            }


    }



}
