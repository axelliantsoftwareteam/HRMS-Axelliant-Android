package com.axelliant.hris.repos

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.axelliant.hris.config.AppConst
import com.axelliant.hris.model.ImagePath
import com.axelliant.hris.model.approval.ApprovalActionRequest
import com.axelliant.hris.model.approval.ApprovalActionItem
import com.axelliant.hris.model.approval.BulkApprovalActionRequest
import com.axelliant.hris.model.attendance.AttRequest
import com.axelliant.hris.model.attendance.AttendanceInput
import com.axelliant.hris.model.attendance.expenseApprovalList
import com.axelliant.hris.model.base.BaseApiModel
import com.axelliant.hris.model.base.BaseModel
import com.axelliant.hris.model.base.Meta
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
import com.axelliant.hris.network.ApiInterface
import com.axelliant.hris.network.BaseCallBack
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Response
import java.lang.reflect.Type

class ExpenseRepo(private var apiInterface: ApiInterface) {

    fun getMyExpenseDetail(attendanceInput: AttendanceInput): MutableLiveData<BaseApiModel<MyExpenseDetailResponse>> {
        val serverResponse = MutableLiveData<BaseApiModel<MyExpenseDetailResponse>>()

        val call: Call<ResponseBody> = apiInterface.callMyExpenseDetail("token ${AppConst.TOKEN}",
            AttRequest().apply {
                this.start_date = attendanceInput.startDate
                this.end_date = attendanceInput.endDate
                this.filters = attendanceInput.filters
            }

        )

        Log.e("HTTP Request", " " + call?.request().toString())

        call.enqueue(object : BaseCallBack<ResponseBody>(call) {
            override fun onFinalSuccess(
                call: Call<ResponseBody>,
                response: Response<ResponseBody>
            ) {

                Log.e("API success", " " + response.body())

                val type: Type = object : TypeToken<BaseApiModel<MyExpenseDetailResponse>>() {}.type
                val jsonString = response.body()?.string()
                val userModel =
                    Gson().fromJson<BaseApiModel<MyExpenseDetailResponse>>(jsonString, type)
                serverResponse.value = userModel
            }


            override fun onFinalFailure(
                errorString: String?
            ) {

                Log.e("API Failure", " $errorString")

                serverResponse.value =
                    BaseApiModel(
                        BaseModel(
                            MyExpenseDetailResponse(
                                meta = Meta(
                                    errorString.toString(),
                                    false
                                )
                            )
                        )
                    )

            }

        })

        return serverResponse
    }

    fun getDocumentDetail(attendanceInput: AttendanceInput): MutableLiveData<BaseApiModel<MyDocumentResponse>> {
        val serverResponse = MutableLiveData<BaseApiModel<MyDocumentResponse>>()

        val call: Call<ResponseBody> = apiInterface.callDocumentRequestDetail("token ${AppConst.TOKEN}",
            AttRequest().apply {
                this.start_date = attendanceInput.startDate
                this.end_date = attendanceInput.endDate
                this.filters = attendanceInput.filters
            }

        )

        Log.e("HTTP Request", " " + call?.request().toString())

        call.enqueue(object : BaseCallBack<ResponseBody>(call) {
            override fun onFinalSuccess(
                call: Call<ResponseBody>,
                response: Response<ResponseBody>
            ) {

                Log.e("API success", " " + response.body())

                val type: Type = object : TypeToken<BaseApiModel<MyDocumentResponse>>() {}.type
                val jsonString = response.body()?.string()
                val userModel =
                    Gson().fromJson<BaseApiModel<MyDocumentResponse>>(jsonString, type)
                serverResponse.value = userModel
            }


            override fun onFinalFailure(
                errorString: String?
            ) {

                Log.e("API Failure", " $errorString")

                serverResponse.value =
                    BaseApiModel(
                        BaseModel(
                            MyDocumentResponse(
                                meta = Meta(
                                    errorString.toString(),
                                    false
                                )
                            )
                        )
                    )

            }

        })

        return serverResponse
    }

