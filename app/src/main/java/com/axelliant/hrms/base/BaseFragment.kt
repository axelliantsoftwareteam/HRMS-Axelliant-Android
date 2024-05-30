package com.axelliant.hrms.base

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.axelliant.hrms.enums.AttendanceFilter
import com.axelliant.hrms.event.EventObserver
import com.axelliant.hrms.navigation.AppNavigator
import com.axelliant.hrms.screens.BaseActivity
import com.axelliant.hrms.utils.Utils
import com.axelliant.hrms.viewmodel.BaseViewModel
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