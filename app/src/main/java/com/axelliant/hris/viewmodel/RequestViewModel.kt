package com.axelliant.hris.viewmodel

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.axelliant.hris.event.Event
import com.axelliant.hris.model.leave.GetAttendanceResponse
import com.axelliant.hris.model.leave.GetLeavesResponse
import com.axelliant.hris.model.leave.PostResponse
import com.axelliant.hris.model.leave.leaveCount.GetLeaveCount
import com.axelliant.hris.model.leave.leaveCount.LeaveCountByDate
import com.axelliant.hris.model.leave.leaveCount.LeaveCountByDaysRequest
import com.axelliant.hris.model.post.AttendanceRequest
import com.axelliant.hris.model.post.LeaveRequest
import com.axelliant.hris.repos.RequestRepo

class RequestViewModel(private val leaveRepo: RequestRepo) : BaseViewModel() {

    val deleteLeaveResponse: MutableLiveData<Event<PostResponse?>> by lazy { MutableLiveData<Event<PostResponse?>>() }
    val updateLeaveResponse: MutableLiveData<Event<PostResponse?>> by lazy { MutableLiveData<Event<PostResponse?>>() }
    val leaveCountByDate: MutableLiveData<Event<LeaveCountByDate?>> by lazy { MutableLiveData<Event<LeaveCountByDate?>>() }
    val postLeaveResponse: MutableLiveData<Event<PostResponse?>> by lazy { MutableLiveData<Event<PostResponse?>>() }
    val attendanceRequestResponse: MutableLiveData<Event<PostResponse?>> by lazy { MutableLiveData<Event<PostResponse?>>() }

    val leaveTypes: MutableLiveData<Event<GetLeavesResponse?>> by lazy { MutableLiveData<Event<GetLeavesResponse?>>() }
    val getLeaveTypesCount: MutableLiveData<Event<GetLeaveCount?>> by lazy { MutableLiveData<Event<GetLeaveCount?>>() }
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


    fun getLeavesCount() {
        isLoading.value = Event(true)
        leaveRepo.getLeavesTypeWithCount()
            .observeForever { data ->
                // Handle the login response
                data?.let { baseModel ->
                    isLoading.value = Event(false)
                    // Handle success
                    getLeaveTypesCount.value = Event(baseModel.message?.data)

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

    fun deleteLeaveQuest(leaveRequest: LeaveRequest) {
        isLoading.value = Event(true)
        leaveRepo.deleteLeaveRequest(leaveRequest)
            .observeForever { data ->
                // Handle the login response
                data?.let { baseModel ->
                    isLoading.value = Event(false)
                    // Handle success
                    updateLeaveResponse.value = Event(baseModel.message?.data)

                } ?: run {
                    isLoading.value = Event(false)
                    // Handle error
                    Log.d("Success VieModel->", "false")


                }
            }


    }

    fun getLeaveCountOnDate(leaveCountByDaysRequest: LeaveCountByDaysRequest) {
        isLoading.value = Event(true)
        leaveRepo.getLeaveCountRequest(leaveCountByDaysRequest)
            .observeForever { data ->
                // Handle the login response
                data?.let { baseModel ->
                    isLoading.value = Event(false)
                    // Handle success
                    leaveCountByDate.value = Event(baseModel.message?.data)

                } ?: run {
                    isLoading.value = Event(false)
                    // Handle error
                    Log.d("Success VieModel->", "false")


                }
            }


    }

    fun updateLeaveQuest(leaveRequest: LeaveRequest) {
        isLoading.value = Event(true)
        leaveRepo.updateLeaveRequest(leaveRequest)
            .observeForever { data ->
                // Handle the login response
                data?.let { baseModel ->
                    isLoading.value = Event(false)
                    // Handle success
                    updateLeaveResponse.value = Event(baseModel.message?.data)

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


    fun updateAttendanceQuest(leaveRequest: AttendanceRequest) {
        isLoading.value = Event(true)
        leaveRepo.updateAttendanceRequest(leaveRequest)
            .observeForever { data ->
                // Handle the login response
                data?.let { baseModel ->
                    isLoading.value = Event(false)
                    // Handle success
                    updateLeaveResponse.value = Event(baseModel.message?.data)

                } ?: run {
                    isLoading.value = Event(false)
                    // Handle error
                    Log.d("Success VieModel->", "false")


                }
            }


    }


    fun deleteAttendanceQuest(leaveRequest: LeaveRequest) {
        isLoading.value = Event(true)
        leaveRepo.deleteAttendanceRequest(leaveRequest)
            .observeForever { data ->
                // Handle the login response
                data?.let { baseModel ->
                    isLoading.value = Event(false)
                    // Handle success
                    updateLeaveResponse.value = Event(baseModel.message?.data)

                } ?: run {
                    isLoading.value = Event(false)
                    // Handle error
                    Log.d("Success VieModel->", "false")


                }
            }


    }
}