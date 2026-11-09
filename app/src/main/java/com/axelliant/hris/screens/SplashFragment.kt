package com.axelliant.hris.screens

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import com.axelliant.hris.R
import com.axelliant.hris.base.BaseFragment
import com.axelliant.hris.databinding.FragmentSplashBinding
import com.axelliant.hris.navigation.AppNavigator
import com.axelliant.hris.utils.SessionManager
import com.bumptech.glide.Glide
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

/** A brief branded entry; animation decoding/failure never gates navigation. */
class SplashFragment : BaseFragment() {
    private var binding: FragmentSplashBinding? = null
    private val sessions: SessionManager by inject()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return FragmentSplashBinding.inflate(inflater, container, false).also { binding = it }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding?.myImageView?.let { Glide.with(this).asGif().load(R.drawable.applogo).into(it) }
        viewLifecycleOwner.lifecycleScope.launch {
            delay(750L)
            if (sessions.checkLogin() && !sessions.getToken().isNullOrBlank()) AppNavigator.navigateToHome()
            else AppNavigator.navigateToLogin()
        }
    }

    override fun onDestroyView() {
        binding?.myImageView?.let { Glide.with(this).clear(it) }
        binding = null
        super.onDestroyView()
    }
}
