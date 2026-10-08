package com.axelliant.hris.screens

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.widget.doAfterTextChanged
import com.axelliant.hris.R
import com.axelliant.hris.base.BaseFragment
import com.axelliant.hris.config.AppConst
import com.axelliant.hris.config.GlobalConfig
import com.axelliant.hris.databinding.FragmentRequestBinding
import com.axelliant.hris.enums.RequestFilter
import com.axelliant.hris.event.EventObserver
import com.axelliant.hris.extention.showErrorMsg
import com.axelliant.hris.extention.showSuccessMsg
import com.axelliant.hris.model.leaveatt.AttendanceRequestRecord
import com.axelliant.hris.model.leaveatt.LeaveQuota
import com.axelliant.hris.model.leaveatt.LeaveRecord
import com.axelliant.hris.model.leaveatt.toNumber
import com.axelliant.hris.navigation.AppNavigator
import com.axelliant.hris.network.observeResult
import com.axelliant.hris.ui.WsFormat
import com.axelliant.hris.viewmodel.RequestViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.CompositeDateValidator
import com.google.android.material.datepicker.DateValidatorPointBackward
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.google.gson.Gson
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/**
 * The leave request and attendance request forms (iOS "Request"). One mode per entry:
 * `AppConst.RequestType` = `RequestFilter.LEAVE` or `ATTENDANCE`; edit mode is entered when the matching
 * `LeaveRequestParam` / `AttendanceRequestParam` JSON is in the arguments.
 */
class RequestFragment : BaseFragment() {

    private enum class Op { CREATE, UPDATE, DELETE }

    private var _binding: FragmentRequestBinding? = null
    private val binding get() = _binding!!

    private val viewModel: RequestViewModel by viewModel()

    private var mode = RequestFilter.LEAVE
    private var isUpdate = false
    private var recordId = ""
    private var lastOp = Op.CREATE

    // Leave form state
    private var quotas: List<LeaveQuota> = emptyList()
    private var leaveType: String? = null
    private var startDate: String? = null // yyyy-MM-dd
    private var endDate: String? = null
    private var halfDate: String? = null
    private var leaveDays = 0.0

