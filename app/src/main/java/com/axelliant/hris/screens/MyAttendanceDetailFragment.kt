package com.axelliant.hris.screens

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.axelliant.hris.R
import com.axelliant.hris.adapter.MyAttendanceDetailAdapter
import com.axelliant.hris.adapter.SubFilterAdapter
import com.axelliant.hris.base.BaseFragment
import com.axelliant.hris.callback.AdapterItemClick
import com.axelliant.hris.config.AppConst
import com.axelliant.hris.config.AppConst.KEY_ID
import com.axelliant.hris.config.GlobalConfig
import com.axelliant.hris.databinding.FragmentMyAttendanceDetailBinding
import com.axelliant.hris.enums.AttendanceFilter
import com.axelliant.hris.enums.AttendanceFilter.Custom
import com.axelliant.hris.enums.AttendanceFilter.MONTH
import com.axelliant.hris.enums.AttendanceFilter.WEEK
import com.axelliant.hris.enums.RequestFilter
import com.axelliant.hris.event.EventObserver
import com.axelliant.hris.extention.showErrorMsg
import com.axelliant.hris.model.attendance.AttendanceDetail
import com.axelliant.hris.model.attendance.AttendanceInput
import com.axelliant.hris.model.dashboard.FilterModel
import com.axelliant.hris.navigation.AppNavigator
import com.axelliant.hris.utils.Utils
import com.axelliant.hris.viewmodel.AttendanceViewModel
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.gson.Gson
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
    private var filterId = ""

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

        attendanceViewModel.attendanceDetailResponse.observe(
            viewLifecycleOwner,
            EventObserver { response ->

                if (response?.meta?.status == true) {
                    dataPopulate(response.attendance_data!!)
                    subFilterPopulations(response.attendance_status!!)


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
        val weeklyAdapter = MyAttendanceDetailAdapter(attendanceData,requireContext(),object :AdapterItemClick{
            override fun onItemClick(customObject: Any, position: Int) {

                val attendanceDetail = customObject as AttendanceDetail
//                AppNavigator.navigateToRequest(Bundle().apply {
//                    this.putString(AppConst.RequestType, RequestFilter.ATTENDANCE.name)
//                    this.putString(AppConst.AttendanceRequestParam, Gson().toJson(attendanceDetail))
//                })


            }
        })
        binding?.rvAttendanceDetail?.adapter = weeklyAdapter



    }

    private fun subFilterPopulations(attendanceStatusList: ArrayList<FilterModel>) {
        attendanceStatusList.add(0,FilterModel().apply {
            this.id = ""
            this.title = "All"
            this.count = "0"
        })

        binding?.rvSubFilter?.layoutManager =
            LinearLayoutManager(requireActivity(), RecyclerView.HORIZONTAL, false)
        val weeklyAdapter = SubFilterAdapter(filterId,
            attendanceStatusList, requireContext(),
            object : AdapterItemClick {
                override fun onItemClick(customObject: Any, position: Int) {
                    val filterObject = customObject as FilterModel

                    filterId = filterObject.id.toString()
                    attendanceViewModel.getAttendanceDetail(getCurrentObject())

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
            this.filters = filterId

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
            attendanceViewModel.getAttendanceDetail(getCurrentObject())
            eventSelection()
        }

        // Showing the date picker dialog
        datePicker.show(activity?.supportFragmentManager!!, "DATE_PICKER")
    }
}