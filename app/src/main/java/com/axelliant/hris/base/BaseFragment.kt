package com.axelliant.hris.base

import androidx.fragment.app.Fragment
import com.axelliant.hris.navigation.AppNavigator
import com.axelliant.hris.screens.BaseActivity
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

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

    fun navigateToBottomSheet(bottomSheetFragment: BottomSheetDialogFragment) {
        bottomSheetFragment.show(parentFragmentManager, bottomSheetFragment.tag)

    }


}