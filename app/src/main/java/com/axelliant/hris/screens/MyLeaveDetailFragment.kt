package com.axelliant.hris.screens

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.content.ContextCompat
import com.axelliant.hris.R
import com.axelliant.hris.base.BaseFragment
import com.axelliant.hris.components.LeaveDetailContent
import com.axelliant.hris.config.AppConst.LeaveRequestParam
import com.axelliant.hris.config.AppConst.RequestType
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
import androidx.fragment.app.viewModels
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

    private var leaveListState by mutableStateOf(listOf<LeaveDetail>())
    private var filterListState by mutableStateOf(listOf<FilterModel>())

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

        setupCompose()

        leaveViewModel.getIsLoading()
            .observe(viewLifecycleOwner, EventObserver { isLoading ->
                if (isLoading) showDialog() else hideDialog()
            })

        binding?.ivBack?.setOnClickListener {
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

    private fun setupCompose() {
        binding?.composeLeaveDetail?.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                LeaveDetailContent(
                    filters = filterListState,
                    selectedFilterId = filterId,
                    leaveList = leaveListState,
                    onFilterClick = { filter ->
                        filterId = filter.id.toString()
                        leaveViewModel.getMyLeaveDetail(getCurrentObject())
                    },
                    onEditClick = { leaveDetail ->
                        if (leaveDetail.status == "Open") {
                            AppNavigator.navigateToRequest(Bundle().apply {
                                this.putString(RequestType, RequestFilter.LEAVE.name)
                                this.putString(LeaveRequestParam, Gson().toJson(leaveDetail))
                            })
                        } else {
                            requireContext().showErrorMsg(
                                ErrorMessages.OPEN_LEAVES_ONLY.errorString.plus(leaveDetail.status)
                            )
                        }
                    }
                )
            }
        }
    }

    private fun subFilterPopulations(leaveStatus: ArrayList<FilterModel>) {
        leaveStatus.add(0, FilterModel().apply {
            this.id = ""
            this.title = "All"
            this.count = "0"
        })
        filterListState = leaveStatus
    }

    private fun dataPopulate(leaves: ArrayList<LeaveDetail>?) {
        leaveListState = leaves ?: arrayListOf()
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
            else -> {}
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
            else -> {}
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
