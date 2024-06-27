package com.axelliant.hrms.viewmodel

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.axelliant.hrms.event.Event
import com.axelliant.hrms.model.ImagePath
import com.axelliant.hrms.model.attendance.AttendanceInput
import com.axelliant.hrms.model.attendance.expenseApprovalList
import com.axelliant.hrms.model.expense.CreateExpense
import com.axelliant.hrms.model.expense.DeleteAttachment
import com.axelliant.hrms.model.expense.GetExpenseResponse
import com.axelliant.hrms.model.expense.MyExpenseDetailResponse
import com.axelliant.hrms.model.expense.MyExpensePostResponse
import com.axelliant.hrms.model.leave.ExpenseApprovalStatus
import com.axelliant.hrms.model.leave.LeaveApproval
import com.axelliant.hrms.model.leave.PostExpenseImageResponse
import com.axelliant.hrms.model.leave.PostResponse
import com.axelliant.hrms.repos.ExpenseRepo

class ExpenseViewModel(private val expenseRepo: ExpenseRepo) : BaseViewModel() {
    val expenseResponse: MutableLiveData<Event<MyExpenseDetailResponse?>> by lazy { MutableLiveData<Event<MyExpenseDetailResponse?>>() }

    val postExpenseResponse: MutableLiveData<Event<PostExpenseImageResponse?>> by lazy { MutableLiveData<Event<PostExpenseImageResponse?>>() }
    val myPostExpenseResponse: MutableLiveData<Event<MyExpensePostResponse?>> by lazy { MutableLiveData<Event<MyExpensePostResponse?>>() }
    val deleteExpenseResponse: MutableLiveData<Event<MyExpensePostResponse?>> by lazy { MutableLiveData<Event<MyExpensePostResponse?>>() }
    val deleteAttachmentResponse: MutableLiveData<Event<MyExpensePostResponse?>> by lazy { MutableLiveData<Event<MyExpensePostResponse?>>() }

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
    fun getMyExpenseFile(imagePath: ImagePath) {
        isLoading.value = Event(true)
        expenseRepo.createMyExpenseImage(imagePath)
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

    fun postExpense(isUpdate:Boolean,createExpense: CreateExpense) {
        isLoading.value = Event(true)
        expenseRepo.createExpense(isUpdate,createExpense)
            .observeForever { data ->
                // Handle the login response
                data?.let { baseModel ->
                    isLoading.value = Event(false)
                    // Handle success
                    myPostExpenseResponse.value = Event(baseModel.message?.data)

                } ?: run {
                    isLoading.value = Event(false)
                    // Handle error
                    Log.d("Success VieModel->", "false")


                }
            }


    }
    fun deleteExpense(createExpense: CreateExpense) {
        isLoading.value = Event(true)
        expenseRepo.deleteExpense(createExpense)
            .observeForever { data ->
                // Handle the login response
                data?.let { baseModel ->
                    isLoading.value = Event(false)
                    // Handle success
                    deleteExpenseResponse.value = Event(baseModel.message?.data)

                } ?: run {
                    isLoading.value = Event(false)
                    // Handle error
                    Log.d("Success VieModel->", "false")


                }
            }


    }
    fun deleteAttachment(deleteAttachment: DeleteAttachment) {
        isLoading.value = Event(true)
        expenseRepo.deleteAttachment(deleteAttachment)
            .observeForever { data ->
                // Handle the login response
                data?.let { baseModel ->
                    isLoading.value = Event(false)
                    // Handle success
                    deleteAttachmentResponse.value = Event(baseModel.message?.data)
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