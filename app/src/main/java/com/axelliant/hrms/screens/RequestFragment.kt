package com.axelliant.hrms.screens

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.axelliant.hrms.R
import com.axelliant.hrms.Test
import com.axelliant.hrms.adapter.CustomSpinnerAdapter
import com.axelliant.hrms.adapter.PersonSpinnerAdapter
import com.axelliant.hrms.base.BaseFragment
import com.axelliant.hrms.databinding.FragmentRequestBinding
import com.axelliant.hrms.enums.RequestFilter
import com.axelliant.hrms.event.EventObserver
import com.axelliant.hrms.extention.showErrorMsg
import com.axelliant.hrms.extention.showSuccessMsg
import com.axelliant.hrms.model.post.AttendanceRequest
import com.axelliant.hrms.model.post.LeaveRequest
import com.axelliant.hrms.navigation.AppNavigator
import com.axelliant.hrms.utils.Utils
import com.axelliant.hrms.viewmodel.LeaveViewModel
import com.axelliant.hrms.viewmodel.RequestViewModel
import com.google.android.material.datepicker.MaterialDatePicker
import org.koin.android.ext.android.inject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale


class RequestFragment : BaseFragment() {

    private var currentFilter = RequestFilter.LEAVE
    private var currentDateString: String? = null

    private var startDateString: String? = null
    private var endDateString: String? = null
    private var selectedDateRange: String? = null

    private var _binding: FragmentRequestBinding? = null
    private val binding get() = _binding

    private val requestViewModel: RequestViewModel by inject()


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentRequestBinding.inflate(inflater).also { _binding = it }
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requestViewModel.getIsLoading()
            .observe(viewLifecycleOwner, EventObserver { isLoading ->
                if (isLoading) {
                    showDialog()
                } else {
                    hideDialog()
                }
            })

        requestViewModel.leaveRequestResponse.observe(
            viewLifecycleOwner,
            EventObserver { response ->

                if (response?.meta?.status == true) {
                    requireContext().showSuccessMsg("success")
                } else {
                    requireContext().showErrorMsg(response?.meta?.message.toString())
                }

            })


        requestViewModel.attendanceRequestResponse.observe(
            viewLifecycleOwner,
            EventObserver { response ->

                if (response?.meta?.status == true) {
                    requireContext().showSuccessMsg("success")
                } else {
                    requireContext().showErrorMsg(response?.meta?.message.toString())
                }

            })


        eventSelection()
        spinnerLeavePopulations()
        spinnerAttendTypePopulations()
        spinnerLocTypePopulations()
        binding?.ivBack?.setOnClickListener {
            AppNavigator.moveBackToPreviousFragment()
        }

        binding?.lyDate?.setOnClickListener {
            pickDate()
        }
        binding?.lyStartDate?.setOnClickListener {
            datePickerDialog()
        }

