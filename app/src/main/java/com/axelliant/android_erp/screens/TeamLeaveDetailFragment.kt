package com.axelliant.android_erp.screens

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.axelliant.android_erp.R
import com.axelliant.android_erp.Test
import com.axelliant.android_erp.adapter.PersonSpinnerAdapter
import com.axelliant.android_erp.adapter.SubFilterAdapter
import com.axelliant.android_erp.adapter.TeamLeaveDetailAdapter
import com.axelliant.android_erp.base.BaseFragment
import com.axelliant.android_erp.callback.AdapterItemClick
import com.axelliant.android_erp.config.GlobalConfig
import com.axelliant.android_erp.databinding.FragmentMyTeamLeaveDetailBinding
import com.axelliant.android_erp.enums.AttendanceFilter
import com.axelliant.android_erp.event.EventObserver
import com.axelliant.android_erp.extention.showErrorMsg
import com.axelliant.android_erp.extention.showSuccessMsg
import com.axelliant.android_erp.model.attendance.AttendanceInput
import com.axelliant.android_erp.model.dashboard.FilterModel
import com.axelliant.android_erp.model.leave.TeamLeaveDetail
import com.axelliant.android_erp.utils.Utils
import com.axelliant.android_erp.viewmodel.LeaveViewModel
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

        leaveViewModel.getTeamLeaveDetail(getCurrentObject())
        eventSelection()

        leaveViewModel.teamLeaveDetailResponse.observe(
            viewLifecycleOwner,
            EventObserver { response ->

                if (response?.meta?.status == true) {

                    dataPopulate(response.leaves)

                } else {
                    requireContext().showErrorMsg(response?.meta?.message.toString())
                }

            })

        subFilterPopulations()
        spinnerPopulations()
    }
    private fun spinnerPopulations() {

        val adapter = PersonSpinnerAdapter(
            requireContext(),  GlobalConfig.getReportingEmploys()
        )
        binding?.spTeamMember?.adapter = adapter

    }

    private fun subFilterPopulations() {
        binding?.rvSubFilter?.layoutManager =
            LinearLayoutManager(requireActivity(), RecyclerView.HORIZONTAL, false)
        val weeklyAdapter = SubFilterAdapter(
            "",
            listOf(
                FilterModel(),
                FilterModel(),
                FilterModel()
            ), requireContext(),
            object : AdapterItemClick {
                override fun onItemClick(customObject: Any, position: Int) {
                    val currentObject = customObject as Test
                    requireContext().showSuccessMsg(
                        currentObject.testString
                    )

                }

            }
        )
        binding?.rvSubFilter?.adapter = weeklyAdapter

    }

    private fun dataPopulate(leaves: ArrayList<TeamLeaveDetail>?) {
        binding?.rvAttend?.layoutManager = LinearLayoutManager(requireActivity())
        val weeklyAdapter = TeamLeaveDetailAdapter(
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
            this.startDate = startDateString!!
            this.endDate = endDateString!!
            this.filter = currentFilter

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