package com.axelliant.android_erp.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.axelliant.android_erp.event.Event


open class BaseViewModel  : ViewModel()  {

    val isLoading : MutableLiveData<Event<Boolean>> by lazy { MutableLiveData<Event<Boolean>>() }
    fun getIsLoading(): LiveData<Event<Boolean>> = isLoading


}