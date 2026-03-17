package com.axelliant.hris.screens

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.axelliant.hris.R
import com.axelliant.hris.adapter.PersonSpinnerAdapter
import com.axelliant.hris.adapter.SubFilterAdapter
import com.axelliant.hris.adapter.TeamLeaveDetailAdapter
import com.axelliant.hris.base.BaseFragment
import com.axelliant.hris.callback.AdapterItemClick
import com.axelliant.hris.config.GlobalConfig
import com.axelliant.hris.databinding.FragmentMyTeamLeaveDetailBinding
import com.axelliant.hris.enums.AttendanceFilter
import com.axelliant.hris.event.EventObserver
import com.axelliant.hris.extention.showErrorMsg
import com.axelliant.hris.model.attendance.AttendanceInput
import com.axelliant.hris.model.dashboard.EmployProfile
import com.axelliant.hris.model.dashboard.FilterModel
import com.axelliant.hris.model.leave.TeamLeaveDetail
import com.axelliant.hris.utils.Utils
import com.axelliant.hris.viewmodel.LeaveViewModel
import com.google.android.material.datepicker.MaterialDatePicker
import org.koin.android.ext.android.inject
import java.util.Date

class TeamLeaveDetailFragment : BaseFragment() {

    private var _binding: FragmentMyTeamLeaveDetailBinding? = null
    private val binding get() = _binding
    private var currentFilter = AttendanceFilter.WEEK
    private val leaveViewModel: LeaveViewModel by inject()
    private var startDateString: String? = null
    private var endDateString: String? = null
    private var filterId = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentMyTeamLeaveDetailBinding.inflate(inflater).also { _binding = it }
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

        binding?.ivBack?.setOnClickListener {
            previousFragmentNavigation()
        }

        spinnerPopulations()
        leaveViewModel.getTeamLeaveDetail(getCurrentObject())
        eventSelection()

        leaveViewModel.teamLeaveDetailResponse.observe(
            viewLifecycleOwner,
            EventObserver { response ->

                if (response?.meta?.status == true) {
                    response.leave_status?.let { subFilterPopulations(it) }
                    if (response.leaves?.size ?: 0 > 0) {
                        binding?.rvAttend?.visibility=View.VISIBLE
                        binding?.tvNoRecord?.visibility=View.GONE
                        dataPopulate(response.leaves)


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
    private fun spinnerPopulations() {

        val adapter = PersonSpinnerAdapter(
            requireContext(),  GlobalConfig.getReportingEmploys()
        )
        binding?.spTeamMember?.adapter = adapter

        binding?.spTeamMember?.onItemSelectedListener = object :
            AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                leaveViewModel.getTeamLeaveDetail(getCurrentObject())

            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
            }

        }

    }

    private fun subFilterPopulations(attendanceStatusList: ArrayList<FilterModel>) {

        attendanceStatusList.add(0,FilterModel().apply {
            this.id = ""
            this.title = "All"
            this.count = "0"
        })
        binding?.rvSubFilter?.layoutManager =
            LinearLayoutManager(requireActivity(), RecyclerView.HORIZONTAL, false)
        val weeklyAdapter = SubFilterAdapter(
            filterId,
            attendanceStatusList, requireContext(),
            object : AdapterItemClick {
                override fun onItemClick(customObject: Any, position: Int) {
                    val filterObject = customObject as FilterModel

                    filterId = filterObject.id.toString()
                    leaveViewModel.getTeamLeaveDetail(getCurrentObject())

                }

            }
        )
        binding?.rvSubFilter?.adapter = weeklyAdapter

    }

    private fun dataPopulate(leaves: ArrayList<TeamLeaveDetail>?) {
        binding?.rvAttend?.layoutManager = LinearLayoutManager(requireActivity())
        val weeklyAdapter = TeamLeaveDetailAdapter(requireContext(),
            leaves!!)
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
            leaveViewModel.getTeamLeaveDetail(getCurrentObject())
            eventSelection()
        }
        binding?.tvMonth?.setOnClickListener {
            currentFilter = AttendanceFilter.MONTH
            leaveViewModel.getTeamLeaveDetail(getCurrentObject())
            eventSelection()
        }

        binding?.tvCustom?.setOnClickListener {
            datePickerDialog()
            currentFilter = AttendanceFilter.Custom
            leaveViewModel.getTeamLeaveDetail(getCurrentObject())
            eventSelection()
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

            else -> {}

        }
        return AttendanceInput().apply {

            val currentEmploy = binding?.spTeamMember?.selectedItem as EmployProfile


            this.startDate = startDateString!!
            this.endDate = endDateString!!
            this.filter = currentFilter
            this.filters= filterId

            if (currentEmploy.name == null)
                this.employeeId = listOf()
            else
                this.employeeId = listOf(currentEmploy.name.toString())

        }

    }

    private fun setDateView() {
        if (startDateString != null && endDateString != null) {
            binding?.tvStartDateTxt?.text = startDateString
            binding?.tvEndDateTxt?.text = endDateString
        }

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

            setDateView()

            currentFilter = AttendanceFilter.Custom
            leaveViewModel.getTeamLeaveDetail(getCurrentObject())
            eventSelection()
        }

        // Showing the date picker dialog
        datePicker.show(activity?.supportFragmentManager!!, "DATE_PICKER")
    }


}