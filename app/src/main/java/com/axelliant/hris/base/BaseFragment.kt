package com.axelliant.hris.base

import android.os.Build
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

    fun appVersion(): String {
        try {
            val ctx = requireContext()
            val packageInfo = ctx.packageManager.getPackageInfo(ctx.packageName, 0)

            val versionName = packageInfo.versionName ?: "N/A"
            val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toLong()
            }
            return "v $versionName ($versionCode)"

        } catch (e: Exception) {
            e.printStackTrace()
            return ""
        }


    }

}