        binding?.btnApply?.setOnClickListener {

            when (currentFilter) {
                RequestFilter.LEAVE -> {

                    if (binding!!.spLeaveType.selectedItemPosition == 0) {
                        requireContext().showErrorMsg("Please select leave type")
                    } else if (startDateString == null) {
                        requireContext().showErrorMsg("Please select start date")
                    } else if (endDateString == null) {
                        requireContext().showErrorMsg("Please select end date")
                    } else {
                        requestViewModel.postLeaveQuest(LeaveRequest().apply {
                            this.start_date = startDateString
                            this.end_date = endDateString
                            this.leave_reason = binding!!.etLeaveReason.text.toString()
                            this.leave_type = binding!!.spLeaveType.selectedItem.toString()

                        })

                    }

                }

                RequestFilter.ATTENDANCE -> {

                    if (binding!!.spAttendType.selectedItemPosition == 0) {
                        requireContext().showErrorMsg("Please select attendance type")
                    } else if (binding!!.spLocType.selectedItemPosition == 0) {
                        requireContext().showErrorMsg("Please select location")
                    } else if (currentDateString == null) {
                        requireContext().showErrorMsg("Please select date")
                    } else {

                        requestViewModel.postAttendanceQuest(AttendanceRequest().apply {
                            this.date = currentDateString
                            this.location_type = binding!!.spLocType.selectedItem.toString()
                            this.attendance_type = binding!!.spAttendType.selectedItem.toString()
                            this.attendance_reason = binding!!.etLeaveReason.text.toString()

                        })


                    }

                }
            }

        }
    }

    private fun pickDate() {
        val c = Calendar.getInstance()
        val year = c.get(Calendar.YEAR)
        val month = c.get(Calendar.MONTH)
        val day = c.get(Calendar.DAY_OF_MONTH)
        val datePickerDialog = DatePickerDialog(
            requireActivity(), R.style.my_dialog_theme, // Apply the theme here
            { view, year, monthOfYear, dayOfMonth ->
                val selectedDate = Calendar.getInstance()
                selectedDate.set(year, monthOfYear, dayOfMonth)

                // Format the date using SimpleDateFormat
                currentDateString = Utils.getServerFormat(date = selectedDate.time)
                binding?.tvDateTxt?.text = currentDateString
            },
            year,
            month,
            day
        )
        // Set the maximum date to today
        datePickerDialog.datePicker.maxDate = c.timeInMillis
        datePickerDialog.show()
    }

    private fun datePickerDialog() {
        // Creating a MaterialDatePicker builder for selecting a date range
        val builder = MaterialDatePicker.Builder.dateRangePicker()
        builder.setTitleText("Select a date range")

        // Building the date picker dialog
        val datePicker = builder.build()
        datePicker.addOnPositiveButtonClickListener { selection ->
            // Retrieving the selected start and end dates
            val startDate = selection.first
            val endDate = selection.second

            // Formatting the selected dates as strings

            startDateString = Utils.getServerFormat(date = Date(startDate))
            endDateString = Utils.getServerFormat(date = Date(endDate))

            // Creating the date range string
            selectedDateRange = "$startDateString - $endDateString"
            setDateView()
        }

        // Showing the date picker dialog
        datePicker.show(activity?.supportFragmentManager!!, "DATE_PICKER")
    }

    private fun setDateView() {
        if (startDateString != null && endDateString != null) {
            binding?.tvStartDateTxt?.text = startDateString
            binding?.tvEndDateTxt?.text = endDateString
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
            currentFilter = RequestFilter.LEAVE
            eventSelection()
            setLeaveView()
        }

        binding?.tvMonth?.setOnClickListener {
            currentFilter = RequestFilter.ATTENDANCE
            eventSelection()
            setAttendanceView()
        }

        when (currentFilter) {
            RequestFilter.LEAVE -> {
                binding?.tvWeek?.background =
                    ContextCompat.getDrawable(requireContext(), R.drawable.rounded_enabled)
                binding?.tvWeek?.setTextColor(requireContext().getColor(R.color.white))
                binding?.lyCreateLeave!!.visibility = View.VISIBLE
                binding?.lyCreateAttend!!.visibility = View.GONE

            }

            RequestFilter.ATTENDANCE -> {

                binding?.tvMonth?.background =
                    ContextCompat.getDrawable(requireContext(), R.drawable.rounded_enabled)
                binding?.tvMonth?.setTextColor(requireContext().getColor(R.color.white))
                binding?.lyCreateAttend!!.visibility = View.VISIBLE
                binding?.lyCreateLeave!!.visibility = View.GONE
            }
        }
    }

    private fun setLeaveView() {

    }

    private fun setAttendanceView() {

    }

    private fun spinnerLeavePopulations() {

        val adapter = CustomSpinnerAdapter(
            requireContext(), arrayListOf(
                Test("Select leave type"),
                Test("Sick"),
                Test("Annual"),
                Test("Casual"),
                Test("Breavement"),
                Test("Complimentary Leave"),
                Test("Marriage"),
                Test("Paternity")
            )
        )
        binding?.spLeaveType?.adapter = adapter

    }

    private fun spinnerAttendTypePopulations() {

        val adapter = CustomSpinnerAdapter(
            requireContext(), arrayListOf(
                Test("Select Attendance type"),
                Test("Check-Out"),
                Test("Check-In")
            )
        )
        binding?.spAttendType?.adapter = adapter

    }

    private fun spinnerLocTypePopulations() {

        val adapter = CustomSpinnerAdapter(
            requireContext(), arrayListOf(
                Test("Select Location"),
                Test("In office"),
                Test("Work from home")
            )
        )
        binding?.spLocType?.adapter = adapter
    }
}




