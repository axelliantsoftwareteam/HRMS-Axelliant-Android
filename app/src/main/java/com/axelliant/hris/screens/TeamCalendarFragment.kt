package com.axelliant.hris.screens

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.axelliant.hris.R
import com.axelliant.hris.adapter.CalendarRow
import com.axelliant.hris.adapter.TeamCalendarAdapter
import com.axelliant.hris.base.BaseFragment
import com.axelliant.hris.databinding.CalItemLegendBinding
import com.axelliant.hris.databinding.FragmentTeamCalendarBinding
import com.axelliant.hris.event.EventObserver
import com.axelliant.hris.extention.showErrorMsg
import com.axelliant.hris.model.team.CalendarStatus
import com.axelliant.hris.model.team.TeamCalendarDay
import com.axelliant.hris.model.team.TeamCalendarMember
import com.axelliant.hris.model.team.TeamNum
import com.axelliant.hris.ui.WsDetailSheet
import com.axelliant.hris.ui.WsFormat
import com.axelliant.hris.ui.WsStatus
import com.axelliant.hris.viewmodel.TeamCalendarViewModel
import com.intuit.sdp.R as SdpR
import org.koin.androidx.viewmodel.ext.android.viewModel

/** Manager's Team Calendar: month header, status legend and one card per member with a month grid. */
class TeamCalendarFragment : BaseFragment() {

    private var _binding: FragmentTeamCalendarBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TeamCalendarViewModel by viewModel()

    /** Members whose month grid is open. */
    private val expanded = HashSet<String>()

    private val adapter by lazy {
        TeamCalendarAdapter(
            onToggle = { key ->
                if (!expanded.add(key)) expanded.remove(key)
                render()
            },
            onDay = ::showDay
        )
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentTeamCalendarBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.calList.adapter = adapter
        binding.calList.itemAnimator = null
        binding.calRefresh.setOnChildScrollUpCallback { _, _ -> _binding?.calList?.canScrollVertically(-1) ?: false }
        binding.calRefresh.setOnRefreshListener {
            viewModel.load { _binding?.calRefresh?.isRefreshing = false }
        }

        binding.calBtnPrev.setOnClickListener { viewModel.shiftMonth(-1) }
        binding.calBtnNext.setOnClickListener { viewModel.shiftMonth(1) }
        binding.calBtnReset.setOnClickListener { viewModel.resetFilters() }
        binding.toolbar.setOnActionClickListener {
            TeamCalendarFilterSheet.show(
                childFragmentManager,
                viewModel.startDate,
                viewModel.endDate,
                viewModel.appliedEmployee,
                viewModel.appliedDesignation
            )
        }
        childFragmentManager.setFragmentResultListener(TeamCalendarFilterSheet.RESULT_KEY, viewLifecycleOwner) { _, b ->
            if (b.getBoolean(TeamCalendarFilterSheet.EXTRA_RESET)) {
                viewModel.resetFilters()
            } else {
                viewModel.applyFilters(
                    b.getString(TeamCalendarFilterSheet.EXTRA_START).orEmpty(),
                    b.getString(TeamCalendarFilterSheet.EXTRA_END).orEmpty(),
                    b.getString(TeamCalendarFilterSheet.EXTRA_EMPLOYEE).orEmpty(),
                    b.getString(TeamCalendarFilterSheet.EXTRA_DESIGNATION).orEmpty()
                )
            }
        }

        viewModel.getIsLoading().observe(viewLifecycleOwner, EventObserver { loading ->
            if (loading) showDialog() else hideDialog()
        })
        viewModel.error.observe(viewLifecycleOwner, EventObserver { requireContext().showErrorMsg(it) })
        viewModel.changed.observe(viewLifecycleOwner) { render() }

        render()
        if (!viewModel.hasLoaded) viewModel.load()
    }

