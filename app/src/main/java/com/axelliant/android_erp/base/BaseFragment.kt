package com.axelliant.android_erp.base

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.axelliant.android_erp.enums.AttendanceFilter
import com.axelliant.android_erp.event.EventObserver
import com.axelliant.android_erp.navigation.AppNavigator
import com.axelliant.android_erp.screens.BaseActivity
import com.axelliant.android_erp.utils.Utils
import com.axelliant.android_erp.viewmodel.BaseViewModel
import com.google.android.material.datepicker.MaterialDatePicker
import org.koin.android.ext.android.inject
import java.util.Date

open class BaseFragment : Fragment() {



    fun previousFragmentNavigation(){
        showDialog()
        AppNavigator.moveBackToPreviousFragment()
    }

     fun showDialog() {
        if (requireActivity() is BaseActivity) {
            if (!(requireActivity() as BaseActivity).isFinishing)
                (requireActivity() as BaseActivity).loadingDialog.show()
        }
    }

     fun hideDialog() {
        if (requireActivity() is BaseActivity)
            (requireActivity() as BaseActivity).loadingDialog.dismiss()
    }


}