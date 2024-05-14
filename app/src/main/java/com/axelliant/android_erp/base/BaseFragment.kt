package com.axelliant.android_erp.base

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.axelliant.android_erp.event.EventObserver
import com.axelliant.android_erp.screens.BaseActivity
import com.axelliant.android_erp.viewmodel.BaseViewModel
import org.koin.android.ext.android.inject

open class BaseFragment : Fragment() {

    private val baseViewModel: BaseViewModel by inject()
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        baseViewModel.getIsLoading()
            .observe(viewLifecycleOwner, EventObserver { isLoading ->
                if (isLoading) {
                    showDialog()
                } else {
                    hideDialog()
                }
            })

    }


    private fun showDialog() {
        if (requireActivity() is BaseActivity) {
            if (!(requireActivity() as BaseActivity).isFinishing)
                (requireActivity() as BaseActivity).loadingDialog.show()
        }
    }

    private fun hideDialog() {
        if (requireActivity() is BaseActivity)
            (requireActivity() as BaseActivity).loadingDialog.dismiss()
    }


}