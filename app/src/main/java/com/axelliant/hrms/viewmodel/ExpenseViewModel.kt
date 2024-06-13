package com.axelliant.hrms.viewmodel

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.axelliant.hrms.event.Event
import com.axelliant.hrms.model.attendance.AttendanceInput
import com.axelliant.hrms.model.expense.AddExpense
import com.axelliant.hrms.model.expense.CreateExpense
import com.axelliant.hrms.model.expense.MyExpenseDetailResponse
import com.axelliant.hrms.model.leave.LeaveResponse
import com.axelliant.hrms.model.leave.MyUpcomingLeaveDetailResponse
import com.axelliant.hrms.model.leave.PostResponse
import com.axelliant.hrms.model.leave.TeamLeaveDetailResponse
import com.axelliant.hrms.model.post.LeaveRequest
import com.axelliant.hrms.repos.ExpenseRepo

class ExpenseViewModel(private val expenseRepo: ExpenseRepo) : BaseViewModel() {
    val expenseResponse: MutableLiveData<Event<MyExpenseDetailResponse?>> by lazy { MutableLiveData<Event<MyExpenseDetailResponse?>>() }

    val postExpenseResponse: MutableLiveData<Event<PostResponse?>> by lazy { MutableLiveData<Event<PostResponse?>>() }


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

    fun postExpense(createExpense: CreateExpense) {
        isLoading.value = Event(true)
        expenseRepo.createExpense(createExpense)
            .observeForever { data ->
                // Handle the login response
                data?.let { baseModel ->
                    isLoading.value = Event(false)
                    // Handle success
                    postExpenseResponse.value = Event(baseModel.message?.data)

                } ?: run {
                    isLoading.value = Event(false)
                    // Handle error
                    Log.d("Success VieModel->", "false")


                }
            }


    }
}