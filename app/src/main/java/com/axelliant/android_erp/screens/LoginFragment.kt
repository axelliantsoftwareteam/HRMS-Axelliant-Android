package com.axelliant.android_erp.screens

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.axelliant.android_erp.base.BaseFragment
import com.axelliant.android_erp.config.AppConst
import com.axelliant.android_erp.databinding.FragmentLoginBinding
import com.axelliant.android_erp.viewmodel.LoginViewModel
import org.koin.android.ext.android.inject
import org.koin.core.component.inject

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

//        if (arguments != null) {
//            val comingFromValue =
//                requireArguments().getString(AppConst.KEY_PARAM, "")
//            binding?.loginView?.text = comingFromValue
//        }

//        loginViewModel.getConversationCall(1)
    }

}