package com.axelliant.hris.screens

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
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
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import com.axelliant.hris.extention.showShimmer
import com.axelliant.hris.extention.hideShimmer
import com.axelliant.hris.ui.designsystem.adapters.FilterAdapter
import com.axelliant.hris.ui.designsystem.adapters.FilterItem
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry


@AndroidEntryPoint
class AttendanceStatsFragment : BaseFragment() {
    private var currentFilter = AttendanceFilter.WEEK
    private var isDataLoaded = false

    private var _binding: FragmentAttendanceStatsBinding? = null
    private val binding get() = _binding

    private val attendanceViewModel: AttendanceViewModel by viewModels()
    private lateinit var filterAdapter: FilterAdapter



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

        binding?.lyMyteamAttend?.isVisible = isManager
        if (!isDataLoaded) {
            binding?.shimmerLayout?.showShimmer(binding?.contentGroup!!)
        }
        attendanceViewModel.getAttendanceStats(getCurrentObject())
        setupFilterBar()

        attendanceViewModel.attendanceResponse.observe(
            viewLifecycleOwner,
            EventObserver { response ->
                binding?.shimmerLayout?.hideShimmer(binding?.contentGroup!!)
                isDataLoaded = true
                if (response?.meta?.status == true) {

                    selfAttendanceStats(response.self_attendance_counts!!)

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

    private fun setupFilterBar() {
        val items = listOf(
            FilterItem(getString(R.string.last_seven), 0),
            FilterItem(getString(R.string.this_month), 1)
        )
        filterAdapter = FilterAdapter(items, selectedPosition = 0) { position, _ ->
            currentFilter = if (position == 0) AttendanceFilter.WEEK else AttendanceFilter.MONTH
            filterAdapter.setSelected(position)
            attendanceViewModel.getAttendanceStats(getCurrentObject())
        }
        binding?.rvAttendanceFilters?.apply {
            layoutManager =
                GridLayoutManager(requireContext(), items.size) // spanCount = item count
            adapter = filterAdapter
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
    private fun teamAttendanceStats(teamAttendanceStats: TeamAttendanceStats) {
        val teamCount = teamAttendanceStats.team_count      // headcount — for center label only
        val present = teamAttendanceStats.present
        val wfh = teamAttendanceStats.work_from_home
        val absent = teamAttendanceStats.absent_count
        val onLeave = teamAttendanceStats.leave_count

        val recordTotal = present + wfh + absent + onLeave   // denominator for %, pie, bars

        binding?.tvTeamTotalCount?.text = teamCount.toString()   // still shows real headcount
        binding?.tvTeamPresentCount?.text = present.toString()
        binding?.tvTeamWfhCount?.text = wfh.toString()
        binding?.tvTeamOnLeaveCount?.text = onLeave.toString()
        binding?.tvTeamAbsentCount?.text = absent.toString()

        setRowStat(binding?.pbPresent, binding?.tvPresentPercent, present, recordTotal)
        setRowStat(binding?.pbWfh, binding?.tvWfhPercent, wfh, recordTotal)
        setRowStat(binding?.pbAbsent, binding?.tvAbsentPercent, absent, recordTotal)
        setRowStat(binding?.pbLeave, binding?.tvLeavePercent, onLeave, recordTotal)

        setupDonutChart(present, wfh, absent, onLeave)
    }

    private fun setRowStat(progressBar: ProgressBar?, percentView: com.axelliant.hris.ui.designsystem.components.AppTextView?, count: Int, total: Int) {
        val pct = if (total > 0) (count * 100 / total) else 0
        progressBar?.progress = pct
        percentView?.text = "($pct%)"
    }

    private fun setupDonutChart(present: Int, wfh: Int, absent: Int, onLeave: Int) {
        val entries = listOf(
            PieEntry(present.toFloat()),
            PieEntry(wfh.toFloat()),
            PieEntry(absent.toFloat()),
            PieEntry(onLeave.toFloat())
        )
        val colors = listOf(
            ContextCompat.getColor(requireContext(), R.color.green),
            ContextCompat.getColor(requireContext(), R.color.sky_blue),
            ContextCompat.getColor(requireContext(), R.color.pure_red),
            ContextCompat.getColor(requireContext(), R.color.purple)
        )
        val dataSet = PieDataSet(entries, "").apply {
            this.colors = colors
            sliceSpace = 3f
            setDrawValues(false)
        }
        binding?.pieTeamStats?.apply {
            data = PieData(dataSet)
            description.isEnabled = false
            legend.isEnabled = false
            setDrawEntryLabels(false)
            isDrawHoleEnabled = true
            holeRadius = 72f
            transparentCircleRadius = 0f
            setTouchEnabled(false)
            setDrawCenterText(false)
            animateY(600)
            invalidate()
        }
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
