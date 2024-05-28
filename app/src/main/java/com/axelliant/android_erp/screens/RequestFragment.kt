package com.axelliant.android_erp.screens

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.axelliant.android_erp.base.BaseFragment
import com.axelliant.android_erp.databinding.FragmentRequestBinding


class RequestFragment : BaseFragment() {

    private var _binding: FragmentRequestBinding? = null
    private val binding get() = _binding


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentRequestBinding.inflate(inflater).also { _binding = it }
        return binding?.root
    }

}




