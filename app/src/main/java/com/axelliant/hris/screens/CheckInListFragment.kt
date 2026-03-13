package com.axelliant.hris.screens

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.axelliant.hris.R
import com.axelliant.hris.adapter.CheckInListAdapter
import com.axelliant.hris.adapter.SubFilterAdapter
import com.axelliant.hris.base.BaseFragment
import com.axelliant.hris.callback.AdapterItemClick
import com.axelliant.hris.config.AppConst
import com.axelliant.hris.databinding.FragmentCheckInListBinding
import com.axelliant.hris.enums.AttendanceFilter
import com.axelliant.hris.enums.AttendanceFilter.Custom
import com.axelliant.hris.enums.AttendanceFilter.MONTH
import com.axelliant.hris.enums.AttendanceFilter.WEEK
import com.axelliant.hris.enums.LeaveStatus
import com.axelliant.hris.enums.RequestFilter
import com.axelliant.hris.event.EventObserver
import com.axelliant.hris.extention.showErrorMsg
import com.axelliant.hris.model.attendance.LeaveCountInput
import com.axelliant.hris.model.checkin.CheckInDetail
import com.axelliant.hris.model.dashboard.FilterModel
import com.axelliant.hris.navigation.AppNavigator
import com.axelliant.hris.network.ErrorMessages
import com.axelliant.hris.utils.Utils
import com.axelliant.hris.viewmodel.AttendanceViewModel
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.gson.Gson
import org.koin.android.ext.android.inject
import java.util.Date


class CheckInListFragment : BaseFragment() {

    private var filterIdList: ArrayList<String>? = null
    private var filterId: String = ""
    private var startDateString: String? = null
    private var endDateString: String? = null
    private var _binding: FragmentCheckInListBinding? = null
    private val binding get() = _binding
    private var currentFilter = WEEK
    private val attendanceViewModel: AttendanceViewModel by inject()


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
                    response.checkin_status?.let { subFilterPopulations(it) }
                    if (response.checkin?.size ?: 0 > 0) {
                        binding?.rvAttendanceDetail?.visibility = View.VISIBLE
                        binding?.tvNoRecord?.visibility = View.GONE

                        response.checkin?.let { dataPopulate(it) }

                    } else {
                        binding?.rvAttendanceDetail?.visibility = View.GONE
                        binding?.tvNoRecord?.visibility = View.VISIBLE
                    }

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
        val weeklyAdapter = CheckInListAdapter(attendanceData,requireContext(),object : AdapterItemClick {
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
        val weeklyAdapter = SubFilterAdapter(
            filterId,
            attendanceStatusList, requireContext(),
            object : AdapterItemClick {
                override fun onItemClick(customObject: Any, position: Int) {
                    val filterObject = customObject as FilterModel

                    filterId = filterObject.id.toString()
                    if (filterIdList == null) {
                        filterIdList = ArrayList()
                    }
                    filterIdList?.clear()

                    filterIdList?.add(filterId)
                    attendanceViewModel.getCheckInList(getCurrentObject(filterObject.id))

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

    private fun getCurrentObject(
        status: String? = null,
        listFilter: ArrayList<String>? = null
    ): LeaveCountInput {

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
        if (status == LeaveStatus.APPROVED.value)
        {
            val statusList = listOf(
                "Approved",
                "approved",
                "Processed",
                "processed",
                "waiting",
                "Waiting"
            )

            return LeaveCountInput().apply {
                this.startDate = startDateString!!
                this.endDate = endDateString!!
                this.filter = currentFilter
                this.filters = statusList

            }
        } else {
            return LeaveCountInput().apply {
                this.startDate = startDateString!!
                this.endDate = endDateString!!
                this.filter = currentFilter
                this.filters = filterIdList

            }
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
