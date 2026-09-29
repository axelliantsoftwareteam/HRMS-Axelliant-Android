package com.axelliant.hris.screens

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.axelliant.hris.R
import com.axelliant.hris.adapter.MyAttendanceDetailAdapter
import com.axelliant.hris.adapter.SubFilterAdapter
import com.axelliant.hris.base.BaseFragment
import com.axelliant.hris.callback.AdapterItemClick
import com.axelliant.hris.config.GlobalConfig
import com.axelliant.hris.core.constants.AppRouteArgs
import com.axelliant.hris.databinding.FragmentMyAttendanceDetailBinding
import com.axelliant.hris.enums.AttendanceFilter
import com.axelliant.hris.enums.AttendanceFilter.Custom
import com.axelliant.hris.enums.AttendanceFilter.MONTH
import com.axelliant.hris.enums.AttendanceFilter.WEEK
import com.axelliant.hris.event.EventObserver
import com.axelliant.hris.extention.showErrorMsg
import com.axelliant.hris.model.attendance.AttendanceDetail
import com.axelliant.hris.model.attendance.AttendanceInput
import com.axelliant.hris.model.dashboard.FilterModel
import com.axelliant.hris.utils.Utils
import com.axelliant.hris.viewmodel.AttendanceViewModel
import com.google.android.material.datepicker.MaterialDatePicker
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.axelliant.hris.ui.designsystem.adapters.FilterAdapter
import com.axelliant.hris.ui.designsystem.adapters.FilterItem
import com.axelliant.hris.utils.Utils.utcToLocalDate
import dagger.hilt.android.AndroidEntryPoint
import java.util.Date

@AndroidEntryPoint
class MyAttendanceDetailFragment : BaseFragment() {

    private var startDateString: String? = null
    private var endDateString: String? = null
    private var _binding: FragmentMyAttendanceDetailBinding? = null
    private val binding get() = _binding
    private var currentFilter = WEEK
    private val attendanceViewModel: AttendanceViewModel by viewModels()

    private var emplId = ""
    private var filterId = ""
    private var attendanceList = arrayListOf<AttendanceDetail>()
    private var filterList = arrayListOf<FilterModel>()
    private lateinit var dateFilterAdapter: FilterAdapter

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
            emplId = bundle.getString(AppRouteArgs.EMPLOYEE_ID, GlobalConfig.currentEmployeeId())
        }

        setupRecyclerViews()

        attendanceViewModel.getIsLoading()
            .observe(viewLifecycleOwner, EventObserver { isLoading ->
                if (isLoading) {
                    showDialog()
                } else {
                    hideDialog()
                }
            })

        attendanceViewModel.getAttendanceDetail(getCurrentObject())

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

        binding?.appTopBar?.setOnBackClickListener {
            previousFragmentNavigation()
        }
    }

    private fun setupRecyclerViews() {
        binding?.rvAttendanceFilters?.layoutManager = LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL, false)
        binding?.rvAttendanceDetail?.layoutManager = LinearLayoutManager(requireContext())
        setupDateFilterBar()
        binding?.tvFromDate?.setOnClickListener { datePickerDialog() }
        binding?.tvToDate?.setOnClickListener { datePickerDialog() }
    }

    private fun dataPopulate(attendanceData: ArrayList<AttendanceDetail>) {
        attendanceList = attendanceData
        renderContent()
    }

    private fun subFilterPopulations(attendanceStatusList: ArrayList<FilterModel>) {
        filterList = arrayListOf<FilterModel>().apply {
            add(FilterModel().apply {
                id = ""
                title = "All"
                count = "0"
            })
            addAll(attendanceStatusList)
        }
        renderContent()
    }

    private fun renderContent() {
        binding?.tvNoRecord?.isVisible = attendanceList.isEmpty()
        binding?.rvAttendanceDetail?.isVisible = attendanceList.isNotEmpty()
        binding?.rvAttendanceFilters?.adapter = SubFilterAdapter(
            filterId,
            filterList,
            requireContext(),
            object : AdapterItemClick {
                override fun onItemClick(customObject: Any, position: Int) {
                    filterId = (customObject as FilterModel).id.orEmpty()
                    attendanceViewModel.getAttendanceDetail(getCurrentObject())
                }
            }
        )
        binding?.rvAttendanceDetail?.adapter = MyAttendanceDetailAdapter(
            attendanceList,
            requireContext(),
            object : AdapterItemClick {
                override fun onItemClick(customObject: Any, position: Int) = Unit
            }
        )
    }

    private fun setupDateFilterBar() {
        val items = listOf(
            FilterItem(getString(R.string.last_seven), 0),
            FilterItem(getString(R.string.this_month), 1),
            FilterItem(getString(R.string.custom), 2)
        )
        dateFilterAdapter = FilterAdapter(items, selectedPosition = 0) { position, _ ->
            when (position) {
                0 -> {
                    currentFilter = WEEK
                    dateFilterAdapter.setSelected(position)
                    attendanceViewModel.getAttendanceDetail(getCurrentObject())
                }
                1 -> {
                    currentFilter = MONTH
                    dateFilterAdapter.setSelected(position)
                    attendanceViewModel.getAttendanceDetail(getCurrentObject())
                }
                2 -> datePickerDialog()
            }
        }
        binding?.rvDateFilters?.apply {
            layoutManager = GridLayoutManager(requireContext(), items.size)
            adapter = dateFilterAdapter
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
                endDateString = Utils.getServerFormat(date = Utils.getLastDayOfMonth())
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
            binding?.tvFromDate?.text = startDateString
            binding?.tvToDate?.text = endDateString
        }
    }


    private fun datePickerDialog() {
        val builder = MaterialDatePicker.Builder.dateRangePicker()
        builder.setTheme(R.style.MyDatePickerTheme)
        builder.setTitleText("Select a date range")

        val datePicker = builder.build()
        datePicker.addOnPositiveButtonClickListener { selection ->
            val startDate = utcToLocalDate(selection.first)
            val endDate = utcToLocalDate(selection.second)

            startDateString = Utils.getServerFormat(date = startDate)
            endDateString = Utils.getServerFormat(date = endDate)

            setDateView()

            currentFilter = AttendanceFilter.Custom
            dateFilterAdapter.setSelected(2)
            attendanceViewModel.getAttendanceDetail(getCurrentObject())
        }

        datePicker.show(activity?.supportFragmentManager!!, "DATE_PICKER")
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
