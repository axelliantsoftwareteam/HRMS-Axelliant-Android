package com.axelliant.hris.base

import android.os.Build
import android.os.Bundle
import android.view.View
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.fragment.app.Fragment
import com.axelliant.hris.dialog.LoadingDialog
import com.axelliant.hris.navigation.AppNavigator
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

open class BaseFragment : Fragment() {
    private var screenDialog: LoadingDialog? = null
    private var dialogRequested = false

    fun previousFragmentNavigation(){
        AppNavigator.moveBackToPreviousFragment()
    }

     fun showDialog() {
        dialogRequested = true
        showOwnedDialog()
    }

     fun hideDialog() {
        dialogRequested = false
        screenDialog?.dismiss()
    }

    private fun showOwnedDialog() {
        val host = activity ?: return
        if (!dialogRequested || host.isFinishing || host.isDestroyed || view == null ||
            !viewLifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) return
        val dialog = screenDialog ?: LoadingDialog(host).also {
            it.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            it.setCanceledOnTouchOutside(false)
            screenDialog = it
        }
        if (!dialog.isShowing) dialog.show()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) { showOwnedDialog() }
            override fun onStop(owner: LifecycleOwner) { screenDialog?.dismiss() }
        })
    }

    override fun onDestroyView() {
        hideDialog()
        screenDialog = null
        super.onDestroyView()
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
