package com.axelliant.hrms.viewmodel

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.axelliant.hrms.enums.AttendanceFilter
import com.axelliant.hrms.event.Event
import com.axelliant.hrms.model.attendance.AttendanceInput
import com.axelliant.hrms.model.attendance.AttendanceResponse
import com.axelliant.hrms.model.attendance.AttendanceStatsResponse
import com.axelliant.hrms.model.attendance.TeamAttendanceResponse
import com.axelliant.hrms.repos.AttendanceRepo

class AttendanceViewModel(private val attendanceRepo: AttendanceRepo) : BaseViewModel() {

     val attendanceResponse: MutableLiveData<Event<AttendanceStatsResponse?>> by lazy { MutableLiveData<Event<AttendanceStatsResponse?>>() }
     val attendanceDetailResponse: MutableLiveData<Event<AttendanceResponse?>> by lazy { MutableLiveData<Event<AttendanceResponse?>>() }
     val teamAttendanceResponse: MutableLiveData<Event<TeamAttendanceResponse?>> by lazy { MutableLiveData<Event<TeamAttendanceResponse?>>() }

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


}