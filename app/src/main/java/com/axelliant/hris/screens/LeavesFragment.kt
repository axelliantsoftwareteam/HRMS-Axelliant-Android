package com.axelliant.hris.screens

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.axelliant.hris.R
import com.axelliant.hris.adapter.RemainingLeaveAdapter
import com.axelliant.hris.adapter.UpcomingLeaveAdapter
import com.axelliant.hris.base.BaseFragment
import com.axelliant.hris.core.constants.AppDateFormats
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
import androidx.fragment.app.viewModels
import com.axelliant.hris.ui.designsystem.adapters.FilterAdapter
import com.axelliant.hris.ui.designsystem.adapters.FilterItem
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale


@AndroidEntryPoint
class LeavesFragment : BaseFragment() {
    private var currentFilter = AttendanceFilter.WEEK

    private var _binding: FragmentLeavesBinding? = null
    private val binding get() = _binding
    private val leaveViewModel: LeaveViewModel by viewModels()

    private lateinit var dateFilterAdapter: FilterAdapter


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
        setupDateFilterBar()

        leaveViewModel.leaveStatResponse.observe(
            viewLifecycleOwner,
            EventObserver { response ->

                if (response?.meta?.status == true) {

                    selfAttendanceStats(response.self_count!!)

                    if (currentFilter == AttendanceFilter.WEEK)
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

        binding?.appTopBar?.setOnBackClickListener {
            AppNavigator.moveBackToPreviousFragment()

        }


        leaveViewModel.getUpcomingLeaveDetail(UpcomingLeaveInput().apply {
            val formatter = SimpleDateFormat(AppDateFormats.SERVER_DATE, Locale.getDefault())
            val tomorrow = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, 1)
            }
            val lastDayOfYear = Calendar.getInstance().apply {
                set(Calendar.MONTH, Calendar.DECEMBER)
                set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
            }
            val formattedTomorrow = formatter.format(tomorrow.time)
            val formattedLastDayOfYear = formatter.format(lastDayOfYear.time)

            Log.d("Upcoming Leaves date", "dates:" + formattedLastDayOfYear + formattedTomorrow)
            this.start_date = formattedTomorrow
            this.end_date = formattedLastDayOfYear
        })
        leaveViewModel.upcomingLeavesResponse.observe(
            viewLifecycleOwner,
            EventObserver { response ->
                if (response?.meta?.status == true) {
                    if (response.upcoming_leaves?.size ?: 0 > 0) {
                        binding?.tvMyShift?.isVisible = true
                        upcomingLeavePopulate(response.upcoming_leaves)
                    } else {
                        binding?.tvMyShift?.isVisible = false
                        binding?.rvUpcomingLeaves?.isVisible = false
                    }
                } else {
                    requireContext().showErrorMsg(response?.meta?.message.toString())
                }
            })
    }

    private fun setupDateFilterBar() {
        val items = listOf(
            FilterItem(getString(R.string.last_seven), 0),
            FilterItem(getString(R.string.this_month), 1)
        )
        dateFilterAdapter = FilterAdapter(items, selectedPosition = 0) { position, _ ->
            currentFilter = if (position == 0) AttendanceFilter.WEEK else AttendanceFilter.MONTH
            dateFilterAdapter.setSelected(position)
            leaveViewModel.getLeaveStats(getCurrentObject())
        }
        binding?.rvDateFilters?.apply {
            layoutManager = GridLayoutManager(requireContext(), items.size)
            adapter = dateFilterAdapter
        }
    }


    private fun remainingLeaveDataPopulate(leaves: ArrayList<LeaveType>) {
        binding?.rvRemaining?.layoutManager = GridLayoutManager(requireContext(), 2)
        val modulesAdapter = RemainingLeaveAdapter(
            leaves
        )
        binding?.rvRemaining?.adapter = modulesAdapter
        binding?.rvRemaining?.isNestedScrollingEnabled = false


    }


    private fun upcomingLeavePopulate(upcomingLeaves: ArrayList<UpcomingLeaves>?) {
        Log.d("upcomingLeaves", "" + upcomingLeaves)
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
        binding?.tvSelfTotalValue?.text = selfStats.total_leave.toString()
        binding?.tvSelfPendingValue?.text = selfStats.self_pending.toString()
        binding?.tvSelfApprovedValue?.text = selfStats.self_approved.toString()
        binding?.tvSelfRejectedValue?.text = selfStats.self_reject.toString()
        binding?.tvSelfRemainingValue?.text = selfStats.remaining_leave.toString()
    }
    private fun teamAttendanceStats(teamLeaveStats: TeamLeaveStats) {
        binding?.tvTeamTotalValue?.text = teamLeaveStats.total_team_members.toString()
        binding?.tvTeamPresentValue?.text = teamLeaveStats.all_leaves.toString()
        binding?.tvTeamApprovedValue?.text = teamLeaveStats.team_approved.toString()
        binding?.tvTeamRejectedValue?.text = teamLeaveStats.team_reject.toString()
        binding?.tvTeamPendingValue?.text = teamLeaveStats.team_pending.toString()

        updateTeamPresentProgress(teamLeaveStats)
    }

    private fun updateTeamPresentProgress(teamLeaveStats: TeamLeaveStats) {
        val total = (teamLeaveStats.total_team_members as? Number)?.toInt() ?: 0
        val present = (teamLeaveStats.all_leaves as? Number)?.toInt() ?: 0
        val percent = if (total > 0) present.toFloat() / total.toFloat() else 0f

        val progressView = binding?.progressTeamPresent ?: return
        (progressView.parent as? View)?.let { track ->
            track.viewTreeObserver.addOnGlobalLayoutListener(object :
                android.view.ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    track.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    val params = progressView.layoutParams
                    params.width = (track.width * percent).toInt()
                    progressView.layoutParams = params
                }
            })
        }

        binding?.tvTeamPresentSummary?.text = "$present of $total present"
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
            AttendanceFilter.Custom -> {}

        }
        return AttendanceInput().apply {
            this.startDate = localStart
            this.endDate = localEnd
            this.filter = currentFilter

        }

    }

}
