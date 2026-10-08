package com.axelliant.hris.screens

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.IdRes
import androidx.core.util.Pair
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
import com.axelliant.hris.event.EventObserver
import com.axelliant.hris.extention.showErrorMsg
import com.axelliant.hris.model.dashboard.FilterModel
import com.axelliant.hris.model.leaveatt.AttendanceDay
import com.axelliant.hris.model.leaveatt.ShiftDetail
import com.axelliant.hris.network.observeResult
import com.axelliant.hris.ui.WsDetailSheet
import com.axelliant.hris.ui.WsFormat
import com.axelliant.hris.ui.WsStatus
import com.axelliant.hris.ui.WsTone
import com.axelliant.hris.viewmodel.AttendanceViewModel
import com.google.android.material.datepicker.MaterialDatePicker
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/**
 * Attendance screen (iOS "Attendance"): the shift card, a period selector with a date line, status
 * chips and one card per day. A day opens its details in a [WsDetailSheet].
 */
class MyAttendanceDetailFragment : BaseFragment() {

    private enum class Period { WEEK, MONTH, CUSTOM }

    private var _binding: FragmentMyAttendanceDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AttendanceViewModel by viewModel()

    private val dayAdapter = AttDayAdapter { showDetails(it) }

    private var employeeId = ""
    private var period = Period.WEEK
    private var startDate = ""
    private var endDate = ""
    private var filterId = ""
    private var chips: List<FilterModel> = emptyList()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMyAttendanceDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        employeeId = arguments?.getString(AppConst.KEY_ID)?.takeIf { it.isNotBlank() } ?: GlobalConfig.currentEmployeeId()

        binding.attRvDays.layoutManager = LinearLayoutManager(requireContext())
        binding.attRvDays.adapter = dayAdapter
        binding.attRvFilters.layoutManager = LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL, false)

        binding.attSwipe.setColorSchemeResources(R.color.ws_primary)
        binding.attSwipe.setOnRefreshListener {
            viewModel.loadShift()
            load()
        }

        binding.attBtnWeek.setOnClickListener { selectPeriod(Period.WEEK) }
        binding.attBtnMonth.setOnClickListener { selectPeriod(Period.MONTH) }
        binding.attBtnCustom.setOnClickListener { pickRange() }

        observeViewModel()

        computeDates(Period.WEEK)
        renderRange()
        viewModel.loadShift()
        load()
    }

    private fun observeViewModel() {
        viewModel.getIsLoading().observe(viewLifecycleOwner, EventObserver { loading ->
            if (loading) showDialog() else hideDialog()
        })

        // The shift card is optional: a failed call just leaves it hidden.
        viewModel.shift.observeResult(viewLifecycleOwner) { data, _ ->
            data.shift_detail?.let { showShift(it) }
        }

        viewModel.attendance.observeResult(viewLifecycleOwner, onError = { message ->
            binding.attSwipe.isRefreshing = false
            requireContext().showErrorMsg(message, "Attendance")
        }) { data, _ ->
            binding.attSwipe.isRefreshing = false
            val days = data.attendance_data.orEmpty().sortedByDescending { WsFormat.parse(it.date)?.time ?: 0L }
            dayAdapter.submitList(days)
            if (days.isEmpty()) binding.attEmpty.show() else binding.attEmpty.hide()
            showChips(data.attendance_status.orEmpty())
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Period and chips
    // ---------------------------------------------------------------------------------------------

    private fun selectPeriod(newPeriod: Period) {
        if (newPeriod == period) return
        computeDates(newPeriod)
        renderRange()
        load()
    }

    /** Last 7 days = today-7 .. today; this month = first .. last day. Custom keeps its picked dates. */
    private fun computeDates(newPeriod: Period) {
        period = newPeriod
        val cal = Calendar.getInstance()
        when (newPeriod) {
            Period.WEEK -> {
                endDate = WsFormat.apiDay(cal)
                cal.add(Calendar.DAY_OF_YEAR, -7)
                startDate = WsFormat.apiDay(cal)
            }
            Period.MONTH -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                startDate = WsFormat.apiDay(cal)
                cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
                endDate = WsFormat.apiDay(cal)
            }
            Period.CUSTOM -> Unit
        }
    }

    private fun renderRange() {
        binding.attTvRange.text = getString(R.string.att_date_range, WsFormat.reversedDay(startDate), WsFormat.reversedDay(endDate))
    }

    @IdRes
    private fun buttonFor(p: Period): Int = when (p) {
        Period.WEEK -> R.id.att_btn_week
        Period.MONTH -> R.id.att_btn_month
        Period.CUSTOM -> R.id.att_btn_custom
    }

    private fun pickRange() {
        val previous = period
        val builder = MaterialDatePicker.Builder.dateRangePicker().setTitleText(R.string.att_range_picker_title)
        if (period == Period.CUSTOM && startDate.isNotEmpty() && endDate.isNotEmpty()) {
            builder.setSelection(Pair(dayToUtcMillis(startDate), dayToUtcMillis(endDate)))
        }
        val picker = builder.build()
        var applied = false
        picker.addOnPositiveButtonClickListener { selection ->
            val first = selection.first ?: return@addOnPositiveButtonClickListener
            val second = selection.second ?: first
            applied = true
            period = Period.CUSTOM
            startDate = utcMillisToDay(first)
            endDate = utcMillisToDay(second)
            renderRange()
            load()
        }
        // A cancelled picker puts the selector back on the period that is still showing.
        picker.addOnDismissListener {
            if (!applied && _binding != null) binding.attSegment.check(buttonFor(previous))
        }
        picker.show(childFragmentManager, "attendanceRange")
    }

    private fun showChips(server: List<FilterModel>) {
        if (server.isNotEmpty() || chips.isEmpty()) {
            val all = FilterModel().apply {
                id = ""
                title = getString(R.string.att_filter_all)
                count = "0"
            }
            chips = listOf(all) + server
        }
        val layoutManager = binding.attRvFilters.layoutManager
        // Rebuilding the adapter would otherwise jump the chip row back to the start.
        val scrollState = layoutManager?.onSaveInstanceState()
        binding.attRvFilters.adapter = SubFilterAdapter(filterId, chips, requireContext(), object : AdapterItemClick {
            override fun onItemClick(customObject: Any, position: Int) {
                filterId = (customObject as FilterModel).id.orEmpty()
                load()
            }
        })
        layoutManager?.onRestoreInstanceState(scrollState)
        binding.attRvFilters.visibility = if (chips.size > 1) View.VISIBLE else View.GONE
    }

    private fun load() {
        if (startDate.isEmpty() || endDate.isEmpty()) return
        viewModel.loadAttendance(startDate, endDate, employeeId, filterId, showLoading = !binding.attSwipe.isRefreshing)
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
        binding.attSwipe.isRefreshing = false
        _binding = null
        super.onDestroyView()
    }

    private companion object {
        private fun dayFormat() = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }

        /** Material date pickers speak UTC midnight. */
        fun dayToUtcMillis(day: String): Long = dayFormat().parse(day)?.time ?: MaterialDatePicker.todayInUtcMilliseconds()

        fun utcMillisToDay(millis: Long): String = dayFormat().format(millis)
    }
}
