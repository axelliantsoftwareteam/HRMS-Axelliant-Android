package com.axelliant.hris.screens

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.axelliant.hris.R
import com.axelliant.hris.adapter.TeamActivityAdapter
import com.axelliant.hris.adapter.TeamRow
import com.axelliant.hris.base.BaseFragment
import com.axelliant.hris.databinding.FragmentTeamActivityBinding
import com.axelliant.hris.event.EventObserver
import com.axelliant.hris.extention.showErrorMsg
import com.axelliant.hris.model.team.TeamArrival
import com.axelliant.hris.model.team.TeamMember
import com.axelliant.hris.ui.WsDetailSheet
import com.axelliant.hris.ui.WsFormat
import com.axelliant.hris.ui.WsTone
import com.axelliant.hris.viewmodel.TeamActivityViewModel
import com.axelliant.hris.viewmodel.TeamSegment
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.util.Locale

/** Manager's Team Activity: arrivals, working hours and leave quota of the team, with a filter sheet. */
class TeamActivityFragment : BaseFragment() {

    private var _binding: FragmentTeamActivityBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TeamActivityViewModel by viewModel()

    private var expandedQuota: String? = null

    private val adapter by lazy {
        TeamActivityAdapter(
            onArrival = ::showArrival,
            onMember = ::showMember,
            onQuota = { key ->
                expandedQuota = if (expandedQuota == key) null else key
                render()
            }
        )
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentTeamActivityBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.teamList.adapter = adapter
        binding.teamList.itemAnimator = null
        binding.teamRefresh.setOnChildScrollUpCallback { _, _ -> _binding?.teamList?.canScrollVertically(-1) ?: false }
        binding.teamRefresh.setOnRefreshListener {
            viewModel.load { _binding?.teamRefresh?.isRefreshing = false }
        }
        binding.teamBtnRetry.setOnClickListener { viewModel.load() }

        binding.toolbar.setOnActionClickListener {
            TeamActivityFilterSheet.show(childFragmentManager, viewModel.filters)
        }
        childFragmentManager.setFragmentResultListener(TeamActivityFilterSheet.RESULT_KEY, viewLifecycleOwner) { _, bundle ->
            viewModel.applyFilters(TeamActivityFilterSheet.parse(bundle))
        }

        binding.teamSegments.check(segmentButton(viewModel.segment))
        binding.teamSegments.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                viewModel.segment = when (checkedId) {
                    R.id.team_seg_hours -> TeamSegment.HOURS
                    R.id.team_seg_quota -> TeamSegment.QUOTA
                    else -> TeamSegment.ARRIVALS
                }
                viewModel.notifyChanged()
            }
        }

        binding.teamSearchInput.setText(viewModel.quotaSearch)
        binding.teamSearchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                val text = s?.toString().orEmpty()
                binding.teamSearchClear.visibility = if (text.isEmpty()) View.GONE else View.VISIBLE
                if (text != viewModel.quotaSearch) {
                    viewModel.quotaSearch = text
                    viewModel.notifyChanged()
                }
            }
        })
        binding.teamSearchClear.setOnClickListener { binding.teamSearchInput.setText("") }

        viewModel.getIsLoading().observe(viewLifecycleOwner, EventObserver { loading ->
            if (loading) showDialog() else hideDialog()
        })
        viewModel.error.observe(viewLifecycleOwner, EventObserver { requireContext().showErrorMsg(it) })
        viewModel.changed.observe(viewLifecycleOwner) { render() }

        if (!viewModel.hasLoaded) viewModel.load() else render()
    }

    private fun segmentButton(segment: TeamSegment): Int = when (segment) {
        TeamSegment.ARRIVALS -> R.id.team_seg_arrivals
        TeamSegment.HOURS -> R.id.team_seg_hours
        TeamSegment.QUOTA -> R.id.team_seg_quota
    }

    // ---- rendering ----

    private fun render() {
        val b = _binding ?: return
        val segment = viewModel.segment
        b.toolbar.setActionVisible(segment != TeamSegment.QUOTA)
        b.toolbar.setActionDotVisible(viewModel.hasActiveFilters)
        b.teamSearchCard.visibility = if (segment == TeamSegment.QUOTA) View.VISIBLE else View.GONE
        b.teamErrorBlock.visibility = if (viewModel.loadFailed) View.VISIBLE else View.GONE

        val rows: List<TeamRow>
        val title: String
        when (segment) {
            TeamSegment.ARRIVALS -> {
                val list = viewModel.filteredArrivals
                rows = list.mapIndexed { i, a -> arrivalRow(a, i) }
                title = getString(R.string.team_title_arrivals, list.size)
            }
            TeamSegment.HOURS -> {
                val list = viewModel.filteredMembers
                rows = list.map { memberRow(it) }
                title = getString(R.string.team_title_hours, list.size)
            }
            TeamSegment.QUOTA -> {
                val list = viewModel.filteredQuota
                rows = list.mapIndexed { i, e -> quotaRow(e, i) }
                title = getString(R.string.team_title_quota, list.size)
            }
        }
        b.teamListTitle.text = title
        adapter.submitList(rows)

        if (viewModel.hasLoaded && rows.isEmpty() && !viewModel.loadFailed) {
            when (segment) {
                TeamSegment.ARRIVALS -> b.teamEmptyState.show(
                    R.drawable.ws_ic_person, getString(R.string.team_empty_arrivals_title), getString(R.string.team_empty_arrivals_message)
                )
                TeamSegment.HOURS -> b.teamEmptyState.show(
                    R.drawable.ws_ic_schedule, getString(R.string.team_empty_hours_title), getString(R.string.team_empty_hours_message)
                )
                TeamSegment.QUOTA -> {
                    val none = viewModel.balances.isEmpty()
                    b.teamEmptyState.show(
                        R.drawable.ws_ic_calendar,
                        getString(if (none) R.string.team_empty_quota_title else R.string.team_empty_search_title),
                        getString(if (none) R.string.team_empty_quota_message else R.string.team_empty_search_message)
                    )
                }
            }
        } else {
            b.teamEmptyState.hide()
        }
    }

    private fun arrivalRow(a: TeamArrival, index: Int): TeamRow.Arrival {
        val ctx = requireContext()
        val time = WsFormat.dateTime(ctx, a.time_iso ?: a.date).ifBlank { getString(R.string.team_time_unavailable) }
        return TeamRow.Arrival(
            key = "${a.employee ?: a.employee_name}|${a.date ?: a.time_iso}|${a.location}|$index",
            source = a,
            name = viewModel.displayName(a),
            designation = viewModel.designation(a).orEmpty(),
            time = time,
            location = a.location?.takeIf { it.isNotBlank() } ?: getString(R.string.team_location_recorded)
        )
    }

    private fun memberRow(m: TeamMember): TeamRow.Member {
        val ctx = requireContext()
        val red = ContextCompat.getColor(ctx, R.color.ws_red)
        val amber = ContextCompat.getColor(ctx, R.color.ws_amber)
        val late = ContextCompat.getColor(ctx, R.color.ws_bittersweet)
        val early = ContextCompat.getColor(ctx, R.color.ws_melrose)
        val holiday = ContextCompat.getColor(ctx, R.color.ws_secondary)
        val balance = viewModel.balanceFor(m)?.let {
            getString(R.string.team_leave_remaining, String.format(Locale.US, "%.1f", it.totalAvailable))
        }
        return TeamRow.Member(
            key = m.identity,
            source = m,
            name = viewModel.displayName(m),
            designation = m.designation?.takeIf { it.isNotBlank() } ?: "—",
            total = m.metric("Total Hrs"),
            avg = m.metric("Avg Hrs"),
            present = m.metric("Present"),
            chips = listOf(
                TeamRow.Chip(getString(R.string.team_chip_absent, m.metric("Absent")), red),
                TeamRow.Chip(getString(R.string.team_chip_leave, m.metric("Leave")), amber),
                TeamRow.Chip(getString(R.string.team_chip_late, m.metric("Lateness")), late),
                TeamRow.Chip(getString(R.string.team_chip_early, m.metric("Early Out")), early),
                TeamRow.Chip(getString(R.string.team_chip_holiday, m.metric("Holiday")), holiday)
            ),
            balance = balance
        )
    }

    private fun quotaRow(e: com.axelliant.hris.model.team.TeamQuotaEmployee, index: Int): TeamRow.Quota {
        val ctx = requireContext()
        val key = e.quotaId(index)
        val allocations = e.leaves?.leave_allocation.orEmpty()
        return TeamRow.Quota(
            key = key,
            image = e.image,
            name = e.employee_name?.takeIf { it.isNotBlank() } ?: getString(R.string.team_employee_fallback),
            designation = e.designation?.takeIf { it.isNotBlank() } ?: "--",
            isSelf = e.isSelf,
            available = getString(R.string.team_available, WsFormat.number(e.totalAvailable)),
            expanded = key == expandedQuota,
            tiles = allocations.mapIndexed { i, l ->
                TeamRow.Tile(
                    value = "${WsFormat.number(l.usedDays)}/${WsFormat.number(l.totalDays)}",
                    name = (l.name ?: "--").uppercase(),
                    detail = getString(R.string.team_tile_detail, WsFormat.number(l.pendingDays), WsFormat.number(l.availableDays)),
                    color = WsTone.cycle(ctx, i)
                )
            }
        )
    }

    // ---- detail sheets ----

    private fun showArrival(a: TeamArrival) {
        val name = viewModel.displayName(a)
        val time = WsFormat.dateTime(requireContext(), a.time_iso ?: a.date).ifBlank { getString(R.string.team_time_unavailable) }
        WsDetailSheet.show(
            childFragmentManager,
            title = name,
            subtitle = getString(R.string.team_arrival_details),
            fields = listOf(
                WsDetailSheet.Field(getString(R.string.team_field_employee), name),
                WsDetailSheet.Field(getString(R.string.team_designation), viewModel.designation(a)),
                WsDetailSheet.Field(getString(R.string.team_field_date_time), time),
                WsDetailSheet.Field(getString(R.string.team_field_location), a.location),
                WsDetailSheet.Field(getString(R.string.team_field_status), a.status)
            )
        )
    }

    private fun showMember(m: TeamMember) {
        val fields = mutableListOf(
            WsDetailSheet.Field(getString(R.string.team_metric_avg), m.metric("Avg Hrs"), half = true),
            WsDetailSheet.Field(getString(R.string.team_metric_present), m.metric("Present"), half = true),
            WsDetailSheet.Field(getString(R.string.team_metric_total), m.metric("Total Hrs"), half = true),
            WsDetailSheet.Field(getString(R.string.team_metric_absent), m.metric("Absent"), half = true),
            WsDetailSheet.Field(getString(R.string.team_metric_leave), m.metric("Leave"), half = true),
            WsDetailSheet.Field(getString(R.string.team_metric_late), m.metric("Lateness"), half = true),
            WsDetailSheet.Field(getString(R.string.team_metric_early), m.metric("Early Out"), half = true),
            WsDetailSheet.Field(getString(R.string.team_metric_holiday), m.metric("Holiday"), half = true),
            WsDetailSheet.Field(getString(R.string.team_metric_weekend), m.metric("Weekend"), half = true)
        )
        val balance = viewModel.balanceFor(m)
        if (balance != null) {
            fields += WsDetailSheet.Field(
                getString(R.string.team_total_remaining), String.format(Locale.US, "%.1f", balance.totalAvailable)
            )
            balance.leaves?.leave_allocation.orEmpty().forEach { l ->
                fields += WsDetailSheet.Field(
                    l.name ?: getString(R.string.team_leave_fallback),
                    getString(
                        R.string.team_allocation_summary,
                        String.format(Locale.US, "%.1f", l.totalDays),
                        String.format(Locale.US, "%.1f", l.usedDays),
                        String.format(Locale.US, "%.1f", l.pendingDays),
                        String.format(Locale.US, "%.1f", l.availableDays)
                    )
                )
            }
        } else {
            fields += WsDetailSheet.Field(getString(R.string.team_leave_fallback), getString(R.string.team_leave_unavailable))
        }
        WsDetailSheet.show(
            childFragmentManager,
            title = viewModel.displayName(m),
            subtitle = m.designation.orEmpty(),
            fields = fields
        )
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
