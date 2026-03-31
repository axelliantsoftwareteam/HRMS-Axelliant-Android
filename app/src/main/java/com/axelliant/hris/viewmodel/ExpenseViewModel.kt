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
import com.axelliant.hris.model.expense.GetExpenseResponse
import com.axelliant.hris.model.expense.MyExpenseDetailResponse
import com.axelliant.hris.model.expense.MyExpensePostResponse
import com.axelliant.hris.model.leave.ExpenseApprovalStatus
import com.axelliant.hris.model.leave.PostExpenseImageResponse
import com.axelliant.hris.model.leave.PostResponse
import com.axelliant.hris.repos.ExpenseRepo
import com.axelliant.hris.utils.Validator

class ExpenseViewModel(private val expenseRepo: ExpenseRepo,private val validator: Validator) : BaseViewModel() {

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

    val expenseTypeResponse: MutableLiveData<Event<GetExpenseResponse?>> by lazy { MutableLiveData<Event<GetExpenseResponse?>>() }
    val expenseApproval: MutableLiveData<Event<expenseApprovalList?>> by lazy { MutableLiveData<Event<expenseApprovalList?>>() }

    val expenseApprovalResponse: MutableLiveData<Event<PostResponse?>> by lazy { MutableLiveData<Event<PostResponse?>>() }


    fun getDocumentReqDetail(attendanceInput: AttendanceInput) {
        isLoading.value = Event(true)
        expenseRepo.getDocumentDetail(attendanceInput)
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
            expenseRepo.createDocument(CreateDocument().apply {
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

    fun bulkExpenseApprovalStatus(expenseIds: List<String>, status: String) {
        isLoading.value = Event(true)
        expenseRepo.bulkExpenseApprovalStatus(expenseIds, status)
            .observeForever { data ->
                data?.let { baseModel ->
                    isLoading.value = Event(false)
                    expenseApprovalResponse.value = Event(baseModel.message?.data)
                } ?: run {
                    isLoading.value = Event(false)
                    Log.d("Success VieModel->", "false")
                }
            }
    }
}
