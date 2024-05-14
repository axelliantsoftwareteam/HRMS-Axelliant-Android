package com.axelliant.android_erp.screens

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.axelliant.android_erp.R
import com.axelliant.android_erp.base.BaseFragment
import com.axelliant.android_erp.config.AppConst.KEY_PARAM
import com.axelliant.android_erp.databinding.FragmentSplashBinding
import com.axelliant.android_erp.navigation.AppNavigator
import com.bumptech.glide.Glide

class SplashFragment : BaseFragment() {

    private var _binding: FragmentSplashBinding? = null
    private val binding get() = _binding
    private var animate = true
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
        binding?.tvTitle?.setOnClickListener {
            val bundle = Bundle()
            bundle.putString(KEY_PARAM, "ComesFromLogin")
            AppNavigator.navigateToLogin(bundle)

        }

        // Load GIF when running the app:
        loadGif(animate)
    }
    private fun loadGif(isAnimationActive: Boolean){
        if (isAnimationActive){
            binding?.let {
                Glide.with(this)
                    .asGif()  // Load as animated GIF
                    .load(R.drawable.applogo)  // Call your GIF here (url, raw, etc.)
                    .into(it.myImageView)
            }

            animate = true
//            val bundle = Bundle()
//            bundle.putString(KEY_PARAM, "ComesFromLogin")
//            AppNavigator.navigateToLogin(bundle)
        }
    }

}