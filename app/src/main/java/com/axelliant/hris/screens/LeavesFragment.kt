package com.axelliant.hris.screens

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.axelliant.hris.R
import com.axelliant.hris.adapter.RemainingLeaveAdapter
import com.axelliant.hris.adapter.UpcomingLeaveAdapter

import com.axelliant.hris.base.BaseFragment
import com.axelliant.hris.config.AppConst.SERVER_DATE_FORMAT
import com.axelliant.hris.config.GlobalConfig
import com.axelliant.hris.databinding.FragmentLeavesBinding
import com.axelliant.hris.enums.AttendanceFilter
import com.axelliant.hris.event.EventObserver
import com.axelliant.hris.extention.showErrorMsg
import com.axelliant.hris.model.attendance.AttendanceInput
import com.axelliant.hris.model.leave.LeaveType
import com.axelliant.hris.model.leave.SelfLeaveStats
import com.axelliant.hris.model.leave.TeamLeaveStats
import com.axelliant.hris.model.leave.UpcomingLeaveInput
import com.axelliant.hris.model.leave.UpcomingLeaves
import com.axelliant.hris.navigation.AppNavigator
import com.axelliant.hris.utils.Utils
import com.axelliant.hris.viewmodel.LeaveViewModel
import org.koin.android.ext.android.inject
import java.time.LocalDate
import java.time.Year
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit


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


    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        leaveViewModel.getIsLoading()
            .observe(viewLifecycleOwner, EventObserver { isLoading ->
                if (isLoading) {
                    showDialog()
                } else {
                    hideDialog()
                }
            })

        val isManager = GlobalConfig.isCurrentManager()
        binding?.tvMyTeam?.isVisible = isManager
        binding?.tvMyTeamView?.isVisible = isManager
        binding?.lyMyteamAttend?.isVisible = isManager

        leaveViewModel.getLeaveStats(getCurrentObject())
        eventSelection()

        leaveViewModel.leaveStatResponse.observe(
            viewLifecycleOwner,
            EventObserver { response ->

                if (response?.meta?.status == true) {

                    selfAttendanceStats(response.self_count!!)

                    if(currentFilter == AttendanceFilter.WEEK)
                        teamAttendanceStats(response.team_count!!)

                    remainingLeaveDataPopulate(response.remaining_balance!!)

                } else {
                    requireContext().showErrorMsg(response?.meta?.message.toString())
                }

            })

        binding?.tvView?.setOnClickListener {
            AppNavigator.navigateToMyLeaveDetail()
        }

        binding?.tvMyTeamView?.setOnClickListener {
            AppNavigator.navigateToTeamLeaveDetail()

        }

        binding?.ivBack?.setOnClickListener {
            AppNavigator.moveBackToPreviousFragment()

        }


        leaveViewModel.getUpcomingLeaveDetail(UpcomingLeaveInput().apply {
            val formatter = DateTimeFormatter.ofPattern(SERVER_DATE_FORMAT)
            val tomorrow = LocalDate.now().plus(1, ChronoUnit.DAYS)
            val formattedTomorrow = tomorrow.format(formatter)

            // Get the last day of the current year
            val lastDayOfYear = Year.now().atMonth(12).atEndOfMonth()
            val formattedLastDayOfYear = lastDayOfYear.format(formatter)

            Log.d("Upcoming Leaves date", "dates:" + formattedLastDayOfYear+formattedTomorrow)
            if (formattedTomorrow!=null && formattedLastDayOfYear!=null)
            {
                this.start_date = formattedTomorrow
                this.end_date = formattedLastDayOfYear
            }
        })
        leaveViewModel.upcomingLeavesResponse.observe(
            viewLifecycleOwner,
            EventObserver { response ->
                if (response?.meta?.status == true) {
                    if (response.upcoming_leaves?.size?: 0 > 0){
                        binding?.tvMyShift?.isVisible=true
                        upcomingLeavePopulate(response.upcoming_leaves)
                    }
                    else{
                        binding?.tvMyShift?.isVisible=false
                        binding?.rvUpcomingLeaves?.isVisible=false
                    }
                } else {
                    requireContext().showErrorMsg(response?.meta?.message.toString())
                }
            })
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
            leaveViewModel.getLeaveStats(getCurrentObject())
            eventSelection()
        }

        binding?.tvMonth?.setOnClickListener {
            currentFilter = AttendanceFilter.MONTH
            leaveViewModel.getLeaveStats(getCurrentObject())
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


    private fun remainingLeaveDataPopulate(leaves: ArrayList<LeaveType>) {
        binding?.rvRemaining?.layoutManager = GridLayoutManager(requireContext(), 3)
        val modulesAdapter = RemainingLeaveAdapter(
            leaves
        )
        binding?.rvRemaining?.adapter = modulesAdapter
        binding?.rvRemaining?.isNestedScrollingEnabled = false


    }


    private fun upcomingLeavePopulate(upcomingLeaves: ArrayList<UpcomingLeaves>?) {
        Log.d("upcomingLeaves", ""+upcomingLeaves)
        if (upcomingLeaves != null) {
            binding?.rvUpcomingLeaves?.layoutManager =
                LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL, false)
            val modulesAdapter = UpcomingLeaveAdapter(
                upcomingLeaves
            )
            binding?.rvUpcomingLeaves?.adapter = modulesAdapter
            binding?.rvUpcomingLeaves?.isNestedScrollingEnabled = false
        }
    }

    private fun selfAttendanceStats(selfStats: SelfLeaveStats) {

        binding?.tvTotalLeaveTxt?.text = selfStats.total_leave.toString() //total leaves

        binding?.tvPendingTxt?.text = selfStats.self_pending.toString()
        binding?.tvApprovedTxt?.text = selfStats.self_approved.toString()
        binding?.tvRejectedTxt?.text = selfStats.self_reject.toString()
        binding?.tvUsedLeavesTxt?.text = selfStats.remaining_leave.toString()  // reamining leave

        /*        binding?.tvCasualTxt?.text = selfAttendanceStats.week_count.toString()
                binding?.tvSickTxt?.text = selfAttendanceStats.week_count.toString()
                binding?.tvAnnualTxt?.text = selfAttendanceStats.week_count.toString()*/

    }

    private fun teamAttendanceStats(teamLeaveStats: TeamLeaveStats) {

        binding?.tvTotalMemberTxt?.text = teamLeaveStats.total_team_members.toString()
        binding?.tvPresentTxt?.text = teamLeaveStats.all_leaves.toString()
        binding?.tvWorkHomeTxt?.text = teamLeaveStats.team_approved.toString()
        binding?.tvTeamsAbsentTxt?.text = teamLeaveStats.team_reject.toString()
        binding?.tvTeamsOnleaveTxt?.text = teamLeaveStats.team_pending.toString()

    }

    private fun getCurrentObject(): AttendanceInput {

        var localStart = ""
        var localEnd = ""
        when (currentFilter) {
            AttendanceFilter.WEEK -> {
                localStart = Utils.getServerFormat(date = Utils.getLastWeek())
                localEnd = Utils.getServerFormat()

            }

            AttendanceFilter.MONTH -> {
                localStart = Utils.getServerFormat(date = Utils.getFirstDayOfMonth())
                localEnd =
                    Utils.getServerFormat(date = Utils.getLastDayOfMonth())
            }

            else -> {}

        }
        return AttendanceInput().apply {
            this.startDate = localStart
            this.endDate = localEnd
            this.filter = currentFilter

        }

    }

}