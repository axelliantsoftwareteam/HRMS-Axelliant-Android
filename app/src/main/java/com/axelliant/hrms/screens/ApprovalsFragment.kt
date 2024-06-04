package com.axelliant.hrms.screens

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.axelliant.hrms.R
import com.axelliant.hrms.base.BaseFragment
import com.axelliant.hrms.databinding.FragmentApprovalsBinding
import com.axelliant.hrms.databinding.FragmentLoginBinding
import com.axelliant.hrms.enums.ApprovalsFilter
import com.axelliant.hrms.enums.AttendanceFilter
import com.axelliant.hrms.event.EventObserver
import com.axelliant.hrms.extention.showErrorMsg
import com.axelliant.hrms.model.attendance.AttendanceInput
import com.axelliant.hrms.model.dashboard.EmployProfile
import com.axelliant.hrms.navigation.AppNavigator
import com.axelliant.hrms.utils.Utils
import com.axelliant.hrms.viewmodel.AttendanceViewModel
import com.microsoft.identity.client.IPublicClientApplication
import com.microsoft.identity.client.ISingleAccountPublicClientApplication
import com.microsoft.identity.client.PublicClientApplication
import com.microsoft.identity.client.SignInParameters
import com.microsoft.identity.client.exception.MsalException
import org.koin.android.ext.android.inject


class ApprovalsFragment : BaseFragment() {

    private var _binding: FragmentApprovalsBinding? = null
    private val binding get() = _binding!!
    private var currentFilter = AttendanceFilter.WEEK
    private val attendanceViewModel: AttendanceViewModel by inject()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentApprovalsBinding.inflate(inflater, container, false)
        return binding.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        attendanceViewModel.getIsLoading()
            .observe(viewLifecycleOwner, EventObserver { isLoading ->
                if (isLoading) {
                    showDialog()
                } else {
                    hideDialog()
                }
            })

        binding?.ivBack?.setOnClickListener {
            previousFragmentNavigation()
        }

    }

    private fun eventSelection() {
        binding?.tvWeek?.background =
            ContextCompat.getDrawable(requireContext(), R.drawable.rounded_disabled)

        binding?.tvMonth?.background =
            ContextCompat.getDrawable(requireContext(), R.drawable.rounded_disabled)


        binding?.tvWeek?.setTextColor(requireContext().getColor(R.color.btn_text_color))
        binding?.tvMonth?.setTextColor(requireContext().getColor(R.color.btn_text_color))

        binding?.tvWeek?.setOnClickListener {
            currentFilter = AttendanceFilter.WEEK
            attendanceViewModel.getTeamAttendance(getCurrentObject())
            eventSelection()
        }
        binding?.tvMonth?.setOnClickListener {
            currentFilter = AttendanceFilter.MONTH
            attendanceViewModel.getTeamAttendance(getCurrentObject())
            eventSelection()
        }


        when (currentFilter) {
            AttendanceFilter.WEEK -> {
                binding.tvWeek?.background =
                    ContextCompat.getDrawable(requireContext(), R.drawable.rounded_enabled)
                binding?.tvWeek?.setTextColor(requireContext().getColor(R.color.white))

            }

            AttendanceFilter.MONTH -> {

                binding?.tvMonth?.background =
                    ContextCompat.getDrawable(requireContext(), R.drawable.rounded_enabled)
                binding?.tvMonth?.setTextColor(requireContext().getColor(R.color.white))
            }


            else -> {}
        }
    }
    private fun getCurrentObject(): AttendanceInput {

        when (currentFilter) {
            AttendanceFilter.WEEK -> {

            }
            AttendanceFilter.MONTH -> {

            }
            else -> {}
        }


        val currentEmploy = binding?.spTeamMember?.selectedItem as EmployProfile
        return AttendanceInput().apply {
            this.filter = currentFilter
            if (currentEmploy.name == null)
                this.employeeId = listOf()
            else
                this.employeeId = listOf(currentEmploy.name.toString())


        }

    }
}