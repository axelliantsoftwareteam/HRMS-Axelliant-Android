package com.axelliant.android_erp.screens

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.axelliant.android_erp.base.BaseFragment
import com.axelliant.android_erp.databinding.FragmentLoginBinding
import com.axelliant.android_erp.navigation.AppNavigator
import com.axelliant.android_erp.viewmodel.LoginViewModel
import org.koin.android.ext.android.inject
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

class LoginFragment : BaseFragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding
    private val loginViewModel: LoginViewModel by inject()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentLoginBinding.inflate(inflater).also { _binding = it }
        return binding?.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding?.btnMicLogin?.setOnClickListener {
            AppNavigator.navigateToHome()

        }

//        if (arguments != null) {
//            val comingFromValue =
//                requireArguments().getString(AppConst.KEY_PARAM, "")
//            binding?.loginView?.text = comingFromValue
//        }

//        loginViewModel.getConversationCall(1)

        try {
            val info: PackageInfo = requireActivity().packageManager.getPackageInfo(
                requireActivity().packageName,
                PackageManager.GET_SIGNATURES
            )
            for (signature: Signature in info.signatures) {
                val messageDigest: MessageDigest = MessageDigest.getInstance("SHA")
                messageDigest.update(signature.toByteArray())
                Log.d(
                    "KeyHash:",
                    "KeyHash=" + android.util.Base64.encodeToString(messageDigest.digest(), android.util.Base64.DEFAULT)
                )
            }
        } catch (e: PackageManager.NameNotFoundException) {
            // Handle NameNotFoundException
        } catch (e: NoSuchAlgorithmException) {
            // Handle NoSuchAlgorithmException
        }

    }

}