    fun createMyExpenseImage(imagePath: ImagePath): MutableLiveData<BaseApiModel<PostExpenseImageResponse>> {
        val serverResponse = MutableLiveData<BaseApiModel<PostExpenseImageResponse>>()
        val docName: MultipartBody.Part = MultipartBody.Part.createFormData("docname", imagePath.docname!!)
        val isPrivate: MultipartBody.Part = MultipartBody.Part.createFormData("is_private", imagePath.is_private!!.toString())
        val folder: MultipartBody.Part = MultipartBody.Part.createFormData("docname", imagePath.folder!!)
        val doctype: MultipartBody.Part = MultipartBody.Part.createFormData("doctype", imagePath.doctype!!)
        val call: Call<ResponseBody> = apiInterface.callMyExpensefile("token ${AppConst.TOKEN}",
            imagePath.file!!,docName,isPrivate,folder,doctype)

        Log.e("HTTP Request", " " + call?.request().toString())

        call.enqueue(object : BaseCallBack<ResponseBody>(call) {
            override fun onFinalSuccess(
                call: Call<ResponseBody>,
                response: Response<ResponseBody>
            ) {

                Log.e("API success", " " + response.body())

                val type: Type = object : TypeToken<BaseApiModel<PostExpenseImageResponse>>() {}.type
                val jsonString = response.body()?.string()
                val userModel =
                    Gson().fromJson<BaseApiModel<PostExpenseImageResponse>>(jsonString, type)
                serverResponse.value = userModel
            }


            override fun onFinalFailure(
                errorString: String?
            ) {

                Log.e("API Failure", " $errorString")

                serverResponse.value =
                    BaseApiModel(
                        BaseModel(
                            PostExpenseImageResponse(
                                meta = Meta(
                                    errorString.toString(),
                                    false
                                )
                            )
                        )
                    )

            }

        })

        return serverResponse
    }

    fun createDocument(
        createDocument: CreateDocument
    ): MutableLiveData<BaseApiModel<PostResponse>> {
        val serverResponse = MutableLiveData<BaseApiModel<PostResponse>>()

        val call: Call<ResponseBody>?
        call = apiInterface.callCreateDocument("token ${AppConst.TOKEN}", createDocument)
        Log.e("HTTP Request", " " + call.request().toString())

        call.enqueue(object : BaseCallBack<ResponseBody>(call) {
            override fun onFinalSuccess(
                call: Call<ResponseBody>,
                response: Response<ResponseBody>
            ) {

                Log.e("API success", " " + response.body())

                val type: Type = object : TypeToken<BaseApiModel<PostResponse>>() {}.type
                val jsonString = response.body()?.string()
                val userModel =
                    Gson().fromJson<BaseApiModel<PostResponse>>(jsonString, type)
                serverResponse.value = userModel
            }


            override fun onFinalFailure(
                errorString: String?
            ) {

                Log.e("API Failure", " $errorString")

                serverResponse.value =
                    BaseApiModel(
                        BaseModel(
                            PostResponse(
                                meta = Meta(
                                    errorString.toString(),
                                    false
                                )
                            )
                        )
                    )

            }

        })

        return serverResponse
    }

    fun createExpense(
        isUpdate: Boolean,
        createExpense: CreateExpense
    ): MutableLiveData<BaseApiModel<MyExpensePostResponse>> {
        val serverResponse = MutableLiveData<BaseApiModel<MyExpensePostResponse>>()

        val call: Call<ResponseBody>?

        if (isUpdate) {
            call = apiInterface.callUpdateExp(
                "token ${AppConst.TOKEN}", createExpense
            )
        } else {
            call = apiInterface.callCreateExp(
                "token ${AppConst.TOKEN}", createExpense
            )
        }


        Log.e("HTTP Request", " " + call?.request().toString())

        call.enqueue(object : BaseCallBack<ResponseBody>(call) {
            override fun onFinalSuccess(
                call: Call<ResponseBody>,
                response: Response<ResponseBody>
            ) {

                Log.e("API success", " " + response.body())

                val type: Type = object : TypeToken<BaseApiModel<MyExpensePostResponse>>() {}.type
                val jsonString = response.body()?.string()
                val userModel =
                    Gson().fromJson<BaseApiModel<MyExpensePostResponse>>(jsonString, type)
                serverResponse.value = userModel
            }


            override fun onFinalFailure(
                errorString: String?
            ) {

                Log.e("API Failure", " $errorString")

                serverResponse.value =
                    BaseApiModel(
                        BaseModel(
                            MyExpensePostResponse(
                                meta = Meta(
                                    errorString.toString(),
                                    false
                                )
                            )
                        )
                    )

            }

        })

        return serverResponse
    }

