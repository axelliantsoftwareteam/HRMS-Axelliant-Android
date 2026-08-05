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
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import com.axelliant.hris.extention.showShimmer
import com.axelliant.hris.extention.hideShimmer
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.dp
import com.microsoft.fluentui.theme.FluentTheme
import com.axelliant.hris.components.AttendanceStatsGrid
import com.axelliant.hris.components.DetailCard
import com.axelliant.hris.components.DetailLabelValue
import com.intuit.sdp.R as SdpR





@AndroidEntryPoint
class AttendanceStatsFragment : BaseFragment() {
    private var currentFilter = AttendanceFilter.WEEK
    private var isDataLoaded = false

    private var _binding: FragmentAttendanceStatsBinding? = null
    private val binding get() = _binding

    private val attendanceViewModel: AttendanceViewModel by viewModels()
    private val absentState = mutableStateOf("0")
    private val presentState = mutableStateOf("0")
    private val missedPunchOutState = mutableStateOf("0")
    private val leavesState = mutableStateOf("0")
    private val holidayState = mutableStateOf("0")
    private val weeklyOffsState = mutableStateOf("0")

    private val teamCountState = mutableStateOf("0")
    private val teamPresentState = mutableStateOf("0")
    private val teamWfhState = mutableStateOf("0")
    private val teamAbsentState = mutableStateOf("0")
    private val teamOnLeaveState = mutableStateOf("0")

    private val shiftNameState = mutableStateOf("")
    private val shiftTimingsState = mutableStateOf("")
    private val workFromState = mutableStateOf("")


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
        setupComposeViews()
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
    override fun onDestroyView() {
        binding?.shimmerLayout?.stopShimmer()
        super.onDestroyView()
        _binding = null
    }
    private fun setShiftTimings(shiftDetails: ShiftData) {
        if (shiftDetails != null) {
            shiftNameState.value = shiftDetails.name.valueQualifier()
            workFromState.value = shiftDetails.location.valueQualifier()
            shiftTimingsState.value = shiftDetails.actual_start.plus(" - ").plus(shiftDetails.actual_end).valueQualifier()
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
                    ContextCompat.getDrawable(requireContext(), R.drawable.fluent_blue)
                binding?.tvWeek?.setTextColor(requireContext().getColor(R.color.white))

            }

            AttendanceFilter.MONTH -> {

                binding?.tvMonth?.background =
                    ContextCompat.getDrawable(requireContext(), R.drawable.fluent_blue)
                binding?.tvMonth?.setTextColor(requireContext().getColor(R.color.white))
            }


            else -> {}
        }
    }


    private fun selfAttendanceStats(selfAttendanceStats: SelfAttendanceStats){
        absentState.value = selfAttendanceStats.absent_count.toString()
        presentState.value = selfAttendanceStats.present.toString()
        missedPunchOutState.value = selfAttendanceStats.missed_punch_out.toString()
        leavesState.value = selfAttendanceStats.leave_count.toString()
        holidayState.value = selfAttendanceStats.holiday_count.toString()
        weeklyOffsState.value = selfAttendanceStats.week_count.toString()

    }
    private fun teamAttendanceStats(teamAttendanceStats: TeamAttendanceStats){
        teamCountState.value = teamAttendanceStats.team_count.toString()
        teamPresentState.value = teamAttendanceStats.present.toString()
        teamWfhState.value = teamAttendanceStats.work_from_home.toString()
        teamOnLeaveState.value = teamAttendanceStats.leave_count.toString()
        teamAbsentState.value = teamAttendanceStats.absent_count.toString()

    }

    private fun setupComposeViews() {
        val red = ComposeColor(ContextCompat.getColor(requireContext(), R.color.pure_red))
        val blue = ComposeColor(ContextCompat.getColor(requireContext(), R.color.sky_blue))
        val neutral = ComposeColor(0xFF1C1C1E)
        val green = ComposeColor(ContextCompat.getColor(requireContext(), R.color.green))

        binding?.composeMyAttendanceStats?.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                FluentTheme {
                    AttendanceStatsGrid(
                        absent = absentState.value,
                        present = presentState.value,
                        missedPunchOut = missedPunchOutState.value,
                        leaves = leavesState.value,
                        holiday = holidayState.value,
                        weeklyOffs = weeklyOffsState.value,
                        redColor = red,
                        blueColor = blue,
                        neutralColor = neutral
                    )
                }
            }
        }

        binding?.composeMyShiftDetails?.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                FluentTheme {
                    DetailCard {


                        DetailLabelValue(
                            label = "Shift Name",
                            value = shiftNameState.value
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Top
                        ) {

                            DetailLabelValue(
                                modifier = Modifier.weight(1.7f),
                                label = "Shift Timings",
                                value = shiftTimingsState.value
                            )

                            Spacer(
                                modifier = Modifier.width(
                                    dimensionResource(SdpR.dimen._20sdp)
                                )
                            )
                            DetailLabelValue(
                                modifier = Modifier.weight(1f),
                                label = "Working From",
                                value = workFromState.value,
                                dotColor = green
                            )
                        }
                    }
                }
            }
        }
        binding?.composeMyTeamStats?.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                FluentTheme {
                    DetailCard {
                        DetailLabelValue(label = "Total Members", value = teamCountState.value)
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(modifier = Modifier.fillMaxWidth()) {
                            DetailLabelValue(Modifier.weight(1f), "Present on Site", teamPresentState.value)
                            DetailLabelValue(Modifier.weight(1f), "Work From Home", teamWfhState.value)
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(modifier = Modifier.fillMaxWidth()) {
                            DetailLabelValue(Modifier.weight(1f), "Absent", teamAbsentState.value)
                            DetailLabelValue(Modifier.weight(1f), "On Leave", teamOnLeaveState.value)
                        }
                    }
                }
            }
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
