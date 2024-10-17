package com.axelliant.hris.screens

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.axelliant.hris.R
import com.axelliant.hris.base.BaseFragment
import com.axelliant.hris.config.AppConst.KEY_ID
import com.axelliant.hris.config.GlobalConfig
import com.axelliant.hris.databinding.FragmentAttendanceStatsBinding
import com.axelliant.hris.enums.AttendanceFilter
import com.axelliant.hris.event.EventObserver
import com.axelliant.hris.extention.showErrorMsg
import com.axelliant.hris.extention.valueQualifier
import com.axelliant.hris.model.attendance.AttendanceInput
import com.axelliant.hris.model.attendance.SelfAttendanceStats
import com.axelliant.hris.model.attendance.ShiftData
import com.axelliant.hris.model.attendance.TeamAttendanceStats
import com.axelliant.hris.navigation.AppNavigator
import com.axelliant.hris.utils.Utils
import com.axelliant.hris.viewmodel.AttendanceViewModel
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

        attendanceViewModel.getAttendanceStats(getCurrentObject())
        eventSelection()

        attendanceViewModel.attendanceResponse.observe(
            viewLifecycleOwner,
            EventObserver { response ->

                if (response?.meta?.status == true) {

                    selfAttendanceStats(response.self_attendance_counts!!)

                    if(currentFilter == AttendanceFilter.WEEK)
                        teamAttendanceStats(response.team_attendance_counts!!)

                    setShiftTimings(response.shift_detail!!)

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

    private fun setShiftTimings(shiftDetails: ShiftData) {
        if (shiftDetails!=null)
        {
            binding?.tvShiftNameTxt?.text=shiftDetails.name.valueQualifier()
            binding?.tvWorkFrom?.text=shiftDetails.location.valueQualifier()
            binding?.tvShiftPremiss?.text=shiftDetails.actual_start.plus(" - ").plus(shiftDetails.actual_end).valueQualifier()
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
            attendanceViewModel.getAttendanceStats(getCurrentObject())
            eventSelection()
        }
        binding?.tvMonth?.setOnClickListener {
            currentFilter = AttendanceFilter.MONTH
            attendanceViewModel.getAttendanceStats(getCurrentObject())
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
        binding?.tvWorkHomeTxt?.text = teamAttendanceStats.work_from_home.toString()
        binding?.tvTeamsOnleaveTxt?.text = teamAttendanceStats.leave_count.toString()
        binding?.tvTeamsAbsentTxt?.text = teamAttendanceStats.absent_count.toString()

    }
    private fun getCurrentObject(): AttendanceInput {

        var startDateString = ""
        var endDateString = ""
        when (currentFilter) {
            AttendanceFilter.WEEK -> {
                startDateString = ""
                endDateString = ""

            }

            AttendanceFilter.MONTH -> {
                startDateString = Utils.getServerFormat(date = Utils.getFirstDayOfMonth())
                endDateString =
                    Utils.getServerFormat(date = Utils.getLastDayOfMonth())

            }

            AttendanceFilter.Custom -> {}
        }
        return AttendanceInput().apply {
            this.startDate = startDateString
            this.endDate = endDateString
            this.filter = currentFilter

        }

    }

}