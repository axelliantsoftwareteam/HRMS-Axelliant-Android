package com.axelliant.hris.screens

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.axelliant.hris.R
import com.axelliant.hris.base.BaseFragment
import com.axelliant.hris.core.constants.AppRouteArgs
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
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import com.axelliant.hris.extention.showShimmer
import com.axelliant.hris.extention.hideShimmer





@AndroidEntryPoint
class AttendanceStatsFragment : BaseFragment() {
    private var currentFilter = AttendanceFilter.WEEK
    private var isDataLoaded = false

    private var _binding: FragmentAttendanceStatsBinding? = null
    private val binding get() = _binding

    private val attendanceViewModel: AttendanceViewModel by viewModels()


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
                if (!isDataLoaded && isLoading) return@EventObserver
                if (isLoading) showDialog() else hideDialog()
            })

        val isManager = GlobalConfig.isCurrentManager()

        binding?.tvMyTeamStat?.isVisible = isManager
        binding?.tvMyTeamView?.isVisible = isManager
        binding?.lyMyteamAttend?.isVisible = isManager
        if (!isDataLoaded) {
            binding?.shimmerLayout?.showShimmer(binding?.contentGroup!!)
        }
        attendanceViewModel.getAttendanceStats(getCurrentObject())
        eventSelection()

        attendanceViewModel.attendanceResponse.observe(
            viewLifecycleOwner,
            EventObserver { response ->
                binding?.shimmerLayout?.hideShimmer(binding?.contentGroup!!)
                isDataLoaded = true
                if (response?.meta?.status == true) {

                    selfAttendanceStats(response.self_attendance_counts!!)

                    if(currentFilter == AttendanceFilter.WEEK)
                        teamAttendanceStats(response.team_attendance_counts!!)

                    setShiftTimings(response.shift_detail!!)

                } else {
                    requireContext().showErrorMsg(response?.meta?.message.toString())
                }

            })
        binding?.appTopBar?.setOnBackClickListener {
            previousFragmentNavigation()
        }

        binding?.tvMyTeam?.setOnClickListener {
            showDialog()
            AppNavigator.navigateToMyAttendanceDetail(Bundle().apply {
                this.putString(AppRouteArgs.EMPLOYEE_ID,GlobalConfig.currentEmployeeId())
            })

        }

        binding?.tvMyTeamView?.setOnClickListener {
            showDialog()
            AppNavigator.navigateToTeamAttendanceDetail()

        }

    }
    override fun onDestroyView() {
        binding?.shimmerLayout?.stopShimmer()
        super.onDestroyView()
        _binding = null
    }
    private fun setShiftTimings(shiftDetails: ShiftData) {
        binding?.tvShiftName?.text = shiftDetails.name.valueQualifier()
        binding?.tvWorkFrom?.text = shiftDetails.location.valueQualifier()
        binding?.tvShiftTimings?.text =
            shiftDetails.actual_start.plus(" - ").plus(shiftDetails.actual_end).valueQualifier()
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
                    ContextCompat.getDrawable(requireContext(), R.drawable.fluent_blue)
                binding?.tvWeek?.setTextColor(requireContext().getColor(R.color.ds_neutral_white))

            }

            AttendanceFilter.MONTH -> {

                binding?.tvMonth?.background =
                    ContextCompat.getDrawable(requireContext(), R.drawable.fluent_blue)
                binding?.tvMonth?.setTextColor(requireContext().getColor(R.color.ds_neutral_white))
            }
            AttendanceFilter.Custom -> {}
        }
    }


    private fun selfAttendanceStats(selfAttendanceStats: SelfAttendanceStats){
        binding?.tvAbsentCount?.text = selfAttendanceStats.absent_count.toString()
        binding?.tvPresentCount?.text = selfAttendanceStats.present.toString()
        binding?.tvMissedPunchCount?.text = selfAttendanceStats.missed_punch_out.toString()
        binding?.tvLeavesCount?.text = selfAttendanceStats.leave_count.toString()
        binding?.tvHolidayCount?.text = selfAttendanceStats.holiday_count.toString()
        binding?.tvWeeklyOffCount?.text = selfAttendanceStats.week_count.toString()

    }
    private fun teamAttendanceStats(teamAttendanceStats: TeamAttendanceStats){
        binding?.tvTeamTotalCount?.text = teamAttendanceStats.team_count.toString()
        binding?.tvTeamPresentCount?.text = teamAttendanceStats.present.toString()
        binding?.tvTeamWfhCount?.text = teamAttendanceStats.work_from_home.toString()
        binding?.tvTeamOnLeaveCount?.text = teamAttendanceStats.leave_count.toString()
        binding?.tvTeamAbsentCount?.text = teamAttendanceStats.absent_count.toString()

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
