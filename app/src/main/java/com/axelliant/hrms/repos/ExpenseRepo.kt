package com.axelliant.hrms.repos

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.axelliant.hrms.config.AppConst
import com.axelliant.hrms.model.attendance.AttRequest
import com.axelliant.hrms.model.attendance.AttendanceInput
import com.axelliant.hrms.model.attendance.expenseApprovalList
import com.axelliant.hrms.model.base.BaseApiModel
import com.axelliant.hrms.model.base.BaseModel
import com.axelliant.hrms.model.base.Meta
import com.axelliant.hrms.model.expense.CreateExpense
import com.axelliant.hrms.model.expense.GetExpenseResponse
import com.axelliant.hrms.model.expense.MyExpenseDetailResponse
import com.axelliant.hrms.model.leave.ExpenseApprovalStatus
import com.axelliant.hrms.model.leave.LeaveApproval
import com.axelliant.hrms.model.leave.PostResponse
import com.axelliant.hrms.network.ApiInterface
import com.axelliant.hrms.network.BaseCallBack
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
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

    fun createExpense(createExpense: CreateExpense): MutableLiveData<BaseApiModel<PostResponse>> {
        val serverResponse = MutableLiveData<BaseApiModel<PostResponse>>()

        val call: Call<ResponseBody> = apiInterface.callCreateExp(
            "token ${AppConst.TOKEN}",createExpense
        )

        Log.e("HTTP Request", " " + call?.request().toString())

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


    fun getExpenseTypes(): MutableLiveData<BaseApiModel<GetExpenseResponse>> {
        val serverResponse = MutableLiveData<BaseApiModel<GetExpenseResponse>>()

        val call: Call<ResponseBody>  = apiInterface.getExpenseType("token ${AppConst.TOKEN}")

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
                    BaseApiModel(BaseModel(GetExpenseResponse(meta = Meta(errorString.toString(), false))))

            }

        })

        return serverResponse
    }

    fun expenseListApproval(
        inputObject: AttendanceInput
    ): MutableLiveData<BaseApiModel<expenseApprovalList>> {
        val serverResponse = MutableLiveData<BaseApiModel<expenseApprovalList>>()

        val call = apiInterface.callExpenseApproval(
            "token ${AppConst.TOKEN}",AttRequest().apply {
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

        val call = apiInterface.callExpenseApprovalStatus(
            "token ${AppConst.TOKEN}",inputObject
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