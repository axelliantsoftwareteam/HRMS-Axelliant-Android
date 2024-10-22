package com.axelliant.hris.observable

import androidx.lifecycle.LiveData

class ObservableCode  : LiveData<Int>() {

    fun set(newValue: Int) {
        value = newValue
    }
}