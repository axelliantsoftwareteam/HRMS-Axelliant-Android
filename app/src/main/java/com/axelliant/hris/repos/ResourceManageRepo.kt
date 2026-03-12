package com.axelliant.hris.repos

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.axelliant.hris.config.AppConst
import com.axelliant.hris.model.ImagePath
import com.axelliant.hris.model.attendance.AttRequest
import com.axelliant.hris.model.attendance.AttendanceInput
import com.axelliant.hris.model.attendance.expenseApprovalList
import com.axelliant.hris.model.base.BaseApiModel
import com.axelliant.hris.model.base.BaseModel
import com.axelliant.hris.model.base.Meta
import com.axelliant.hris.model.documentRequest.CreateDocument
import com.axelliant.hris.model.documentRequest.MyDocumentResponse
import com.axelliant.hris.model.documentRequest.SubmitDocument
import com.axelliant.hris.model.leave.ExpenseApprovalStatus
import com.axelliant.hris.model.leave.PostExpenseImageResponse
import com.axelliant.hris.model.leave.PostResponse
import com.axelliant.hris.model.resourceManage.CreateResourceHour
import com.axelliant.hris.model.resourceManage.DeleteProject
import com.axelliant.hris.model.resourceManage.GetListProject
import com.axelliant.hris.model.resourceManage.MyHoursDetails
import com.axelliant.hris.model.resourceManage.PostHoursRequestResponse
import com.axelliant.hris.network.ApiInterface
import com.axelliant.hris.network.BaseCallBack
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Response
import java.lang.reflect.Type

class ResourceManageRepo(private var apiInterface: ApiInterface) {

    fun getMyHoursDetail(attendanceInput: AttendanceInput): MutableLiveData<BaseApiModel<MyHoursDetails>> {
        val serverResponse = MutableLiveData<BaseApiModel<MyHoursDetails>>()

        val call: Call<ResponseBody> = apiInterface.callMyResourceHoursDetail("token ${AppConst.TOKEN}",
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

                val type: Type = object : TypeToken<BaseApiModel<MyHoursDetails>>() {}.type
                val jsonString = response.body()?.string()
                val userModel =
                    Gson().fromJson<BaseApiModel<MyHoursDetails>>(jsonString, type)
                serverResponse.value = userModel
            }


            override fun onFinalFailure(
                errorString: String?
            ) {

                Log.e("API Failure", " $errorString")

                serverResponse.value =
                    BaseApiModel(
                        BaseModel(
                            MyHoursDetails(
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

        Log.e("HTTP Request", " " + call.request().toString())

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

    fun createDocument(createDocument: CreateDocument): MutableLiveData<BaseApiModel<PostResponse>> {
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

    fun createResourceHour(isUpdate: Boolean, createResourceHour: CreateResourceHour): MutableLiveData<BaseApiModel<PostHoursRequestResponse>> {
        val serverResponse = MutableLiveData<BaseApiModel<PostHoursRequestResponse>>()

        val call: Call<ResponseBody> = if (isUpdate) {
            apiInterface.callUpdateResourceType(
                "token ${AppConst.TOKEN}", createResourceHour
            )
        } else {
            apiInterface.callCreateResourceHour(
                "token ${AppConst.TOKEN}", createResourceHour
            )
        }


        Log.e("HTTP Request", " " + call.request().toString())

        call.enqueue(object : BaseCallBack<ResponseBody>(call) {
            override fun onFinalSuccess(
                call: Call<ResponseBody>,
                response: Response<ResponseBody>
            ) {

                Log.e("API success", " " + response.body())

                val type: Type = object : TypeToken<BaseApiModel<PostHoursRequestResponse>>() {}.type
                val jsonString = response.body()?.string()
                val userModel =
                    Gson().fromJson<BaseApiModel<PostHoursRequestResponse>>(jsonString, type)
                serverResponse.value = userModel
            }


            override fun onFinalFailure(
                errorString: String?
            ) {

                Log.e("API Failure", " $errorString")

                serverResponse.value =
                    BaseApiModel(
                        BaseModel(
                            PostHoursRequestResponse(
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
    fun submitResourceHour(submitDocument: SubmitDocument): MutableLiveData<BaseApiModel<PostResponse>> {
        val serverResponse = MutableLiveData<BaseApiModel<PostResponse>>()

        val call: Call<ResponseBody> = apiInterface.callSubmitResource(
                "token ${AppConst.TOKEN}", submitDocument)
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

    fun deleteProject(
        deleteProject: DeleteProject
    ): MutableLiveData<BaseApiModel<PostHoursRequestResponse>> {
        val serverResponse = MutableLiveData<BaseApiModel<PostHoursRequestResponse>>()
        val call: Call<ResponseBody> = apiInterface.deleteHoursCall(
            "token ${AppConst.TOKEN}", deleteProject)

        Log.e("HTTP Request", " " + call.request().toString())

        call.enqueue(object : BaseCallBack<ResponseBody>(call) {
            override fun onFinalSuccess(
                call: Call<ResponseBody>,
                response: Response<ResponseBody>)
            {
                Log.e("API success", " " + response.body())
                val type: Type = object : TypeToken<BaseApiModel<PostHoursRequestResponse>>() {}.type
                val jsonString = response.body()?.string()
                val userModel =
                    Gson().fromJson<BaseApiModel<PostHoursRequestResponse>>(jsonString, type)
                serverResponse.value = userModel
            }
            override fun onFinalFailure(
                errorString: String?
            ) {

                Log.e("API Failure", " $errorString")

                serverResponse.value =
                    BaseApiModel(
                        BaseModel(
                            PostHoursRequestResponse(
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

    fun getProjectsTypes(): MutableLiveData<BaseApiModel<GetListProject>> {
        val serverResponse = MutableLiveData<BaseApiModel<GetListProject>>()

        val call: Call<ResponseBody> = apiInterface.getProjectTypeList("token ${AppConst.TOKEN}")

        Log.e("HTTP Request", " " + call.request().toString())

        call.enqueue(object : BaseCallBack<ResponseBody>(call) {
            override fun onFinalSuccess(
                call: Call<ResponseBody>,
                response: Response<ResponseBody>
            ) {

                Log.e("API success", " " + response.body())

                val type: Type = object : TypeToken<BaseApiModel<GetListProject>>() {}.type
                val jsonString = response.body()?.string()
                val userModel = Gson().fromJson<BaseApiModel<GetListProject>>(jsonString, type)
                serverResponse.value = userModel
            }


            override fun onFinalFailure(
                errorString: String?
            ) {

                Log.e("API Failure", " $errorString")

                serverResponse.value =
                    BaseApiModel(
                        BaseModel(
                            GetListProject(
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

    fun resourceListApproval(
        inputObject: AttendanceInput
    ): MutableLiveData<BaseApiModel<MyHoursDetails>> {
        val serverResponse = MutableLiveData<BaseApiModel<MyHoursDetails>>()

        val call = apiInterface.callResourcesApproval(
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

                val type: Type = object : TypeToken<BaseApiModel<MyHoursDetails>>() {}.type
                val jsonString = response.body()?.string()
                val userModel =
                    Gson().fromJson<BaseApiModel<MyHoursDetails>>(jsonString, type)
                serverResponse.value = userModel
            }


            override fun onFinalFailure(
                errorString: String?
            ) {

                Log.e("API Failure", " $errorString")

                serverResponse.value =
                    BaseApiModel(
                        BaseModel(
                            MyHoursDetails(
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

        val call = apiInterface.callExpenseApprovalStatus(
            "token ${AppConst.TOKEN}", inputObject
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


}