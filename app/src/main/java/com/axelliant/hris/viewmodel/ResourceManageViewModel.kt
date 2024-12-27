package com.axelliant.hris.viewmodel

import android.util.Log
import androidx.databinding.ObservableField
import androidx.lifecycle.MutableLiveData
import com.axelliant.hris.event.Event
import com.axelliant.hris.model.ImagePath
import com.axelliant.hris.model.attendance.AttendanceInput
import com.axelliant.hris.model.attendance.expenseApprovalList
import com.axelliant.hris.model.documentRequest.CreateDocument
import com.axelliant.hris.model.documentRequest.MyDocumentResponse
import com.axelliant.hris.model.expense.CreateExpense
import com.axelliant.hris.model.expense.DeleteAttachment
import com.axelliant.hris.model.expense.MyExpenseDetailResponse
import com.axelliant.hris.model.expense.MyExpensePostResponse
import com.axelliant.hris.model.leave.ExpenseApprovalStatus
import com.axelliant.hris.model.leave.PostExpenseImageResponse
import com.axelliant.hris.model.leave.PostResponse
import com.axelliant.hris.model.resourceManage.CreateResourceHour
import com.axelliant.hris.model.resourceManage.GetListProject
import com.axelliant.hris.repos.ResourceManageRepo
import com.axelliant.hris.utils.Validator

class ResourceManageViewModel(private val resourceManageRepo: ResourceManageRepo, private val validator: Validator) : BaseViewModel() {

    var subject = ObservableField<String>()
    var description = ObservableField<String>()

    val _subjectErrorLiveData = MutableLiveData<String?>()
    val subjectError: MutableLiveData<String?> get() = _subjectErrorLiveData

    private val _descriptionErrorLiveData = MutableLiveData<String?>()
    val descriptionError: MutableLiveData<String?> get() = _descriptionErrorLiveData

    val expenseResponse: MutableLiveData<Event<MyExpenseDetailResponse?>> by lazy { MutableLiveData<Event<MyExpenseDetailResponse?>>() }
    val documentResponse: MutableLiveData<Event<MyDocumentResponse?>> by lazy { MutableLiveData<Event<MyDocumentResponse?>>() }

    val postExpenseResponse: MutableLiveData<Event<PostExpenseImageResponse?>> by lazy { MutableLiveData<Event<PostExpenseImageResponse?>>() }
    val myPostExpenseResponse: MutableLiveData<Event<MyExpensePostResponse?>> by lazy { MutableLiveData<Event<MyExpensePostResponse?>>() }
    val deleteExpenseResponse: MutableLiveData<Event<MyExpensePostResponse?>> by lazy { MutableLiveData<Event<MyExpensePostResponse?>>() }
    val deleteAttachmentResponse: MutableLiveData<Event<MyExpensePostResponse?>> by lazy { MutableLiveData<Event<MyExpensePostResponse?>>() }

    val projectTypeResponse: MutableLiveData<Event<GetListProject?>> by lazy { MutableLiveData<Event<GetListProject?>>() }
    val expenseApproval: MutableLiveData<Event<expenseApprovalList?>> by lazy { MutableLiveData<Event<expenseApprovalList?>>() }

    val expenseApprovalResponse: MutableLiveData<Event<PostResponse?>> by lazy { MutableLiveData<Event<PostResponse?>>() }


    fun getDocumentReqDetail(attendanceInput: AttendanceInput) {
        isLoading.value = Event(true)
        resourceManageRepo.getDocumentDetail(attendanceInput)
            .observeForever { data ->
                // Handle the login response
                data?.let { baseModel ->
                    isLoading.value = Event(false)
                    // Handle success
                    documentResponse.value = Event(baseModel.message?.data)

                } ?: run {
                    isLoading.value = Event(false)
                    // Handle error
                    Log.d("Success VieModel->", "false")


                }
            }
    }

    fun postDocument() {

        val enteredSubject = subject.get()
        val enteredDescription = description.get()

        // Perform unified validation
        val subjectResult = validator.validateEmailAndPhoneField(enteredSubject)
        val descriptionResult = validator.validateEmailAndPhoneField(enteredDescription)

        subjectError.value = null
        descriptionError.value = null

        if (subjectResult.isValid && descriptionResult.isValid)
        {
            isLoading.value = Event(true)
            resourceManageRepo.createDocument(CreateDocument().apply {
                this.subject = enteredSubject
                this.detail = enteredDescription
            })
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
        else{
            if (!subjectResult.isValid)
                subjectError.value = subjectResult.errorMessage
            else
                subjectError.value = null

            if (!descriptionResult.isValid)
                descriptionError.value = descriptionResult.errorMessage
            else
                descriptionError.value = null
        }
    }

//    fun validateDocument(subject: String?, description: String?): Boolean {
//        var isValid = true
//
//        if (subject.isNullOrEmpty()) {
//            subjectError.value = "Subject cannot be empty"
//            isValid = false
//        } else {
//            subjectError.value = null
//        }
//
//        if (description.isNullOrEmpty()) {
//            descriptionError.value = "Description cannot be empty"
//            isValid = false
//        } else {
//            descriptionError.value = null
//        }
//
//        return isValid
//    }

    fun getMyExpenseDetail(attendanceInput: AttendanceInput) {
        isLoading.value = Event(true)
        resourceManageRepo.getMyExpenseDetail(attendanceInput)
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
        resourceManageRepo.createMyExpenseImage(imagePath)
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


    fun deleteExpense(createExpense: CreateExpense) {
        isLoading.value = Event(true)
        resourceManageRepo.deleteExpense(createExpense)
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

    fun getExpenseApproval(inputObject: AttendanceInput) {
        isLoading.value = Event(true)
        resourceManageRepo.expenseListApproval(inputObject)
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
        resourceManageRepo.expenseApprovalStatus(inputObject)
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