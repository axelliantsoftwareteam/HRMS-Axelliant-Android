package com.axelliant.android_erp.observable

import androidx.lifecycle.LiveData

class ObservableCode  : LiveData<Int>() {

    fun set(newValue: Int) {
        value = newValue
    }
}