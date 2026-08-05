package com.axelliant.hris.screens

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.core.content.ContextCompat
import com.axelliant.hris.R
import com.axelliant.hris.base.BaseFragment
import com.axelliant.hris.callback.AdapterItemClick
import com.axelliant.hris.components.CheckInListComponent
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
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import java.util.Date

@AndroidEntryPoint
class CheckInListFragment : BaseFragment() {

    private var filterIdList: ArrayList<String>? = null
    private var filterId: String = ""
    private var startDateString: String? = null
    private var endDateString: String? = null
    private var _binding: FragmentCheckInListBinding? = null
    private val binding get() = _binding
    private var currentFilter = WEEK
    private var selectedChipId: String = ""
    private var subFilters: ArrayList<FilterModel> = arrayListOf()
    private var checkInList: ArrayList<CheckInDetail> = arrayListOf()
    private var noRecord: Boolean = false
    private val attendanceViewModel: AttendanceViewModel by viewModels()

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
                if (isLoading) showDialog() else hideDialog()
            })

        attendanceViewModel.getCheckInList(getCurrentObject())
        eventSelection()

        attendanceViewModel.checkInListResponse.observe(
            viewLifecycleOwner,
            EventObserver { response ->
                if (response?.meta?.status == true) {
                    response.checkin_status?.let {
                        it.add(0, FilterModel().apply {
                            this.id = ""
                            this.title = "All"
                            this.count = "0"
                        })
                        subFilters = it
                    }
                    checkInList = response.checkin ?: arrayListOf()
                    noRecord = checkInList.isEmpty()
                    renderCompose()
                } else {
                    requireContext().showErrorMsg(response?.meta?.message.toString())
                }
            })

        binding?.ivBack?.setOnClickListener {
            previousFragmentNavigation()
        }
    }

    private fun renderCompose() {
        binding?.composeCheckInList?.setContent {
            CheckInListComponent(
                checkInList = checkInList,
                subFilters = subFilters,
                startDate = startDateString ?: "",
                endDate = endDateString ?: "",
                selectedChipId = selectedChipId,
                noRecord = noRecord,
                onChipSelected = { chip ->
                    selectedChipId = chip.id.toString()
                    filterId = selectedChipId
                    if (filterIdList == null) filterIdList = ArrayList()
                    filterIdList?.clear()
                    filterIdList?.add(filterId)
                    attendanceViewModel.getCheckInList(getCurrentObject(filterId))
                },
                onRowClick = { attendanceDetail ->
                    if (attendanceDetail.requeststatus == "Pending") {
                        AppNavigator.navigateToRequest(Bundle().apply {
                            this.putString(AppConst.RequestType, RequestFilter.ATTENDANCE.name)
                            this.putString(
                                AppConst.AttendanceRequestParam,
                                Gson().toJson(attendanceDetail)
                            )
                        })
                    } else {
                        requireContext().showErrorMsg(
                            ErrorMessages.CHECK_IN_PENDING_ONLY.toString().plus(" ")
                                .plus(attendanceDetail.requeststatus)
                        )
                    }
                }
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
                    ContextCompat.getDrawable(requireContext(), R.drawable.fluent_blue)
                binding?.tvWeek?.setTextColor(requireContext().getColor(R.color.white))
            }
            MONTH -> {
                binding?.tvMonth?.background =
                    ContextCompat.getDrawable(requireContext(), R.drawable.fluent_blue)
                binding?.tvMonth?.setTextColor(requireContext().getColor(R.color.white))
            }
            Custom -> {
                binding?.tvCustom?.background =
                    ContextCompat.getDrawable(requireContext(), R.drawable.fluent_blue)
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
            }
            MONTH -> {
                startDateString = Utils.getServerFormat(date = Utils.getFirstDayOfMonth())
                endDateString = Utils.getServerFormat(date = Utils.getLastDayOfMonth())
            }
            Custom -> {}
        }

        return if (status == LeaveStatus.APPROVED.value) {
            val statusList = listOf(
                "Approved", "approved", "Processed", "processed", "waiting", "Waiting"
            )
            LeaveCountInput().apply {
                this.startDate = startDateString!!
                this.endDate = endDateString!!
                this.filter = currentFilter
                this.filters = statusList
            }
        } else {
            LeaveCountInput().apply {
                this.startDate = startDateString!!
                this.endDate = endDateString!!
                this.filter = currentFilter
                this.filters = filterIdList
            }
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
            attendanceViewModel.getCheckInList(getCurrentObject())
            eventSelection()
        }

        datePicker.show(activity?.supportFragmentManager!!, "DATE_PICKER")
    }
}
