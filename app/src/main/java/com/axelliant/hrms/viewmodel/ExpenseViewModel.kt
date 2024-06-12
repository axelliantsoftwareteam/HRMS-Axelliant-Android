package com.axelliant.hrms.viewmodel

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.axelliant.hrms.event.Event
import com.axelliant.hrms.model.attendance.AttendanceInput
import com.axelliant.hrms.model.expense.MyExpenseDetailResponse
import com.axelliant.hrms.model.leave.LeaveResponse
import com.axelliant.hrms.model.leave.MyUpcomingLeaveDetailResponse
import com.axelliant.hrms.model.leave.PostResponse
import com.axelliant.hrms.model.leave.TeamLeaveDetailResponse
import com.axelliant.hrms.repos.ExpenseRepo

class ExpenseViewModel(private val expenseRepo: ExpenseRepo) : BaseViewModel() {

    val leaveStatResponse: MutableLiveData<Event<LeaveResponse?>> by lazy { MutableLiveData<Event<LeaveResponse?>>() }
    val upcomingLeavesResponse: MutableLiveData<Event<MyUpcomingLeaveDetailResponse?>> by lazy { MutableLiveData<Event<MyUpcomingLeaveDetailResponse?>>() }
    val expenseResponse: MutableLiveData<Event<MyExpenseDetailResponse?>> by lazy { MutableLiveData<Event<MyExpenseDetailResponse?>>() }
    val teamLeaveDetailResponse: MutableLiveData<Event<TeamLeaveDetailResponse?>> by lazy { MutableLiveData<Event<TeamLeaveDetailResponse?>>() }
    val leaveApprovalResponse: MutableLiveData<Event<PostResponse?>> by lazy { MutableLiveData<Event<PostResponse?>>() }


    fun getMyExpenseDetail(attendanceInput: AttendanceInput) {
        isLoading.value = Event(true)
        expenseRepo.getMyExpenseDetail(attendanceInput)
            .observeForever { data ->
                // Handle the login response
                data?.let { baseModel ->
                    isLoading.value = Event(false)
                    // Handle success
                    expenseResponse.value = Event(baseModel.message?.data)

                } ?: run {
                    isLoading.value = Event(false)
                    // Handle error
                    Log.d("Success VieModel->", "false")


                }
            }
    }
}