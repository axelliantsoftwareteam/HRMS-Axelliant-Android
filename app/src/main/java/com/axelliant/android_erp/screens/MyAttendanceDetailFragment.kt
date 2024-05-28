package com.axelliant.android_erp.screens

import android.R.attr.defaultValue
import android.R.attr.key
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.axelliant.android_erp.R
import com.axelliant.android_erp.Test
import com.axelliant.android_erp.adapter.MyAttendanceDetailAdapter
import com.axelliant.android_erp.adapter.SubFilterAdapter
import com.axelliant.android_erp.base.BaseFragment
import com.axelliant.android_erp.callback.AdapterItemClick
import com.axelliant.android_erp.config.AppConst.KEY_ID
import com.axelliant.android_erp.config.GlobalConfig
import com.axelliant.android_erp.databinding.FragmentMyAttendanceDetailBinding
import com.axelliant.android_erp.enums.AttendanceFilter.Custom
import com.axelliant.android_erp.enums.AttendanceFilter.MONTH
import com.axelliant.android_erp.enums.AttendanceFilter.WEEK
import com.axelliant.android_erp.event.EventObserver
import com.axelliant.android_erp.extention.showErrorMsg
import com.axelliant.android_erp.extention.showSuccessMsg
import com.axelliant.android_erp.model.attendance.AttendanceDetail
import com.axelliant.android_erp.model.attendance.AttendanceInput
import com.axelliant.android_erp.utils.Utils
import com.axelliant.android_erp.viewmodel.AttendanceViewModel
import com.google.android.material.datepicker.MaterialDatePicker
import org.koin.android.ext.android.inject
import java.util.Date


class MyAttendanceDetailFragment : BaseFragment() {

    private var startDateString: String? = null
    private var endDateString: String? = null
    private var _binding: FragmentMyAttendanceDetailBinding? = null
    private val binding get() = _binding
    private var currentFilter = WEEK
    private val attendanceViewModel: AttendanceViewModel by inject()

    private var emplId = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentMyAttendanceDetailBinding.inflate(inflater).also { _binding = it }
        return binding?.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val bundle = this.arguments
        if (bundle != null) {
            emplId = bundle.getString(KEY_ID, GlobalConfig.currentEmployeeId())
        }
        attendanceViewModel.getIsLoading()
            .observe(viewLifecycleOwner, EventObserver { isLoading ->
                if (isLoading) {
                    showDialog()
                } else {
                    hideDialog()
                }
            })

        attendanceViewModel.getAttendanceDetail(getCurrentObject())
        eventSelection()
        subFilterPopulations()

        attendanceViewModel.attendanceDetailResponse.observe(
            viewLifecycleOwner,
            EventObserver { response ->

                if (response?.meta?.status == true) {
                    dataPopulate(response.attendance_data!!)

                } else {
                    requireContext().showErrorMsg(response?.meta?.message.toString())
                }

            })

        binding?.ivBack?.setOnClickListener {
            previousFragmentNavigation()
        }


    }


    private fun dataPopulate(attendanceData: ArrayList<AttendanceDetail>) {
        binding?.rvAttendanceDetail?.layoutManager = LinearLayoutManager(requireActivity())
        val weeklyAdapter = MyAttendanceDetailAdapter(attendanceData)
        binding?.rvAttendanceDetail?.adapter = weeklyAdapter


    }

    private fun subFilterPopulations() {
        binding?.rvSubFilter?.layoutManager =
            LinearLayoutManager(requireActivity(), RecyclerView.HORIZONTAL, false)
        val weeklyAdapter = SubFilterAdapter(
            listOf(
                Test("Pending"),
                Test("Approved"),
                Test("Work from home"),
                Test("In office"),
                Test("Remote"),
                Test("Rejected")
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
            currentFilter = WEEK
            attendanceViewModel.getAttendanceDetail(getCurrentObject())
            eventSelection()
        }

        binding?.tvMonth?.setOnClickListener {
            currentFilter = MONTH
            attendanceViewModel.getAttendanceDetail(getCurrentObject())
            eventSelection()
        }

        binding?.tvCustom?.setOnClickListener {
            datePickerDialog()
        }

        when (currentFilter) {
            WEEK -> {
                binding?.tvWeek?.background =
                    ContextCompat.getDrawable(requireContext(), R.drawable.rounded_enabled)
                binding?.tvWeek?.setTextColor(requireContext().getColor(R.color.white))

            }

            MONTH -> {

                binding?.tvMonth?.background =
                    ContextCompat.getDrawable(requireContext(), R.drawable.rounded_enabled)
                binding?.tvMonth?.setTextColor(requireContext().getColor(R.color.white))
            }

            Custom -> {

                binding?.tvCustom?.background =
                    ContextCompat.getDrawable(requireContext(), R.drawable.rounded_enabled)
                binding?.tvCustom?.setTextColor(requireContext().getColor(R.color.white))
            }

            else -> {}
        }
    }

    private fun getCurrentObject(): AttendanceInput {

        when (currentFilter) {
            WEEK -> {
                startDateString = Utils.getServerFormat(date = Utils.getLastWeek())
                endDateString = Utils.getServerFormat()

                setDateView()
            }

            MONTH -> {
                startDateString = Utils.getServerFormat(date = Utils.getFirstDayOfMonth())
                endDateString =
                    Utils.getServerFormat(date = Utils.getLastDayOfMonth())

                setDateView()
            }

            Custom -> {}
        }
        return AttendanceInput().apply {
            this.startDate = startDateString!!
            this.endDate = endDateString!!
            this.employeeId = listOf(emplId)
            this.filter = currentFilter

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

            currentFilter = Custom
            attendanceViewModel.getAttendanceDetail(getCurrentObject())
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
}