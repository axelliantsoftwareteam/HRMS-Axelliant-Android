package com.axelliant.hris.screens

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.axelliant.hris.R
import com.axelliant.hris.adapter.MyLeaveDetailAdapter
import com.axelliant.hris.adapter.SubFilterAdapter
import com.axelliant.hris.base.BaseFragment
import com.axelliant.hris.callback.AdapterItemClick
import com.axelliant.hris.core.constants.AppRouteArgs
import com.axelliant.hris.databinding.FragmentMyLeaveDetailBinding
import com.axelliant.hris.enums.AttendanceFilter
import com.axelliant.hris.enums.RequestFilter
import com.axelliant.hris.event.EventObserver
import com.axelliant.hris.extention.showErrorMsg
import com.axelliant.hris.model.attendance.AttendanceInput
import com.axelliant.hris.model.dashboard.FilterModel
import com.axelliant.hris.model.leave.LeaveDetail
import com.axelliant.hris.navigation.AppNavigator
import com.axelliant.hris.network.ErrorMessages
import com.axelliant.hris.utils.Utils
import com.axelliant.hris.viewmodel.LeaveViewModel
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import java.util.Date

@AndroidEntryPoint
class MyLeaveDetailFragment : BaseFragment() {

    private var _binding: FragmentMyLeaveDetailBinding? = null
    private val binding get() = _binding
    private var currentFilter = AttendanceFilter.WEEK
    private val leaveViewModel: LeaveViewModel by viewModels()
    private var startDateString: String? = null
    private var endDateString: String? = null

    private var filterId = ""
    private var leaveList = arrayListOf<LeaveDetail>()
    private var filterList = arrayListOf<FilterModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentMyLeaveDetailBinding.inflate(inflater).also { _binding = it }
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerViews()

        leaveViewModel.getIsLoading()
            .observe(viewLifecycleOwner, EventObserver { isLoading ->
                if (isLoading) showDialog() else hideDialog()
            })

        binding?.appTopBar?.setOnBackClickListener {
            previousFragmentNavigation()
        }

        leaveViewModel.getMyLeaveDetail(getCurrentObject())
        eventSelection()

        leaveViewModel.myLeaveDetailResponse.observe(
            viewLifecycleOwner,
            EventObserver { response ->
                if (response?.meta?.status == true) {
                    response.leave_status?.let { subFilterPopulations(it) }
                    dataPopulate(response.leaves)
                } else {
                    requireContext().showErrorMsg(response?.meta?.message.toString())
                }
            })
    }

    private fun setupRecyclerViews() {
        binding?.rvLeaveFilters?.layoutManager =
            LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL, false)
        binding?.rvLeaveDetail?.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun subFilterPopulations(leaveStatus: ArrayList<FilterModel>) {
        filterList = arrayListOf<FilterModel>().apply {
            add(FilterModel().apply {
                id = ""
                title = "All"
                count = "0"
            })
            addAll(leaveStatus)
        }
        renderContent()
    }

    private fun dataPopulate(leaves: ArrayList<LeaveDetail>?) {
        leaveList = leaves ?: arrayListOf()
        renderContent()
    }

    private fun renderContent() {
        binding?.tvNoRecord?.isVisible = leaveList.isEmpty()
        binding?.rvLeaveDetail?.isVisible = leaveList.isNotEmpty()
        binding?.rvLeaveFilters?.adapter = SubFilterAdapter(
            filterId,
            filterList,
            requireContext(),
            object : AdapterItemClick {
                override fun onItemClick(customObject: Any, position: Int) {
                    filterId = (customObject as FilterModel).id.orEmpty()
                    leaveViewModel.getMyLeaveDetail(getCurrentObject())
                }
            }
        )
        binding?.rvLeaveDetail?.adapter = MyLeaveDetailAdapter(
            leaveList,
            requireContext(),
            object : AdapterItemClick {
                override fun onItemClick(customObject: Any, position: Int) {
                    openLeaveRequest(customObject as LeaveDetail)
                }
            }
        )
    }

    private fun openLeaveRequest(leaveDetail: LeaveDetail) {
        if (leaveDetail.status == "Open") {
            AppNavigator.navigateToRequest(Bundle().apply {
                this.putString(AppRouteArgs.REQUEST_TYPE, RequestFilter.LEAVE.name)
                this.putString(AppRouteArgs.LEAVE_REQUEST, Gson().toJson(leaveDetail))
            })
        } else {
            requireContext().showErrorMsg(
                ErrorMessages.OPEN_LEAVES_ONLY.errorString.plus(leaveDetail.status)
            )
        }
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
            leaveViewModel.getMyLeaveDetail(getCurrentObject())
            eventSelection()
        }
        binding?.tvMonth?.setOnClickListener {
            currentFilter = AttendanceFilter.MONTH
            leaveViewModel.getMyLeaveDetail(getCurrentObject())
            eventSelection()
        }
        binding?.tvCustom?.setOnClickListener {
            datePickerDialog()
        }

        when (currentFilter) {
            AttendanceFilter.WEEK -> {
                binding?.tvWeek?.background =
                    ContextCompat.getDrawable(requireContext(), R.drawable.fluent_blue)
                binding?.tvWeek?.setTextColor(requireContext().getColor(R.color.white))
            }
            AttendanceFilter.MONTH -> {
                binding?.tvMonth?.background =
                    ContextCompat.getDrawable(requireContext(), R.drawable.fluent_blue)
                binding?.tvMonth?.setTextColor(requireContext().getColor(R.color.white))
            }
            AttendanceFilter.Custom -> {
                binding?.tvCustom?.background =
                    ContextCompat.getDrawable(requireContext(), R.drawable.fluent_blue)
                binding?.tvCustom?.setTextColor(requireContext().getColor(R.color.white))
            }
        }
    }

    private fun getCurrentObject(): AttendanceInput {
        when (currentFilter) {
            AttendanceFilter.WEEK -> {
                startDateString = Utils.getServerFormat(date = Utils.getLastWeek())
                endDateString = Utils.getServerFormat()
            }
            AttendanceFilter.MONTH -> {
                startDateString = Utils.getServerFormat(date = Utils.getFirstDayOfMonth())
                endDateString = Utils.getServerFormat(date = Utils.getLastDayOfMonth())
            }
            AttendanceFilter.Custom -> {}
        }
        return AttendanceInput().apply {
            this.startDate = startDateString!!
            this.endDate = endDateString!!
            this.filter = currentFilter
            this.filters = filterId
        }
    }

    private fun datePickerDialog() {
        val builder = MaterialDatePicker.Builder.dateRangePicker()
        builder.setTitleText("Select a date range")
        builder.setTheme(R.style.MyDatePickerTheme)
        val datePicker = builder.build()
        datePicker.addOnPositiveButtonClickListener { selection ->
            val startDate = selection.first
            val endDate = selection.second
            startDateString = Utils.getServerFormat(date = Date(startDate))
            endDateString = Utils.getServerFormat(date = Date(endDate))
            currentFilter = AttendanceFilter.Custom
            leaveViewModel.getMyLeaveDetail(getCurrentObject())
            eventSelection()
        }
        datePicker.show(activity?.supportFragmentManager!!, "DATE_PICKER")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