    fun deleteExpense(
        createExpense: CreateExpense
    ): MutableLiveData<BaseApiModel<MyExpensePostResponse>> {
        val serverResponse = MutableLiveData<BaseApiModel<MyExpensePostResponse>>()

        val call: Call<ResponseBody> = apiInterface.deleteExpenseCall(
            "token ${AppConst.TOKEN}", createExpense
        )


        Log.e("HTTP Request", " " + call?.request().toString())

        call.enqueue(object : BaseCallBack<ResponseBody>(call) {
            override fun onFinalSuccess(
                call: Call<ResponseBody>,
                response: Response<ResponseBody>
            ) {

                Log.e("API success", " " + response.body())

                val type: Type = object : TypeToken<BaseApiModel<MyExpensePostResponse>>() {}.type
                val jsonString = response.body()?.string()
                val userModel =
                    Gson().fromJson<BaseApiModel<MyExpensePostResponse>>(jsonString, type)
                serverResponse.value = userModel
            }


            override fun onFinalFailure(
                errorString: String?
            ) {

                Log.e("API Failure", " $errorString")

                serverResponse.value =
                    BaseApiModel(
                        BaseModel(
                            MyExpensePostResponse(
                                meta = Meta(
                                    errorString.toString(),
                                    false
                                )
                            )
                        )
                    )

            }

        })

        return serverResponse
    }


    fun deleteAttachment(
        deleteAttachment: DeleteAttachment
    ): MutableLiveData<BaseApiModel<MyExpensePostResponse>> {
        val serverResponse = MutableLiveData<BaseApiModel<MyExpensePostResponse>>()

        val call: Call<ResponseBody> = apiInterface.deleteExpenseAttachmentCall(
            "token ${AppConst.TOKEN}", deleteAttachment
        )


        Log.e("HTTP Request", " " + call?.request().toString())

        call.enqueue(object : BaseCallBack<ResponseBody>(call) {
            override fun onFinalSuccess(
                call: Call<ResponseBody>,
                response: Response<ResponseBody>
            ) {

                Log.e("API success", " " + response.body())

                val type: Type = object : TypeToken<BaseApiModel<MyExpensePostResponse>>() {}.type
                val jsonString = response.body()?.string()
                val userModel =
                    Gson().fromJson<BaseApiModel<MyExpensePostResponse>>(jsonString, type)
                serverResponse.value = userModel
            }


            override fun onFinalFailure(
                errorString: String?
            ) {

                Log.e("API Failure", " $errorString")

                serverResponse.value =
                    BaseApiModel(
                        BaseModel(
                            MyExpensePostResponse(
                                meta = Meta(
                                    errorString.toString(),
                                    false
                                )
                            )
                        )
                    )

            }

        })

        return serverResponse
    }


    fun getExpenseTypes(): MutableLiveData<BaseApiModel<GetExpenseResponse>> {
        val serverResponse = MutableLiveData<BaseApiModel<GetExpenseResponse>>()

        val call: Call<ResponseBody> = apiInterface.getExpenseType("token ${AppConst.TOKEN}")

        Log.e("HTTP Request", " " + call?.request().toString())

        call.enqueue(object : BaseCallBack<ResponseBody>(call) {
            override fun onFinalSuccess(
                call: Call<ResponseBody>,
                response: Response<ResponseBody>
            ) {

                Log.e("API success", " " + response.body())

                val type: Type = object : TypeToken<BaseApiModel<GetExpenseResponse>>() {}.type
                val jsonString = response.body()?.string()
                val userModel = Gson().fromJson<BaseApiModel<GetExpenseResponse>>(jsonString, type)
                serverResponse.value = userModel
            }


            override fun onFinalFailure(
                errorString: String?
            ) {

                Log.e("API Failure", " $errorString")

                serverResponse.value =
                    BaseApiModel(
                        BaseModel(
                            GetExpenseResponse(
                                meta = Meta(
                                    errorString.toString(),
                                    false
                                )
                            )
                        )
                    )

            }

        })

        return serverResponse
    }

