package com.axelliant.hris.screens

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentManager
import com.axelliant.hris.R
import com.axelliant.hris.databinding.CalSheetFiltersBinding
import com.axelliant.hris.extention.showErrorMsg
import com.axelliant.hris.model.team.TeamDates
import com.axelliant.hris.ui.WsFormat
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.datepicker.MaterialDatePicker

/**
 * "Calendar Activity Filter": date range plus employee and designation. Delivers the choice through
 * the Fragment Result API ([RESULT_KEY]); [EXTRA_RESET] is set when "Reset" was tapped.
 */
class TeamCalendarFilterSheet : BottomSheetDialogFragment() {

    private var _binding: CalSheetFiltersBinding? = null
    private val binding get() = _binding!!

    private var startDate = ""
    private var endDate = ""

    override fun getTheme(): Int = R.style.Ws_BottomSheetDialog

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = CalSheetFiltersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        (dialog as? BottomSheetDialog)?.behavior?.apply {
            skipCollapsed = true
            state = BottomSheetBehavior.STATE_EXPANDED
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val args = requireArguments()
        startDate = savedInstanceState?.getString(STATE_START) ?: args.getString(EXTRA_START).orEmpty()
        endDate = savedInstanceState?.getString(STATE_END) ?: args.getString(EXTRA_END).orEmpty()
        if (savedInstanceState == null) {
            binding.calInputEmployee.setText(args.getString(EXTRA_EMPLOYEE))
            binding.calInputDesignation.setText(args.getString(EXTRA_DESIGNATION))
        }
        renderDates()

        binding.calSheetClose.setOnClickListener { dismiss() }
        binding.calRowStart.setOnClickListener {
            val picker = MaterialDatePicker.Builder.datePicker()
                .setSelection(TeamDates.toPickerMillis(startDate))
                .build()
            picker.addOnPositiveButtonClickListener {
                startDate = TeamDates.fromPickerMillis(it)
                renderDates()
            }
            picker.show(childFragmentManager, "calStartPicker")
        }
        binding.calRowEnd.setOnClickListener {
            // The end date cannot be before the start date.
            val min = TeamDates.toPickerMillis(startDate)
            val picker = MaterialDatePicker.Builder.datePicker()
                .setSelection(maxOf(min, TeamDates.toPickerMillis(endDate)))
                .setCalendarConstraints(
                    CalendarConstraints.Builder().setStart(min).setValidator(DateValidatorPointForward.from(min)).build()
                )
                .build()
            picker.addOnPositiveButtonClickListener {
                endDate = TeamDates.fromPickerMillis(it)
                renderDates()
            }
            picker.show(childFragmentManager, "calEndPicker")
        }
        binding.calBtnSheetReset.setOnClickListener {
            parentFragmentManager.setFragmentResult(RESULT_KEY, bundleOf(EXTRA_RESET to true))
            dismiss()
        }
        binding.calBtnSheetApply.setOnClickListener {
            if (endDate < startDate) {
                requireContext().showErrorMsg(getString(R.string.cal_end_before_start))
                return@setOnClickListener
            }
            parentFragmentManager.setFragmentResult(
                RESULT_KEY,
                bundleOf(
                    EXTRA_RESET to false,
                    EXTRA_START to startDate,
                    EXTRA_END to endDate,
                    EXTRA_EMPLOYEE to binding.calInputEmployee.text?.toString().orEmpty().trim(),
                    EXTRA_DESIGNATION to binding.calInputDesignation.text?.toString().orEmpty().trim()
                )
            )
            dismiss()
        }
    }

    private fun renderDates() {
        binding.calStartValue.text = WsFormat.date(startDate)
        binding.calEndValue.text = WsFormat.date(endDate)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(STATE_START, startDate)
        outState.putString(STATE_END, endDate)
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        const val RESULT_KEY = "teamCalendarFilters"
        const val EXTRA_RESET = "reset"
        const val EXTRA_START = "start"
        const val EXTRA_END = "end"
        const val EXTRA_EMPLOYEE = "employee"
        const val EXTRA_DESIGNATION = "designation"
        private const val TAG = "TeamCalendarFilterSheet"
        private const val STATE_START = "stateStart"
        private const val STATE_END = "stateEnd"

        fun show(fm: FragmentManager, start: String, end: String, employee: String, designation: String) {
            if (fm.findFragmentByTag(TAG) != null || fm.isStateSaved) return
            TeamCalendarFilterSheet().apply {
                arguments = bundleOf(
                    EXTRA_START to start,
                    EXTRA_END to end,
                    EXTRA_EMPLOYEE to employee,
                    EXTRA_DESIGNATION to designation
                )
            }.show(fm, TAG)
        }
    }
}
