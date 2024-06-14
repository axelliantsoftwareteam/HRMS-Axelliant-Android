package com.axelliant.hrms.viewmodel

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.axelliant.hrms.event.Event
import com.axelliant.hrms.model.attendance.AttendanceInput
import com.axelliant.hrms.model.attendance.expenseApprovalList
import com.axelliant.hrms.model.expense.CreateExpense
import com.axelliant.hrms.model.expense.GetExpenseResponse
import com.axelliant.hrms.model.expense.MyExpenseDetailResponse
import com.axelliant.hrms.model.leave.ExpenseApprovalStatus
import com.axelliant.hrms.model.leave.LeaveApproval
import com.axelliant.hrms.model.leave.PostResponse
import com.axelliant.hrms.repos.ExpenseRepo

class ExpenseViewModel(private val expenseRepo: ExpenseRepo) : BaseViewModel() {
    val expenseResponse: MutableLiveData<Event<MyExpenseDetailResponse?>> by lazy { MutableLiveData<Event<MyExpenseDetailResponse?>>() }

    val postExpenseResponse: MutableLiveData<Event<PostResponse?>> by lazy { MutableLiveData<Event<PostResponse?>>() }

    val expenseTypeResponse: MutableLiveData<Event<GetExpenseResponse?>> by lazy { MutableLiveData<Event<GetExpenseResponse?>>() }
    val expenseApproval: MutableLiveData<Event<expenseApprovalList?>> by lazy { MutableLiveData<Event<expenseApprovalList?>>() }

    val expenseApprovalResponse: MutableLiveData<Event<PostResponse?>> by lazy { MutableLiveData<Event<PostResponse?>>() }

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

    fun getExpenseTypeList() {
        isLoading.value = Event(true)
        expenseRepo.getExpenseTypes()
            .observeForever { data ->
                // Handle the login response
                data?.let { baseModel ->
                    isLoading.value = Event(false)
                    // Handle success
                    expenseTypeResponse.value = Event(baseModel.message?.data)

                } ?: run {
                    isLoading.value = Event(false)
                    // Handle error
                    Log.d("Success VieModel->", "false")


                }
            }


    }

    fun getExpenseApproval(inputObject: AttendanceInput) {
        isLoading.value = Event(true)
        expenseRepo.expenseListApproval(inputObject)
            .observeForever { data ->
                // Handle the login response
                data?.let { baseModel ->
                    isLoading.value = Event(false)
                    // Handle success
                    expenseApproval.value = Event(baseModel.message?.data)

                } ?: run {
                    isLoading.value = Event(false)
                    // Handle error
                    Log.d("Success VieModel->", "false")


                }
            }


    }

    fun expenseApprovalStatus(inputObject: ExpenseApprovalStatus) {
        isLoading.value = Event(true)
        expenseRepo.expenseApprovalStatus(inputObject)
            .observeForever { data ->
                // Handle the login response
                data?.let { baseModel ->
                    isLoading.value = Event(false)
                    // Handle success
                    expenseApprovalResponse.value = Event(baseModel.message?.data)

                } ?: run {
                    isLoading.value = Event(false)
                    // Handle error
                    Log.d("Success VieModel->", "false")


                }
            }


    }
}