    fun expenseListApproval(
        inputObject: AttendanceInput
    ): MutableLiveData<BaseApiModel<expenseApprovalList>> {
        val serverResponse = MutableLiveData<BaseApiModel<expenseApprovalList>>()

        val call = apiInterface.callExpenseApproval(
            "token ${AppConst.TOKEN}", AttRequest().apply {
                this.start_date = inputObject.startDate
                this.end_date = inputObject.endDate
                this.employee_list = inputObject.employeeId
            }
        )



        Log.e("HTTP Request", " " + call.request().toString())

        call.enqueue(object : BaseCallBack<ResponseBody>(call) {
            override fun onFinalSuccess(
                call: Call<ResponseBody>,
                response: Response<ResponseBody>
            ) {

                Log.e("API success", " " + response.body())

                val type: Type = object : TypeToken<BaseApiModel<expenseApprovalList>>() {}.type
                val jsonString = response.body()?.string()
                val userModel =
                    Gson().fromJson<BaseApiModel<expenseApprovalList>>(jsonString, type)
                serverResponse.value = userModel
            }


            override fun onFinalFailure(
                errorString: String?
            ) {

                Log.e("API Failure", " $errorString")

                serverResponse.value =
                    BaseApiModel(
                        BaseModel(
                            expenseApprovalList(
                                meta = Meta(
                                    errorString.toString(),
                                    false
                                )
                            )
                        )
                    )

            }

        })

        return serverResponse
    }


    fun expenseApprovalStatus(
        inputObject: ExpenseApprovalStatus
    ): MutableLiveData<BaseApiModel<PostResponse>> {
        val serverResponse = MutableLiveData<BaseApiModel<PostResponse>>()

        val call = apiInterface.takeApprovalAction(
            "token ${AppConst.TOKEN}",
            ApprovalActionRequest(
                approval_type = "expense",
                reference_name = inputObject.expense_id,
                status = inputObject.status
            )
        )

        Log.e("HTTP Request", " " + call.request().toString())

        call.enqueue(object : BaseCallBack<ResponseBody>(call) {
            override fun onFinalSuccess(
                call: Call<ResponseBody>,
                response: Response<ResponseBody>
            ) {

                Log.e("API success", " " + response.body())

                val type: Type = object : TypeToken<BaseApiModel<PostResponse>>() {}.type
                val jsonString = response.body()?.string()
                val userModel =
                    Gson().fromJson<BaseApiModel<PostResponse>>(jsonString, type)
                serverResponse.value = userModel
            }


            override fun onFinalFailure(
                errorString: String?
            ) {

                Log.e("API Failure", " $errorString")

                serverResponse.value =
                    BaseApiModel(
                        BaseModel(
                            PostResponse(
                                meta = Meta(
                                    errorString.toString(),
                                    false
                                )
                            )
                        )
                    )

            }

        })

        return serverResponse
    }

    fun bulkExpenseApprovalStatus(
        expenseIds: List<String>,
        status: String
    ): MutableLiveData<BaseApiModel<PostResponse>> {
        val serverResponse = MutableLiveData<BaseApiModel<PostResponse>>()

        val call = apiInterface.bulkTakeApprovalAction(
            "token ${AppConst.TOKEN}",
            BulkApprovalActionRequest(
                actions = expenseIds.filter { it.isNotBlank() }.map {
                    ApprovalActionItem(
                        approval_type = "expense",
                        reference_name = it
                    )
                },
                status = status
            )
        )

        Log.e("HTTP Request", " " + call.request().toString())

        call.enqueue(object : BaseCallBack<ResponseBody>(call) {
            override fun onFinalSuccess(
                call: Call<ResponseBody>,
                response: Response<ResponseBody>
            ) {
                val type: Type = object : TypeToken<BaseApiModel<PostResponse>>() {}.type
                val jsonString = response.body()?.string()
                val userModel = Gson().fromJson<BaseApiModel<PostResponse>>(jsonString, type)
                serverResponse.value = userModel
            }

            override fun onFinalFailure(errorString: String?) {
                serverResponse.value =
                    BaseApiModel(
                        BaseModel(
                            PostResponse(
                                meta = Meta(
                                    errorString.toString(),
                                    false
                                )
                            )
                        )
                    )
            }
        })

        return serverResponse
    }


}
