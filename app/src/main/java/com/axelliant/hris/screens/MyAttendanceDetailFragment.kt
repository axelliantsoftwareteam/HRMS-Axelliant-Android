package com.axelliant.hris.screens

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.IdRes
import androidx.core.util.Pair
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.axelliant.hris.R
import com.axelliant.hris.adapter.AttDayAdapter
import com.axelliant.hris.adapter.AttFormat
import com.axelliant.hris.adapter.SubFilterAdapter
import com.axelliant.hris.base.BaseFragment
import com.axelliant.hris.callback.AdapterItemClick
import com.axelliant.hris.config.AppConst
import com.axelliant.hris.config.GlobalConfig
import com.axelliant.hris.databinding.FragmentMyAttendanceDetailBinding
import com.axelliant.hris.loading.ListLoadKind
import com.axelliant.hris.model.dashboard.FilterModel
import com.axelliant.hris.model.leaveatt.AttendanceDay
import com.axelliant.hris.model.leaveatt.ShiftDetail
import com.axelliant.hris.ui.WsDetailSheet
import com.axelliant.hris.ui.WsFormat
import com.axelliant.hris.ui.WsListLoading
import com.axelliant.hris.ui.WsStatus
import com.axelliant.hris.ui.WsTone
import com.axelliant.hris.viewmodel.AttendanceViewModel
import com.axelliant.hris.viewmodel.AttendanceViewModel.Period
import com.google.android.material.datepicker.MaterialDatePicker
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/** Attendance list with retained selection/content and independent optional shift loading. */
class MyAttendanceDetailFragment : BaseFragment() {
    private var _binding: FragmentMyAttendanceDetailBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AttendanceViewModel by viewModel()
    private val dayAdapter = AttDayAdapter { showDetails(it) }
    private var loading: WsListLoading? = null
    private var chips: List<FilterModel> = emptyList()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMyAttendanceDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.initialize(arguments?.getString(AppConst.KEY_ID)?.takeIf { it.isNotBlank() } ?: GlobalConfig.currentEmployeeId())
        dayAdapter.stateRestorationPolicy = RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
        binding.attRvDays.layoutManager = LinearLayoutManager(requireContext())
        binding.attRvDays.adapter = dayAdapter
        binding.attRvFilters.layoutManager = LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL, false)
        loading = WsListLoading(viewLifecycleOwner, binding.attListHost, { viewModel.reload() },
            emptyTitle = getString(R.string.att_empty_title), emptyMessage = getString(R.string.att_empty_message))
            .also { it.bind(viewModel.listLoading()) }
        binding.attSwipe.setColorSchemeResources(R.color.ws_primary)
        binding.attSwipe.setOnRefreshListener {
            viewModel.loadShift(force = true)
            viewModel.reload(ListLoadKind.PULL_REFRESH)
        }
        binding.attBtnWeek.setOnClickListener { viewModel.selectPeriod(Period.WEEK) }
        binding.attBtnMonth.setOnClickListener { viewModel.selectPeriod(Period.MONTH) }
        binding.attBtnCustom.setOnClickListener { pickRange() }
        viewModel.selection.observe(viewLifecycleOwner) {
            renderRange()
            showChips(viewModel.attendance.value?.attendance_status.orEmpty())
        }
        viewModel.attendance.observe(viewLifecycleOwner) { data ->
            dayAdapter.submitList(data?.attendance_data.orEmpty())
            showChips(data?.attendance_status.orEmpty())
            renderRange()
        }
        viewModel.shift.observe(viewLifecycleOwner) { shift ->
            if (shift == null) binding.attCardShift.visibility = View.GONE else showShift(shift)
        }
        viewModel.listLoading().observe(viewLifecycleOwner) { state ->
            binding.attSwipe.isRefreshing = state.pullRefreshing
            renderRange()
        }
        @Suppress("UNCHECKED_CAST")
        val restored = childFragmentManager.findFragmentByTag("attendanceRange") as? MaterialDatePicker<Pair<Long, Long>>
        restored?.let(::connectPicker)
        viewModel.ensureLoaded()
    }

    private fun renderRange() {
        val selected = viewModel.selection.value ?: return
        binding.attSegment.check(buttonFor(selected.period))
        val dates = getString(R.string.att_date_range, WsFormat.reversedDay(selected.from), WsFormat.reversedDay(selected.to))
        val previous = viewModel.displayedSelection?.let { it != selected } == true
        binding.attTvRange.text = if (previous) dates + "\n" + getString(R.string.list_previous_results) else dates
    }

    @IdRes private fun buttonFor(period: Period): Int = when (period) {
        Period.WEEK -> R.id.att_btn_week
        Period.MONTH -> R.id.att_btn_month
        Period.CUSTOM -> R.id.att_btn_custom
    }

    private fun pickRange() {
        if (childFragmentManager.isStateSaved || childFragmentManager.findFragmentByTag("attendanceRange") != null) return
        val selected = viewModel.selection.value ?: return
        val picker = MaterialDatePicker.Builder.dateRangePicker().setTitleText(R.string.att_range_picker_title)
            .setSelection(Pair(dayToUtcMillis(selected.from), dayToUtcMillis(selected.to))).build()
        connectPicker(picker)
        picker.show(childFragmentManager, "attendanceRange")
    }

    private fun connectPicker(picker: MaterialDatePicker<Pair<Long, Long>>) {
        val owner = viewLifecycleOwner
        picker.addOnPositiveButtonClickListener { range ->
            if (owner.lifecycle.currentState == Lifecycle.State.DESTROYED) return@addOnPositiveButtonClickListener
            val first = range.first ?: return@addOnPositiveButtonClickListener
            val last = range.second ?: return@addOnPositiveButtonClickListener
            viewModel.setCustomRange(first, last)
        }
        picker.addOnDismissListener {
            if (owner.lifecycle.currentState != Lifecycle.State.DESTROYED) renderRange()
        }
    }

    private fun showChips(server: List<FilterModel>) {
        if (server.isNotEmpty() || chips.isEmpty()) {
            val all = FilterModel().apply { id = ""; title = getString(R.string.att_filter_all); count = "0" }
            chips = listOf(all) + server
        }
        val layoutManager = binding.attRvFilters.layoutManager
        val scrollState = layoutManager?.onSaveInstanceState()
        binding.attRvFilters.adapter = SubFilterAdapter(viewModel.selection.value?.filter.orEmpty(), chips,
            requireContext(), object : AdapterItemClick {
                override fun onItemClick(customObject: Any, position: Int) {
                    viewModel.selectFilter((customObject as FilterModel).id.orEmpty())
                }
            }).apply { stateRestorationPolicy = RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY }
        layoutManager?.onRestoreInstanceState(scrollState)
        binding.attRvFilters.visibility = if (chips.size > 1) View.VISIBLE else View.GONE
    }

    // ---------------------------------------------------------------------------------------------
    // Shift card and details sheet
    // ---------------------------------------------------------------------------------------------

    private fun showShift(shift: ShiftDetail) {
        val ctx = requireContext()
        val placeholder = getString(R.string.att_shift_placeholder)
        val start = shift.actual_start?.takeIf { it.isNotBlank() }?.let { WsFormat.clockText(ctx, it) } ?: placeholder
        val end = shift.actual_end?.takeIf { it.isNotBlank() }?.let { WsFormat.clockText(ctx, it) } ?: placeholder
        val times = getString(R.string.att_shift_times, start, end)
        val place = shift.location?.takeIf { it.isNotBlank() }

        binding.attCardShift.visibility = View.VISIBLE
        WsStatus.iconBadge(binding.attShiftIcon, WsTone.attendance(ctx))
        binding.attShiftName.text = shift.name?.takeIf { it.isNotBlank() } ?: getString(R.string.att_shift_not_assigned)
        binding.attShiftDetails.text = if (place == null) times else times + getString(R.string.att_shift_separator) + place
    }

    private fun showDetails(day: AttendanceDay) {
        val ctx = requireContext()
        val fields = listOf(
            WsDetailSheet.Field(getString(R.string.att_field_location), day.custom_attendance_status, half = true),
            WsDetailSheet.Field(getString(R.string.att_field_working_hours), AttFormat.hours(ctx, day), half = true),
            WsDetailSheet.Field(getString(R.string.att_field_shift), day.shift, half = true),
            WsDetailSheet.Field(getString(R.string.att_field_shift_timings), day.shift_timings, half = true),
            WsDetailSheet.Field(getString(R.string.att_field_actual_in), AttFormat.timeText(ctx, day.in_time_iso, day.in_time), half = true),
            WsDetailSheet.Field(getString(R.string.att_field_expected_in), AttFormat.timeText(ctx, day.expected_in_iso, day.expected_in), half = true),
            WsDetailSheet.Field(getString(R.string.att_field_actual_out), AttFormat.timeText(ctx, day.out_time_iso, day.out_time), half = true),
            WsDetailSheet.Field(getString(R.string.att_field_expected_out), AttFormat.timeText(ctx, day.expected_out_iso, day.expected_out), half = true)
        )
        WsDetailSheet.show(
            childFragmentManager,
            title = AttFormat.sheetTitle(day.date),
            subtitle = getString(R.string.att_sheet_subtitle),
            status = day.shownStatus,
            fields = fields
        )
    }

    override fun onDestroyView() {
        loading?.release()
        loading = null
        binding.attSwipe.isRefreshing = false
        binding.attRvDays.adapter = null
        binding.attRvFilters.adapter = null
        _binding = null
        super.onDestroyView()
    }

    private companion object {
        fun dayToUtcMillis(day: String): Long = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            .apply { timeZone = TimeZone.getTimeZone("UTC") }.parse(day)?.time ?: MaterialDatePicker.todayInUtcMilliseconds()
    }
}