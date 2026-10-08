package com.axelliant.hris.screens

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentManager
import com.axelliant.hris.R
import com.axelliant.hris.databinding.TeamSheetFiltersBinding
import com.axelliant.hris.extention.showErrorMsg
import com.axelliant.hris.model.team.TeamDates
import com.axelliant.hris.ui.WsFormat
import com.axelliant.hris.viewmodel.TeamActivityFilters
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.datepicker.MaterialDatePicker

/**
 * Team Activity filters (Arrivals and Hours tabs): date range plus employee, designation, location
 * and status text filters. Delivers the chosen filters through the Fragment Result API.
 */
class TeamActivityFilterSheet : BottomSheetDialogFragment() {

    private var _binding: TeamSheetFiltersBinding? = null
    private val binding get() = _binding!!

    private var startDate = ""
    private var endDate = ""

    override fun getTheme(): Int = R.style.Ws_BottomSheetDialog

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = TeamSheetFiltersBinding.inflate(inflater, container, false)
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
        startDate = savedInstanceState?.getString(STATE_START) ?: args.getString(ARG_START).orEmpty()
        endDate = savedInstanceState?.getString(STATE_END) ?: args.getString(ARG_END).orEmpty()
        if (savedInstanceState == null) {
            binding.teamInputEmployee.setText(args.getString(ARG_EMPLOYEE))
            binding.teamInputDesignation.setText(args.getString(ARG_DESIGNATION))
            binding.teamInputLocation.setText(args.getString(ARG_LOCATION))
            binding.teamInputStatus.setText(args.getString(ARG_STATUS))
        }
        renderDates()

        binding.teamSheetClose.setOnClickListener { dismiss() }
        binding.teamRowStart.setOnClickListener { pickDate(startDate) { startDate = it; renderDates() } }
        binding.teamRowEnd.setOnClickListener { pickDate(endDate) { endDate = it; renderDates() } }
        binding.teamBtnReset.setOnClickListener { deliver(TeamActivityFilters.default()) }
        binding.teamBtnApply.setOnClickListener {
            if (startDate > endDate) {
                requireContext().showErrorMsg(getString(R.string.team_start_after_end))
                return@setOnClickListener
            }
            deliver(
                TeamActivityFilters(
                    startDate = startDate,
                    endDate = endDate,
                    employee = binding.teamInputEmployee.text?.toString().orEmpty().trim(),
                    designation = binding.teamInputDesignation.text?.toString().orEmpty().trim(),
                    location = binding.teamInputLocation.text?.toString().orEmpty().trim(),
                    status = binding.teamInputStatus.text?.toString().orEmpty().trim()
                )
            )
        }
    }

    private fun renderDates() {
        binding.teamStartValue.text = WsFormat.date(startDate)
        binding.teamEndValue.text = WsFormat.date(endDate)
    }

    private fun pickDate(current: String, onPicked: (String) -> Unit) {
        val picker = MaterialDatePicker.Builder.datePicker()
            .setSelection(TeamDates.toPickerMillis(current))
            .build()
        picker.addOnPositiveButtonClickListener { onPicked(TeamDates.fromPickerMillis(it)) }
        picker.show(childFragmentManager, "teamDatePicker")
    }

    private fun deliver(filters: TeamActivityFilters) {
        parentFragmentManager.setFragmentResult(
            RESULT_KEY,
            bundleOf(
                ARG_START to filters.startDate,
                ARG_END to filters.endDate,
                ARG_EMPLOYEE to filters.employee,
                ARG_DESIGNATION to filters.designation,
                ARG_LOCATION to filters.location,
                ARG_STATUS to filters.status
            )
        )
        dismiss()
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
        const val RESULT_KEY = "teamActivityFilters"
        private const val TAG = "TeamActivityFilterSheet"
        private const val ARG_START = "start"
        private const val ARG_END = "end"
        private const val ARG_EMPLOYEE = "employee"
        private const val ARG_DESIGNATION = "designation"
        private const val ARG_LOCATION = "location"
        private const val ARG_STATUS = "status"
        private const val STATE_START = "stateStart"
        private const val STATE_END = "stateEnd"

        fun show(fm: FragmentManager, current: TeamActivityFilters) {
            if (fm.findFragmentByTag(TAG) != null || fm.isStateSaved) return
            TeamActivityFilterSheet().apply {
                arguments = bundleOf(
                    ARG_START to current.startDate,
                    ARG_END to current.endDate,
                    ARG_EMPLOYEE to current.employee,
                    ARG_DESIGNATION to current.designation,
                    ARG_LOCATION to current.location,
                    ARG_STATUS to current.status
                )
            }.show(fm, TAG)
        }

        /** Reads the filters back from a fragment result bundle. */
        fun parse(bundle: Bundle) = TeamActivityFilters(
            startDate = bundle.getString(ARG_START).orEmpty(),
            endDate = bundle.getString(ARG_END).orEmpty(),
            employee = bundle.getString(ARG_EMPLOYEE).orEmpty(),
            designation = bundle.getString(ARG_DESIGNATION).orEmpty(),
            location = bundle.getString(ARG_LOCATION).orEmpty(),
            status = bundle.getString(ARG_STATUS).orEmpty()
        )
    }
}
