package com.axelliant.android_erp.screens

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.axelliant.android_erp.R

import com.axelliant.android_erp.base.BaseFragment
import com.axelliant.android_erp.databinding.FragmentLeavesBinding
import com.axelliant.android_erp.enums.AttendanceFilter
import com.axelliant.android_erp.event.EventObserver
import com.axelliant.android_erp.extention.showErrorMsg
import com.axelliant.android_erp.model.attendance.SelfAttendanceStats
import com.axelliant.android_erp.model.attendance.TeamAttendanceStats
import com.axelliant.android_erp.navigation.AppNavigator
import com.axelliant.android_erp.viewmodel.LeaveViewModel
import org.koin.android.ext.android.inject


class LeavesFragment : BaseFragment() {
    private var currentFilter = AttendanceFilter.WEEK

    private var _binding: FragmentLeavesBinding? = null
    private val binding get() = _binding
    private val leaveViewModel: LeaveViewModel by inject()
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentLeavesBinding.inflate(inflater).also { _binding = it }
        return binding?.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        leaveViewModel.getLeaveStats(currentFilter)
        eventSelection()


        leaveViewModel.leaveStatResponse.observe(
            viewLifecycleOwner,
            EventObserver { response ->

                if (response?.meta?.status == true) {

                    selfAttendanceStats(response.self_attendance_counts!!)
                    teamAttendanceStats(response.team_attendance_counts!!)

                } else {
                    requireContext().showErrorMsg(response?.meta?.message.toString())
                }

            })

        binding?.tvView?.setOnClickListener {
            AppNavigator.navigateToMyLeaveDetail()
        }

        binding?.tvMyTeamView?.setOnClickListener{
            AppNavigator.navigateToTeamLeaveDetail()

        }

        binding?.ivBack?.setOnClickListener{
            AppNavigator.moveBackToPreviousFragment()

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
            leaveViewModel.getLeaveStats(currentFilter)
            eventSelection()
        }

        binding?.tvMonth?.setOnClickListener {
            currentFilter = AttendanceFilter.MONTH
            leaveViewModel.getLeaveStats(currentFilter)
            eventSelection()
        }

        when (currentFilter) {
            AttendanceFilter.WEEK -> {
                binding?.tvWeek?.background =
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



    private fun selfAttendanceStats(selfAttendanceStats: SelfAttendanceStats){
        binding?.tvTotalLeaveTxt?.text = selfAttendanceStats.absent_count.toString()
        binding?.tvPendingTxt?.text =selfAttendanceStats.present.toString()
        binding?.tvApprovedTxt?.text =selfAttendanceStats.missed_punch_out.toString()
        binding?.tvRejectedTxt?.text =selfAttendanceStats.leave_count.toString()


        binding?.tvUsedLeavesTxt?.text =selfAttendanceStats.holiday_count.toString()
        binding?.tvCasualTxt?.text =selfAttendanceStats.week_count.toString()
        binding?.tvSickTxt?.text =selfAttendanceStats.week_count.toString()
        binding?.tvAnnualTxt?.text =selfAttendanceStats.week_count.toString()

    }
    private fun teamAttendanceStats(teamAttendanceStats: TeamAttendanceStats){
        binding?.tvTotalMemberTxt?.text = teamAttendanceStats.team_count.toString()
        binding?.tvPresentTxt?.text = teamAttendanceStats.present.toString()
        binding?.tvWorkHomeTxt?.text = teamAttendanceStats.work_from_home.toString()
        binding?.tvTeamsAbsentTxt?.text = teamAttendanceStats.leave_count.toString()
        binding?.tvTeamsOnleaveTxt?.text = teamAttendanceStats.absent_count.toString()

    }


}