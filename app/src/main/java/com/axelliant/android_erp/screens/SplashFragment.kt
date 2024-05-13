package com.axelliant.android_erp.screens

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.axelliant.android_erp.base.BaseFragment
import com.axelliant.android_erp.config.AppConst.KEY_PARAM
import com.axelliant.android_erp.databinding.FragmentSplashBinding
import com.axelliant.android_erp.navigation.AppNavigator

class SplashFragment : BaseFragment() {

    private var _binding: FragmentSplashBinding? = null
    private val binding get() = _binding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentSplashBinding.inflate(inflater).also { _binding = it }
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding?.splashView?.setOnClickListener {
            val bundle = Bundle()
            bundle.putString(KEY_PARAM, "ComesFromLogin")
            AppNavigator.navigateToLogin(bundle)

        }
    }

}