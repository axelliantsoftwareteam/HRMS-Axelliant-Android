package com.axelliant.hris.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.axelliant.hris.event.Event
import com.axelliant.hris.network.ApiResult
import com.axelliant.hris.network.enqueueResult
import okhttp3.ResponseBody
import retrofit2.Call
import java.lang.reflect.Type


open class BaseViewModel  : ViewModel()  {

    val isLoading : MutableLiveData<Event<Boolean>> by lazy { MutableLiveData<Event<Boolean>>() }
    fun getIsLoading(): LiveData<Event<Boolean>> = isLoading

    /**
     * Runs [call], parses it as [type] and posts the [ApiResult] to [target]. Used by the redesigned
     * screens instead of one repo method per endpoint. [showLoading] toggles the full-screen loader.
     */
    protected fun <T> request(
        call: Call<ResponseBody>,
        type: Type,
        target: MutableLiveData<Event<ApiResult<T>>>,
        showLoading: Boolean = true
    ) {
        if (showLoading) isLoading.value = Event(true)
        call.enqueueResult<T>(type) { result ->
            if (showLoading) isLoading.value = Event(false)
            target.value = Event(result)
        }
    }

}