    private fun render() {
        val b = _binding ?: return
        b.toolbar.setActionDotVisible(viewModel.hasActiveFilters)
        b.calRangeTitle.text = viewModel.rangeTitle
        b.calDaysLabel.text = getString(R.string.cal_days_view, viewModel.dates.size)
        val visible = viewModel.visibleMembers
        b.calMembersPill.text = getString(R.string.cal_members_count, visible.size, viewModel.members.size)
        b.calBtnReset.visibility = if (viewModel.hasActiveFilters) View.VISIBLE else View.GONE
        renderLegend()

        val rows = visible.mapIndexed { index, m ->
            val key = m.employee_id?.takeIf { it.isNotEmpty() } ?: "member-$index"
            CalendarRow(
                key = key,
                member = m,
                counts = viewModel.counts(m),
                expanded = key in expanded,
                highlighted = viewModel.highlighted
            )
        }
        adapter.submitList(rows)

        when {
            viewModel.hasLoaded && viewModel.members.isEmpty() -> b.calEmptyState.show(
                R.drawable.ws_ic_person, getString(R.string.cal_empty_title), getString(R.string.cal_empty_message)
            )
            viewModel.hasLoaded && visible.isEmpty() -> b.calEmptyState.show(
                R.drawable.ws_ic_filter, getString(R.string.cal_no_matches_title), getString(R.string.cal_no_matches_message)
            )
            else -> b.calEmptyState.hide()
        }
    }

    private fun renderLegend() {
        val b = _binding ?: return
        val ctx = requireContext()
        val density = ctx.resources.displayMetrics.density
        val hairline = maxOf(1, density.toInt())
        val thick = maxOf(2, (1.5f * density).toInt())
        val chipRadius = ctx.resources.getDimensionPixelSize(SdpR.dimen._6sdp).toFloat()
        val swatchRadius = ctx.resources.getDimensionPixelSize(SdpR.dimen._2sdp).toFloat()
        val inflater = LayoutInflater.from(ctx)
        b.calLegend.removeAllViews()
        CalendarStatus.values().forEach { status ->
            val color = ContextCompat.getColor(ctx, status.colorRes)
            val isOn = status in viewModel.highlighted
            val item = CalItemLegendBinding.inflate(inflater, b.calLegend, false)
            item.calLegendLabel.text = getString(status.labelRes)
            item.calLegendLabel.setTextColor(color)
            item.calLegendSwatch.background = GradientDrawable().apply {
                cornerRadius = swatchRadius
                setColor(if (status == CalendarStatus.NO_RECORD) Color.TRANSPARENT else color)
                setStroke(hairline, WsStatus.tint(color, 50))
            }
            item.root.background = GradientDrawable().apply {
                cornerRadius = chipRadius
                setColor(
                    when {
                        isOn -> WsStatus.tint(color, 25)
                        status == CalendarStatus.NO_RECORD -> WsStatus.tint(color, 8)
                        else -> WsStatus.tint(color, 16)
                    }
                )
                if (isOn) setStroke(thick, color) else setStroke(hairline, WsStatus.tint(color, 20))
            }
            item.root.setOnClickListener { viewModel.toggleHighlight(status) }
            b.calLegend.addView(item.root)
        }
    }

    private fun showDay(member: TeamCalendarMember, day: TeamCalendarDay) {
        val ctx = requireContext()
        val status = CalendarStatus.of(day)
        fun clock(iso: String?, raw: String?): String =
            WsFormat.time(ctx, iso).ifBlank { WsFormat.clockText(ctx, raw) }
        val hours = TeamNum.num(day.working_hours)
        WsDetailSheet.show(
            childFragmentManager,
            title = WsFormat.date(day.date).ifBlank { day.date.orEmpty() },
            subtitle = member.employee_name,
            status = day.label?.takeIf { it.isNotEmpty() } ?: getString(status.labelRes),
            fields = listOf(
                WsDetailSheet.Field(getString(R.string.team_designation), member.designation),
                WsDetailSheet.Field(getString(R.string.cal_field_shift), day.shift),
                WsDetailSheet.Field(getString(R.string.cal_field_shift_timings), day.shift_timings),
                WsDetailSheet.Field(getString(R.string.cal_field_checked_in), clock(day.in_time_iso, day.in_time)),
                WsDetailSheet.Field(getString(R.string.cal_field_checked_out), clock(day.out_time_iso, day.out_time)),
                WsDetailSheet.Field(getString(R.string.cal_field_working_hours), WsFormat.hours(hours)),
                WsDetailSheet.Field(getString(R.string.team_field_location), day.location)
            )
        )
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
