package com.axelliant.hrms.screens

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.axelliant.hrms.R
import com.axelliant.hrms.adapter.CheckInListAdapter
import com.axelliant.hrms.adapter.MyAttendanceDetailAdapter
import com.axelliant.hrms.adapter.SubFilterAdapter
import com.axelliant.hrms.base.BaseFragment
import com.axelliant.hrms.callback.AdapterItemClick
import com.axelliant.hrms.config.AppConst
import com.axelliant.hrms.config.AppConst.KEY_ID
import com.axelliant.hrms.config.GlobalConfig
import com.axelliant.hrms.databinding.FragmentCheckInListBinding
import com.axelliant.hrms.databinding.FragmentMyAttendanceDetailBinding
import com.axelliant.hrms.enums.AttendanceFilter
import com.axelliant.hrms.enums.AttendanceFilter.Custom
import com.axelliant.hrms.enums.AttendanceFilter.MONTH
import com.axelliant.hrms.enums.AttendanceFilter.WEEK
import com.axelliant.hrms.enums.RequestFilter
import com.axelliant.hrms.event.EventObserver
import com.axelliant.hrms.extention.showErrorMsg
import com.axelliant.hrms.model.attendance.AttendanceDetail
import com.axelliant.hrms.model.attendance.AttendanceInput
import com.axelliant.hrms.model.checkin.CheckInDetail
import com.axelliant.hrms.model.dashboard.FilterModel
import com.axelliant.hrms.model.leave.LeaveDetail
import com.axelliant.hrms.navigation.AppNavigator
import com.axelliant.hrms.network.ErrorMessages
import com.axelliant.hrms.utils.Utils
import com.axelliant.hrms.viewmodel.AttendanceViewModel
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.gson.Gson
import org.koin.android.ext.android.inject
import java.util.Date


class CheckInListFragment : BaseFragment() {

    private var startDateString: String? = null
    private var endDateString: String? = null
    private var _binding: FragmentCheckInListBinding? = null
    private val binding get() = _binding
    private var currentFilter = WEEK
    private val attendanceViewModel: AttendanceViewModel by inject()

    private var filterId = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentCheckInListBinding.inflate(inflater).also { _binding = it }
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

        attendanceViewModel.getCheckInList(getCurrentObject())
        eventSelection()

        attendanceViewModel.checkInListResponse.observe(
            viewLifecycleOwner,
            EventObserver { response ->

                if (response?.meta?.status == true) {
                    dataPopulate(response.checkin!!)
                    subFilterPopulations(response.checkin_status!!)


                } else {
                    requireContext().showErrorMsg(response?.meta?.message.toString())
                }

            })

        binding?.ivBack?.setOnClickListener {
            previousFragmentNavigation()
        }


    }


    private fun dataPopulate(attendanceData: ArrayList<CheckInDetail>) {

        binding?.rvAttendanceDetail?.layoutManager = LinearLayoutManager(requireActivity())
        val weeklyAdapter = CheckInListAdapter(attendanceData, object : AdapterItemClick {
            override fun onItemClick(customObject: Any, position: Int) {

                val attendanceDetail = customObject as CheckInDetail

                if (attendanceDetail.requeststatus == "Pending") {
                    AppNavigator.navigateToRequest(Bundle().apply {
                        this.putString(AppConst.RequestType, RequestFilter.ATTENDANCE.name)
                        this.putString(
                            AppConst.AttendanceRequestParam,
                            Gson().toJson(attendanceDetail)
                        )
                    })
                } else
                    requireContext().showErrorMsg(
                        ErrorMessages.CHECK_IN_PENDING_ONLY.toString().plus(" ")
                            .plus(attendanceDetail.requeststatus)
                    )

            }
        })
        binding?.rvAttendanceDetail?.adapter = weeklyAdapter


    }

    private fun subFilterPopulations(attendanceStatusList: ArrayList<FilterModel>) {
        attendanceStatusList.add(0, FilterModel().apply {
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
                    attendanceViewModel.getCheckInList(getCurrentObject())

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
            attendanceViewModel.getCheckInList(getCurrentObject())
            eventSelection()
        }

        binding?.tvMonth?.setOnClickListener {
            currentFilter = MONTH
            attendanceViewModel.getCheckInList(getCurrentObject())
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
            attendanceViewModel.getCheckInList(getCurrentObject())
            eventSelection()
        }

        // Showing the date picker dialog
        datePicker.show(activity?.supportFragmentManager!!, "DATE_PICKER")
    }
}