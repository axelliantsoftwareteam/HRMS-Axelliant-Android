package com.axelliant.android_erp.screens

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.axelliant.android_erp.R
import com.axelliant.android_erp.base.BaseFragment
import com.axelliant.android_erp.config.AppConst.KEY_ID
import com.axelliant.android_erp.config.GlobalConfig
import com.axelliant.android_erp.databinding.FragmentAttendanceStatsBinding
import com.axelliant.android_erp.enums.AttendanceFilter
import com.axelliant.android_erp.event.EventObserver
import com.axelliant.android_erp.extention.showErrorMsg
import com.axelliant.android_erp.model.attendance.SelfAttendanceStats
import com.axelliant.android_erp.model.attendance.TeamAttendanceStats
import com.axelliant.android_erp.navigation.AppNavigator
import com.axelliant.android_erp.viewmodel.AttendanceViewModel
import com.axelliant.android_erp.viewmodel.LeaveViewModel
import org.koin.android.ext.android.inject



class AttendanceStatsFragment : BaseFragment() {
    private var currentFilter = AttendanceFilter.WEEK

    private var _binding: FragmentAttendanceStatsBinding? = null
    private val binding get() = _binding

    private val attendanceViewModel: AttendanceViewModel by inject()


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentAttendanceStatsBinding.inflate(inflater).also { _binding = it }
        return binding?.root
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


        val isManager = GlobalConfig.isCurrentManager()

        binding?.tvMyTeamStat?.isVisible = isManager
        binding?.tvMyTeamView?.isVisible = isManager
        binding?.lyMyteamAttend?.isVisible = isManager

        attendanceViewModel.getAttendanceStats(currentFilter)
        eventSelection()

        attendanceViewModel.attendanceResponse.observe(
            viewLifecycleOwner,
            EventObserver { response ->

                if (response?.meta?.status == true) {

                    selfAttendanceStats(response.self_attendance_counts!!)
                    teamAttendanceStats(response.team_attendance_counts!!)

                } else {
                    requireContext().showErrorMsg(response?.meta?.message.toString())
                }

            })


        binding?.ivBack?.setOnClickListener {
            previousFragmentNavigation()

        }

        binding?.tvMyTeam?.setOnClickListener {
            showDialog()
            AppNavigator.navigateToMyAttendanceDetail(Bundle().apply {
                this.putString(KEY_ID,GlobalConfig.currentEmployeeId())
            })

        }

        binding?.tvMyTeamView?.setOnClickListener {
            showDialog()
            AppNavigator.navigateToTeamAttendanceDetail()

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
            attendanceViewModel.getAttendanceStats(currentFilter)
            eventSelection()
        }
        binding?.tvMonth?.setOnClickListener {
            currentFilter = AttendanceFilter.MONTH
            attendanceViewModel.getAttendanceStats(currentFilter)
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
        binding?.tvAbsentTxt?.text = selfAttendanceStats.absent_count.toString()
        binding?.tvHalfDayTxt?.text =selfAttendanceStats.present.toString()
        binding?.tvMissPunchOutTxt?.text =selfAttendanceStats.missed_punch_out.toString()
        binding?.tvLeavesTxt?.text =selfAttendanceStats.leave_count.toString()
        binding?.tvHolidayTxt?.text =selfAttendanceStats.holiday_count.toString()
        binding?.tvWeeklyOffsTxt?.text =selfAttendanceStats.week_count.toString()

    }
    private fun teamAttendanceStats(teamAttendanceStats: TeamAttendanceStats){
        binding?.tvTotalMemberTxt?.text = teamAttendanceStats.team_count.toString()
        binding?.tvPresentTxt?.text = teamAttendanceStats.present.toString()
        binding?.tvWorkFromTxt?.text = teamAttendanceStats.work_from_home.toString()
        binding?.tvTeamsOnleaveTxt?.text = teamAttendanceStats.leave_count.toString()
        binding?.tvTeamsAbsentTxt?.text = teamAttendanceStats.absent_count.toString()

    }

}