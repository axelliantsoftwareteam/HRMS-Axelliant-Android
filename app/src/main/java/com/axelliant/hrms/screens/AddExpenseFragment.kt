package com.axelliant.hrms.screens

import android.os.Bundle
import android.os.Handler
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.axelliant.hrms.R
import com.axelliant.hrms.config.AppConst
import com.axelliant.hrms.databinding.FragmentAddExpenseBinding
import com.axelliant.hrms.databinding.FragmentRequestBinding
import com.axelliant.hrms.enums.RequestFilter
import com.axelliant.hrms.event.EventObserver
import com.axelliant.hrms.extention.nullToEmpty
import com.axelliant.hrms.extention.showErrorMsg
import com.axelliant.hrms.extention.showSuccessMsg
import com.axelliant.hrms.model.checkin.CheckInDetail
import com.axelliant.hrms.model.leave.LeaveDetail
import com.axelliant.hrms.model.post.LeaveRequest
import com.axelliant.hrms.navigation.AppNavigator
import com.google.gson.Gson

class AddExpenseFragment : Fragment() {

    private var _binding: FragmentAddExpenseBinding? = null
    private val binding get() = _binding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {


        _binding = FragmentAddExpenseBinding.inflate(inflater).also { _binding = it }
        return binding?.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding?.btnApply?.setOnClickListener {

        }




        binding?.ivBack?.setOnClickListener {
            AppNavigator.moveBackToPreviousFragment()
        }

    }


}