package com.axelliant.hris.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.axelliant.hris.event.Event


open class BaseViewModel  : ViewModel()  {

    val isLoading : MutableLiveData<Event<Boolean>> by lazy { MutableLiveData<Event<Boolean>>() }
    fun getIsLoading(): LiveData<Event<Boolean>> = isLoading


}