    // Attendance form state
    private var attendanceType: String? = null
    private var location: String? = null
    private var attendanceDate: String? = null // yyyy-MM-dd
    private var attendanceTime: String? = null // HH:mm
    private var typeOptions: List<String> = emptyList()
    private var locationOptions: List<String> = emptyList()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentRequestBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        readArguments(savedInstanceState == null)
        setupChrome()
        observeViewModel()
        if (mode == RequestFilter.LEAVE) setupLeaveForm() else setupAttendanceForm()
    }

    // ---------------------------------------------------------------------------------------------
    // Entry: mode and edit prefill
    // ---------------------------------------------------------------------------------------------

    private fun readArguments(prefill: Boolean) {
        val args = arguments
        val leaveJson = args?.getString(AppConst.LeaveRequestParam)
        val attendanceJson = args?.getString(AppConst.AttendanceRequestParam)
        val type = args?.getString(AppConst.RequestType)
        mode = if (type == RequestFilter.ATTENDANCE.name || (type == null && leaveJson.isNullOrBlank() && !attendanceJson.isNullOrBlank())) {
            RequestFilter.ATTENDANCE
        } else {
            RequestFilter.LEAVE
        }
        if (!prefill) return

        val gson = Gson()
        if (mode == RequestFilter.LEAVE && !leaveJson.isNullOrBlank()) {
            val record = runCatching { gson.fromJson(leaveJson, LeaveRecord::class.java) }.getOrNull() ?: return
            isUpdate = true
            recordId = record.name.orEmpty()
            leaveType = record.leave_type
            startDate = record.from_date?.let { dayPart(it) }
            endDate = record.to_date?.let { dayPart(it) }
            binding.reqEtLeaveReason.setText(record.reason)
        } else if (mode == RequestFilter.ATTENDANCE && !attendanceJson.isNullOrBlank()) {
            val record = runCatching { gson.fromJson(attendanceJson, AttendanceRequestRecord::class.java) }.getOrNull() ?: return
            isUpdate = true
            recordId = record.name.orEmpty()
            attendanceType = record.log_type
            location = record.location
            attendanceDate = record.time?.let { dayPart(it) }
            attendanceTime = record.time?.let { timePart(it) }
            binding.reqEtReason.setText(record.reason)
        }
    }

    private fun setupChrome() {
        val leave = mode == RequestFilter.LEAVE
        binding.reqToolbar.title = getString(if (leave) R.string.req_title_leave else R.string.req_title_attendance)
        binding.reqTvHeader.setText(
            when {
                leave && isUpdate -> R.string.req_header_update_leave
                leave -> R.string.req_header_create_leave
                isUpdate -> R.string.req_header_update_attendance
                else -> R.string.req_header_create_attendance
            }
        )
        binding.reqGroupLeave.visibility = if (leave) View.VISIBLE else View.GONE
        binding.reqGroupAttendance.visibility = if (leave) View.GONE else View.VISIBLE
        binding.reqBtnApply.visibility = if (isUpdate) View.GONE else View.VISIBLE
        binding.reqGroupEditActions.visibility = if (isUpdate) View.VISIBLE else View.GONE

        binding.reqBtnApply.setOnClickListener { submit() }
        binding.reqBtnUpdate.setOnClickListener { submit() }
        binding.reqBtnDelete.setOnClickListener { confirmDelete() }
    }

    private fun observeViewModel() {
        viewModel.getIsLoading().observe(viewLifecycleOwner, EventObserver { loading ->
            if (loading) showDialog() else hideDialog()
        })

        viewModel.quotas.observeResult(viewLifecycleOwner, onError = ::showError) { data, _ ->
            quotas = data.allocations.filter { !it.name.isNullOrBlank() }
            when {
                // Edit keeps the request's own type; create starts on the first type, as the web form does.
                leaveType.isNullOrBlank() && quotas.isNotEmpty() -> selectLeaveType(quotas.first().name!!)
                !leaveType.isNullOrBlank() -> selectLeaveType(leaveType!!)
                else -> toast(R.string.req_no_leave_types)
            }
        }

        viewModel.options.observeResult(viewLifecycleOwner, onError = ::showError) { data, _ ->
            typeOptions = data.checkin.orEmpty().mapNotNull { it.type?.takeIf(String::isNotBlank) }
            locationOptions = data.location.orEmpty().mapNotNull { it.type?.takeIf(String::isNotBlank) }
            buildToggle(binding.reqToggleType, typeOptions, ::typeLabel, ::typeIcon, attendanceType) {
                attendanceType = it
                binding.reqErrType.visibility = View.GONE
            }
            buildToggle(binding.reqToggleLocation, locationOptions, { it }, ::locationIcon, location) {
                location = it
                binding.reqErrLocation.visibility = View.GONE
            }
            if (typeOptions.isEmpty() || locationOptions.isEmpty()) toast(R.string.req_no_options)
        }

        viewModel.leaveDays.observeResult(viewLifecycleOwner, onError = { showLeaveDays(0.0) }) { data, _ ->
            showLeaveDays(data.days.toNumber())
        }

        viewModel.action.observeResult(viewLifecycleOwner, onError = ::showError) { data, _ ->
            requireContext().showSuccessMsg(data.status_message?.takeIf { it.isNotBlank() } ?: getString(successText()))
            binding.reqBtnApply.isEnabled = false
            binding.reqBtnUpdate.isEnabled = false
            binding.reqBtnDelete.isEnabled = false
            Handler(Looper.getMainLooper()).postDelayed({
                if (_binding != null) AppNavigator.moveBackToPreviousFragment()
            }, POP_DELAY_MS)
        }
    }

    private fun showError(message: String) {
        requireContext().showErrorMsg(message, "Request")
    }

    private fun toast(@StringRes text: Int) {
        Toast.makeText(requireContext(), text, Toast.LENGTH_SHORT).show()
    }

    @StringRes
    private fun successText(): Int = when (mode) {
        RequestFilter.LEAVE -> when (lastOp) {
            Op.CREATE -> R.string.req_toast_leave_created
            Op.UPDATE -> R.string.req_toast_leave_updated
            Op.DELETE -> R.string.req_toast_leave_deleted
        }
        else -> when (lastOp) {
            Op.CREATE -> R.string.req_toast_attendance_created
            Op.UPDATE -> R.string.req_toast_attendance_updated
            Op.DELETE -> R.string.req_toast_attendance_deleted
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Leave form
    // ---------------------------------------------------------------------------------------------

    private fun setupLeaveForm() {
        with(binding) {
            // Leave type is fixed once a request exists.
            reqTilLeaveType.isEnabled = !isUpdate
            reqEtLeaveType.isEnabled = !isUpdate
            if (!isUpdate) {
                reqEtLeaveType.setOnClickListener { pickLeaveType() }
                reqTilLeaveType.setEndIconOnClickListener { pickLeaveType() }
            }

            reqEtStartDate.setOnClickListener { pickStartDate() }
            reqTilStartDate.setEndIconOnClickListener { pickStartDate() }
            reqEtEndDate.setOnClickListener { pickEndDate() }
            reqTilEndDate.setEndIconOnClickListener { pickEndDate() }
            reqEtHalfDate.setOnClickListener { pickHalfDate() }
            reqTilHalfDate.setEndIconOnClickListener { pickHalfDate() }

            reqCbHalfDay.setOnCheckedChangeListener { _, checked ->
                if (!checked) halfDate = null
                renderLeaveDates()
                refreshLeaveDays()
            }
            reqEtLeaveReason.doAfterTextChanged { reqTilLeaveReason.error = null }
        }
        renderLeaveDates()
        viewModel.loadLeaveQuotas()
    }

    private fun pickLeaveType() {
        if (quotas.isEmpty()) {
            toast(R.string.req_no_leave_types)
            viewModel.loadLeaveQuotas()
            return
        }
        val names = quotas.map { it.name.orEmpty() }.toTypedArray()
        val checked = names.indexOfFirst { it.equals(leaveType, true) }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.req_pick_leave_type)
            .setSingleChoiceItems(names, checked) { dialog, which ->
                selectLeaveType(names[which])
                dialog.dismiss()
            }
            .show()
    }

    private fun selectLeaveType(name: String) {
        leaveType = name
        val quota = quotas.firstOrNull { it.name.equals(name, true) }
        binding.reqEtLeaveType.setText(quota?.name ?: name)
        binding.reqTilLeaveType.error = null
        binding.reqEtRemaining.setText(WsFormat.number(quota?.remaining ?: 0.0))
        refreshLeaveDays()
    }

    private fun pickStartDate() {
        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText(R.string.req_pick_start_date)
            .apply { startDate?.let { setSelection(dayToUtcMillis(it)) } }
            .build()
        picker.addOnPositiveButtonClickListener { millis ->
            startDate = utcMillisToDay(millis)
            // The end must not be before the start.
            if (endDate != null && endDate!! < startDate!!) endDate = null
            if (halfDate != null && (halfDate!! < startDate!! || endDate == null || halfDate!! > endDate!!)) halfDate = null
            binding.reqTilStartDate.error = null
            renderLeaveDates()
            refreshLeaveDays()
        }
        picker.show(childFragmentManager, "leaveStart")
    }

    private fun pickEndDate() {
        val builder = MaterialDatePicker.Builder.datePicker().setTitleText(R.string.req_pick_end_date)
        startDate?.let {
            val startMillis = dayToUtcMillis(it)
            builder.setCalendarConstraints(
                CalendarConstraints.Builder()
                    .setStart(startMillis)
                    .setValidator(DateValidatorPointForward.from(startMillis))
                    .build()
            )
        }
        (endDate ?: startDate)?.let { builder.setSelection(dayToUtcMillis(it)) }
        val picker = builder.build()
        picker.addOnPositiveButtonClickListener { millis ->
            endDate = utcMillisToDay(millis)
            if (halfDate != null && (startDate == null || halfDate!! < startDate!! || halfDate!! > endDate!!)) halfDate = null
            binding.reqTilEndDate.error = null
            renderLeaveDates()
            refreshLeaveDays()
        }
        picker.show(childFragmentManager, "leaveEnd")
    }

    private fun pickHalfDate() {
        val s = startDate ?: return
        val e = endDate ?: return
        val sMillis = dayToUtcMillis(s)
        val eMillis = dayToUtcMillis(e)
        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText(R.string.req_pick_half_date)
            .setCalendarConstraints(
                CalendarConstraints.Builder()
                    .setStart(sMillis)
                    .setEnd(eMillis)
                    .setValidator(
                        CompositeDateValidator.allOf(
                            listOf<CalendarConstraints.DateValidator>(
                                DateValidatorPointForward.from(sMillis),
                                DateValidatorPointBackward.before(eMillis)
                            )
                        )
                    )
                    .build()
            )
            .apply { halfDate?.let { setSelection(dayToUtcMillis(it)) } }
            .build()
        picker.addOnPositiveButtonClickListener { millis ->
            halfDate = utcMillisToDay(millis)
            binding.reqTilHalfDate.error = null
            renderLeaveDates()
            refreshLeaveDays()
        }
        picker.show(childFragmentManager, "leaveHalf")
    }

    private fun renderLeaveDates() {
        with(binding) {
            reqEtStartDate.setText(WsFormat.reversedDay(startDate))
            reqEtEndDate.setText(WsFormat.reversedDay(endDate))
            reqEtHalfDate.setText(WsFormat.reversedDay(halfDate))
            // A half day inside a multi-day leave needs its own date.
            val multiDay = startDate != null && endDate != null && startDate != endDate
            reqColHalfDate.visibility = if (reqCbHalfDay.isChecked && multiDay) View.VISIBLE else View.GONE
        }
    }

    /** Server-side day count; "0" while anything is missing. */
    private fun refreshLeaveDays() {
        val type = leaveType
        val from = startDate
        val to = endDate
        if (type.isNullOrBlank() || from == null || to == null) {
            showLeaveDays(0.0)
            return
        }
        viewModel.loadLeaveDays(type, from, to, binding.reqCbHalfDay.isChecked, halfDate)
    }

    private fun showLeaveDays(days: Double) {
        leaveDays = days
        _binding?.reqEtLeaveCount?.setText(WsFormat.number(days))
    }

    private fun validateLeave(): Boolean {
        with(binding) {
            val reason = reqEtLeaveReason.text?.toString().orEmpty().trim()
            val needsHalfDate = reqCbHalfDay.isChecked && startDate != null && endDate != null && startDate != endDate
            reqTilLeaveType.error = if (leaveType.isNullOrBlank()) getString(R.string.req_err_leave_type) else null
            reqTilStartDate.error = if (startDate == null) getString(R.string.req_err_start_date) else null
            reqTilEndDate.error = if (endDate == null) getString(R.string.req_err_end_date) else null
            reqTilHalfDate.error = if (needsHalfDate && halfDate == null) getString(R.string.req_err_half_date) else null
            reqTilLeaveReason.error = if (reason.isEmpty()) getString(R.string.req_err_leave_reason) else null
            val valid = listOf(reqTilLeaveType, reqTilStartDate, reqTilEndDate, reqTilHalfDate, reqTilLeaveReason)
                .all { it.error == null }
            if (!valid) {
                toast(R.string.req_toast_leave_fields)
                return false
            }
        }
        // A new request must fit the quota; the server re-checks edits.
        val remaining = quotas.firstOrNull { it.name.equals(leaveType, true) }?.remaining ?: 0.0
        if (!isUpdate && leaveDays > remaining) {
            toast(R.string.req_toast_quota_exceeded)
            return false
        }
        return true
    }

    private fun submitLeave() {
        if (!validateLeave()) return
        val body = mutableMapOf<String, Any?>(
            "leave_type" to leaveType,
            "start_date" to startDate,
            "end_date" to endDate,
            "leave_reason" to binding.reqEtLeaveReason.text?.toString().orEmpty().trim()
        )
        if (binding.reqCbHalfDay.isChecked) {
            body["half_day"] = true
            halfDate?.let { body["half_day_date"] = it }
        }
        if (isUpdate) {
            body["leave_id"] = recordId
            lastOp = Op.UPDATE
            viewModel.updateLeave(body)
        } else {
            lastOp = Op.CREATE
            viewModel.createLeave(body)
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Attendance form
    // ---------------------------------------------------------------------------------------------

    private fun setupAttendanceForm() {
        with(binding) {
            reqEtDate.setOnClickListener { pickAttendanceDate() }
            reqTilDate.setEndIconOnClickListener { pickAttendanceDate() }
            reqEtTime.setOnClickListener { pickAttendanceTime() }
            reqTilTime.setEndIconOnClickListener { pickAttendanceTime() }
            reqEtReason.doAfterTextChanged { reqTilReason.error = null }
        }
        renderAttendanceDateTime()
        viewModel.loadOptions()
    }

    /** Fills [group] with one segmented button per option and selects [selected] (or the first). */
    private fun buildToggle(
        group: MaterialButtonToggleGroup,
        options: List<String>,
        label: (String) -> String,
        icon: (String) -> Int,
        selected: String?,
        onSelect: (String) -> Unit
    ) {
        group.clearOnButtonCheckedListeners()
        group.removeAllViews()
        val buttons = options.map { option ->
            val button = layoutInflater.inflate(R.layout.req_toggle_button, group, false) as MaterialButton
            button.id = View.generateViewId()
            button.text = label(option)
            button.setIconResource(icon(option))
            button.tag = option
            group.addView(button)
            button
        }
        group.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) group.findViewById<View>(checkedId)?.tag?.let { onSelect(it as String) }
        }
        val initial = buttons.firstOrNull { (it.tag as String).equals(selected, true) } ?: buttons.firstOrNull()
        initial?.let { group.check(it.id) }
    }

    private fun typeLabel(value: String): String = when (value.uppercase()) {
        "IN" -> getString(R.string.req_check_in)
        "OUT" -> getString(R.string.req_check_out)
        else -> value
    }

    @DrawableRes
    private fun typeIcon(value: String): Int =
        if (value.equals("OUT", true)) R.drawable.ws_ic_arrow_out else R.drawable.ws_ic_arrow_in

    @DrawableRes
    private fun locationIcon(value: String): Int {
        val text = value.lowercase()
        return when {
            text.contains("home") -> R.drawable.ws_ic_home
            text.contains("office") -> R.drawable.ws_ic_apartment
            else -> R.drawable.ws_ic_location
        }
    }

    private fun pickAttendanceDate() {
        // Only days up to today can be requested.
        val today = MaterialDatePicker.todayInUtcMilliseconds()
        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText(R.string.req_pick_date)
            .setCalendarConstraints(
                CalendarConstraints.Builder()
                    .setEnd(today)
                    .setOpenAt(attendanceDate?.let { dayToUtcMillis(it) } ?: today)
                    .setValidator(DateValidatorPointBackward.before(today))
                    .build()
            )
            .setSelection(attendanceDate?.let { dayToUtcMillis(it) } ?: today)
            .build()
        picker.addOnPositiveButtonClickListener { millis ->
            attendanceDate = utcMillisToDay(millis)
            binding.reqTilDate.error = null
            renderAttendanceDateTime()
        }
        picker.show(childFragmentManager, "attendanceDate")
    }

    private fun pickAttendanceTime() {
        val now = Calendar.getInstance()
        val parts = attendanceTime?.split(":")
        val hour = parts?.getOrNull(0)?.toIntOrNull() ?: now.get(Calendar.HOUR_OF_DAY)
        val minute = parts?.getOrNull(1)?.toIntOrNull() ?: now.get(Calendar.MINUTE)
        val picker = MaterialTimePicker.Builder()
            .setTimeFormat(if (DateFormat.is24HourFormat(requireContext())) TimeFormat.CLOCK_24H else TimeFormat.CLOCK_12H)
            .setHour(hour)
            .setMinute(minute)
            .setTitleText(R.string.req_pick_time)
            .build()
        picker.addOnPositiveButtonClickListener {
            attendanceTime = String.format(Locale.US, "%02d:%02d", picker.hour, picker.minute)
            binding.reqTilTime.error = null
            renderAttendanceDateTime()
        }
        picker.show(childFragmentManager, "attendanceTime")
    }

    private fun renderAttendanceDateTime() {
        binding.reqEtDate.setText(WsFormat.reversedDay(attendanceDate))
        binding.reqEtTime.setText(attendanceTime?.let { WsFormat.clockText(requireContext(), it) }.orEmpty())
    }

    private fun validateAttendance(): Boolean {
        val valid = with(binding) {
            val reason = reqEtReason.text?.toString().orEmpty().trim()
            reqErrType.visibility = if (attendanceType.isNullOrBlank()) View.VISIBLE else View.GONE
            reqErrLocation.visibility = if (location.isNullOrBlank()) View.VISIBLE else View.GONE
            reqTilDate.error = if (attendanceDate == null) getString(R.string.req_err_date) else null
            reqTilTime.error = if (attendanceTime == null) getString(R.string.req_err_time) else null
            reqTilReason.error = if (reason.isEmpty()) getString(R.string.req_err_reason) else null
            reqErrType.visibility == View.GONE && reqErrLocation.visibility == View.GONE &&
                listOf<TextInputLayout>(reqTilDate, reqTilTime, reqTilReason).all { it.error == null }
        }
        if (!valid) toast(R.string.req_toast_attendance_fields)
        return valid
    }

    private fun submitAttendance() {
        if (!validateAttendance()) return
        val body = mutableMapOf<String, Any?>(
            "log_type" to attendanceType,
            "location" to location,
            "date_time" to "$attendanceDate $attendanceTime:00",
            "attendance_reason" to binding.reqEtReason.text?.toString().orEmpty().trim(),
            "request_status" to "Pending",
            "checkin_id" to if (isUpdate) recordId else ""
        )
        if (isUpdate) {
            lastOp = Op.UPDATE
            viewModel.updateAttendance(body)
        } else {
            lastOp = Op.CREATE
            GlobalConfig.currentEmployeeId().takeIf { it.isNotBlank() }?.let { body["employee"] = it }
            viewModel.createAttendance(body)
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Footer actions
    // ---------------------------------------------------------------------------------------------

    private fun submit() {
        hideKeyboard()
        if (mode == RequestFilter.LEAVE) submitLeave() else submitAttendance()
    }

    private fun confirmDelete() {
        hideKeyboard()
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.req_delete_title)
            .setMessage(R.string.req_delete_message)
            .setNegativeButton(R.string.req_cancel, null)
            .setPositiveButton(R.string.req_delete) { _, _ ->
                lastOp = Op.DELETE
                if (mode == RequestFilter.LEAVE) viewModel.deleteLeave(recordId) else viewModel.deleteAttendance(recordId)
            }
            .show()
    }

    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.root.windowToken, 0)
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private companion object {
        const val POP_DELAY_MS = 600L

        private val DAY = Regex("""\d{4}-\d{2}-\d{2}""")
        private val TIME = Regex("""[ T](\d{1,2}):(\d{1,2})""")

        /** "2026-07-09 09:30:00" -> "2026-07-09" (null when there is no date). */
        fun dayPart(value: String): String? = DAY.find(value)?.value

        /** "2026-07-09 9:5:00" -> "09:05" (null when there is no time). */
        fun timePart(value: String): String? =
            TIME.find(value)?.destructured?.let { (h, m) -> String.format(Locale.US, "%02d:%02d", h.toInt(), m.toInt()) }

        private fun dayFormat() = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }

        /** Material date pickers speak UTC midnight. */
        fun dayToUtcMillis(day: String): Long = runCatching { dayFormat().parse(day)?.time }.getOrNull() ?: MaterialDatePicker.todayInUtcMilliseconds()

        fun utcMillisToDay(millis: Long): String = dayFormat().format(millis)
    }
}
