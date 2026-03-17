package com.axelliant.hris.viewmodel

import android.util.Log
import androidx.databinding.ObservableField
import androidx.lifecycle.MutableLiveData
import com.axelliant.hris.event.Event
import com.axelliant.hris.model.attendance.AttendanceInput
import com.axelliant.hris.model.attendance.expenseApprovalList
import com.axelliant.hris.model.documentRequest.MyDocumentResponse
import com.axelliant.hris.model.documentRequest.SubmitDocument
import com.axelliant.hris.model.leave.ExpenseApprovalStatus
import com.axelliant.hris.model.leave.PostResponse
import com.axelliant.hris.model.resourceManage.CreateResourceHour
import com.axelliant.hris.model.resourceManage.DeleteProject
import com.axelliant.hris.model.resourceManage.GetListProject
import com.axelliant.hris.model.resourceManage.MyHoursDetails
import com.axelliant.hris.model.resourceManage.PostHoursRequestResponse
import com.axelliant.hris.repos.ResourceManageRepo
import com.axelliant.hris.utils.Validator

class ResourceManageViewModel(private val resourceManageRepo: ResourceManageRepo, private val validator: Validator) : BaseViewModel() {

    var subject = ObservableField<String>()
    var description = ObservableField<String>()

    val _subjectErrorLiveData = MutableLiveData<String?>()
    val subjectError: MutableLiveData<String?> get() = _subjectErrorLiveData

    private val _descriptionErrorLiveData = MutableLiveData<String?>()
    val descriptionError: MutableLiveData<String?> get() = _descriptionErrorLiveData

    val hoursResponse: MutableLiveData<Event<MyHoursDetails?>> by lazy { MutableLiveData<Event<MyHoursDetails?>>() }
    val documentResponse: MutableLiveData<Event<MyDocumentResponse?>> by lazy { MutableLiveData<Event<MyDocumentResponse?>>() }

    val myPostExpenseResponse: MutableLiveData<Event<PostHoursRequestResponse?>> by lazy { MutableLiveData<Event<PostHoursRequestResponse?>>() }
    val deleteExpenseResponse: MutableLiveData<Event<PostHoursRequestResponse?>> by lazy { MutableLiveData<Event<PostHoursRequestResponse?>>() }

    val projectTypeResponse: MutableLiveData<Event<GetListProject?>> by lazy { MutableLiveData<Event<GetListProject?>>() }

    val postResponse: MutableLiveData<Event<PostResponse?>> by lazy { MutableLiveData<Event<PostResponse?>>() }


    fun getMyHoursDetail(attendanceInput: AttendanceInput) {
        isLoading.value = Event(true)
        resourceManageRepo.getMyHoursDetail(attendanceInput)
            .observeForever { data ->
                // Handle the login response
                data?.let { baseModel ->
                    isLoading.value = Event(false)
                    // Handle success
                    hoursResponse.value = Event(baseModel.message?.data)

                } ?: run {
                    isLoading.value = Event(false)
                    // Handle error
                    Log.d("Success VieModel->", "false")


                }
            }
    }
    fun submitResourceHour(submitDocument: SubmitDocument)
    {
        isLoading.value = Event(true)
        resourceManageRepo.submitResourceHour(submitDocument)
            .observeForever { data ->
                // Handle the login response
                data?.let { baseModel ->
                    isLoading.value = Event(false)
                    // Handle success
                    postResponse.value = Event(baseModel.message?.data)

                } ?: run {
                    isLoading.value = Event(false)
                    // Handle error
                    Log.d("Success VieModel->", "false")


                }
            }


    }


    fun postResourceHour(isUpdate:Boolean, createResourceHour: CreateResourceHour) {
        isLoading.value = Event(true)
        resourceManageRepo.createResourceHour(isUpdate,createResourceHour)
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


    fun deleteExpense(deleteProject: DeleteProject) {
        isLoading.value = Event(true)
        resourceManageRepo.deleteProject(deleteProject)
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

    fun getProjectTypeList() {
        isLoading.value = Event(true)
        resourceManageRepo.getProjectsTypes()
            .observeForever { data ->
                // Handle the login response
                data?.let { baseModel ->
                    isLoading.value = Event(false)
                    // Handle success
                    projectTypeResponse.value = Event(baseModel.message?.data)

                } ?: run {
                    isLoading.value = Event(false)
                    // Handle error
                    Log.d("Success VieModel->", "false")


                }
            }


    }

    fun getResourcesApproval(inputObject: AttendanceInput) {
        isLoading.value = Event(true)
        resourceManageRepo.resourceListApproval(inputObject)
            .observeForever { data ->
                // Handle the login response
                data?.let { baseModel ->
                    isLoading.value = Event(false)
                    // Handle success
                    hoursResponse.value = Event(baseModel.message?.data)

                } ?: run {
                    isLoading.value = Event(false)
                    // Handle error
                    Log.d("Success VieModel->", "false")


                }
            }
    }

    fun expenseApprovalStatus(inputObject: ExpenseApprovalStatus) {
        isLoading.value = Event(true)
        resourceManageRepo.expenseApprovalStatus(inputObject)
            .observeForever { data ->
                // Handle the login response
                data?.let { baseModel ->
                    isLoading.value = Event(false)
                    // Handle success
                    postResponse.value = Event(baseModel.message?.data)

                } ?: run {
                    isLoading.value = Event(false)
                    // Handle error
                    Log.d("Success VieModel->", "false")


                }
            }
    }
}