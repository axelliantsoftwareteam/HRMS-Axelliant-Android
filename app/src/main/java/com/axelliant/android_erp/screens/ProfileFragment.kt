package com.axelliant.android_erp.screens

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.axelliant.android_erp.base.BaseFragment
import com.axelliant.android_erp.databinding.FragmentLeavesBinding
import com.axelliant.android_erp.databinding.FragmentLoginBinding
import com.axelliant.android_erp.databinding.FragmentProfileBinding
import com.axelliant.android_erp.viewmodel.LoginViewModel
import org.koin.android.ext.android.inject

class ProfileFragment : BaseFragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentProfileBinding.inflate(inflater).also { _binding = it }
        return binding?.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

    }

}