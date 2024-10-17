package com.axelliant.hris.screens

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.axelliant.hris.R
import com.axelliant.hris.adapter.PersonSpinnerAdapter
import com.axelliant.hris.adapter.TeamAttendanceDetailAdapter
import com.axelliant.hris.base.BaseFragment
import com.axelliant.hris.callback.AdapterItemClick
import com.axelliant.hris.config.AppConst.KEY_ID
import com.axelliant.hris.config.GlobalConfig
import com.axelliant.hris.databinding.FragmentTeamAttendanceDetailBinding
import com.axelliant.hris.enums.AttendanceFilter
import com.axelliant.hris.event.EventObserver
import com.axelliant.hris.extention.showErrorMsg
import com.axelliant.hris.model.attendance.AttendanceData
import com.axelliant.hris.model.attendance.AttendanceInput
import com.axelliant.hris.model.dashboard.EmployProfile
import com.axelliant.hris.navigation.AppNavigator
import com.axelliant.hris.utils.Utils
import com.axelliant.hris.viewmodel.AttendanceViewModel
import com.google.android.material.datepicker.MaterialDatePicker
import org.koin.android.ext.android.inject
import java.util.Date


class TeamAttendanceDetailFragment : BaseFragment() {

    private var startDateString: String? = null
    private var endDateString: String? = null
    private var _binding: FragmentTeamAttendanceDetailBinding? = null
    private val binding get() = _binding
    private var currentFilter = AttendanceFilter.WEEK
    private var selectedDateRange: String? = null
    private var emplId = ""

    private val attendanceViewModel: AttendanceViewModel by inject()


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentTeamAttendanceDetailBinding.inflate(inflater).also { _binding = it }
        return binding?.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        attendanceViewModel.getIsLoading()
            .observe(viewLifecycleOwner, EventObserver { isLoading ->
                if (isLoading) {
                    showDialog()
                } else {
                    hideDialog()
                }
            })

        binding?.ivBack?.setOnClickListener {
            previousFragmentNavigation()
        }

        eventSelection()
        spinnerPopulations()

        attendanceViewModel.getTeamAttendance(getCurrentObject())

        attendanceViewModel.teamAttendanceResponse.observe(
            viewLifecycleOwner,
            EventObserver { response ->

                if (response?.meta?.status == true) {
                    // success
                    if (response.attendance_data?.size ?: 0 > 0) {
                        binding?.rvAttend?.visibility=View.VISIBLE
                        binding?.tvNoRecord?.visibility=View.GONE

                        response.attendance_data?.let { dataPopulate(it) }
                    } else {
                        binding?.rvAttend?.visibility = View.GONE
                        binding?.tvNoRecord?.visibility = View.VISIBLE
                    }

                    binding?.tvTeamMemberTxt?.text = response.team_count.toString()
                } else {
                    requireContext().showErrorMsg(response?.meta?.message.toString())
                }

            })


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

            currentFilter = AttendanceFilter.Custom
            attendanceViewModel.getTeamAttendance(getCurrentObject())
            eventSelection()
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

    private fun spinnerPopulations() {

        val adapter = PersonSpinnerAdapter(
            requireContext(), GlobalConfig.getReportingEmploys()
        )
        binding?.spTeamMember?.adapter = adapter


        binding?.spTeamMember?.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                attendanceViewModel.getTeamAttendance(getCurrentObject())

            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
            }

        }


    }


    private fun dataPopulate(detailArrayList: ArrayList<AttendanceData>) {
        binding?.rvAttend?.layoutManager = LinearLayoutManager(requireActivity())
        val weeklyAdapter = TeamAttendanceDetailAdapter(requireContext(),
            detailArrayList,
            object : AdapterItemClick {
                override fun onItemClick(customObject: Any, position: Int) {
                    val currentObject = customObject as AttendanceData
                    AppNavigator.navigateToMyAttendanceDetail(Bundle().apply {
                        this.putString(KEY_ID, currentObject.id)
                    })


                }

            }
        )
        binding?.rvAttend?.adapter = weeklyAdapter


    }

    private fun eventSelection() {
        binding?.tvWeek?.background =
            ContextCompat.getDrawable(requireContext(), R.drawable.rounded_disabled)

        binding?.tvMonth?.background =
            ContextCompat.getDrawable(requireContext(), R.drawable.rounded_disabled)

        binding?.tvCustom?.background =
            ContextCompat.getDrawable(requireContext(), R.drawable.rounded_disabled)


        binding?.tvWeek?.setTextColor(requireContext().getColor(R.color.btn_text_color))
        binding?.tvMonth?.setTextColor(requireContext().getColor(R.color.btn_text_color))
        binding?.tvCustom?.setTextColor(requireContext().getColor(R.color.btn_text_color))

        binding?.tvWeek?.setOnClickListener {
            currentFilter = AttendanceFilter.WEEK
            attendanceViewModel.getTeamAttendance(getCurrentObject())
            eventSelection()
        }
        binding?.tvMonth?.setOnClickListener {
            currentFilter = AttendanceFilter.MONTH
            attendanceViewModel.getTeamAttendance(getCurrentObject())
            eventSelection()
        }

        binding?.tvCustom?.setOnClickListener {

            datePickerDialog()
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

            AttendanceFilter.Custom -> {

                binding?.tvCustom?.background =
                    ContextCompat.getDrawable(requireContext(), R.drawable.rounded_enabled)
                binding?.tvCustom?.setTextColor(requireContext().getColor(R.color.white))
            }

            else -> {}
        }
    }


    private fun getCurrentObject(): AttendanceInput {

        when (currentFilter) {
            AttendanceFilter.WEEK -> {
                startDateString = Utils.getServerFormat(date = Utils.getLastWeek())
                endDateString = Utils.getServerFormat()

                setDateView()
            }

            AttendanceFilter.MONTH -> {
                startDateString = Utils.getServerFormat(date = Utils.getFirstDayOfMonth())
                endDateString =
                    Utils.getServerFormat(date = Utils.getLastDayOfMonth())

                setDateView()
            }

            AttendanceFilter.Custom -> {}
        }


        val currentEmploy = binding?.spTeamMember?.selectedItem as EmployProfile
        return AttendanceInput().apply {
            this.startDate = startDateString!!
            this.endDate = endDateString!!
            this.filter = currentFilter

            if (currentEmploy.name == null)
                this.employeeId = listOf()
            else
                this.employeeId = listOf(currentEmploy.name.toString())


        }

    }


}