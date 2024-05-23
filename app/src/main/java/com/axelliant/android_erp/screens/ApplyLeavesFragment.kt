package com.axelliant.android_erp.screens

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.axelliant.android_erp.R
import com.axelliant.android_erp.databinding.FragmentApplyLeavesBinding
import com.axelliant.android_erp.databinding.FragmentLeavesBinding


class ApplyLeavesFragment : Fragment() {

    private var _binding: FragmentApplyLeavesBinding? = null
    private val binding get() = _binding


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentApplyLeavesBinding.inflate(inflater).also { _binding = it }
        return binding?.root
    }

}




