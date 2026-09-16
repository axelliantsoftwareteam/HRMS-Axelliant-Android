package com.axelliant.hris.screens

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.axelliant.hris.R
import com.axelliant.hris.adapter.CheckInListAdapter
import com.axelliant.hris.adapter.SubFilterAdapter
import com.axelliant.hris.base.BaseFragment
import com.axelliant.hris.callback.AdapterItemClick
import com.axelliant.hris.core.constants.AppRouteArgs
import com.axelliant.hris.databinding.FragmentCheckInListBinding
import com.axelliant.hris.enums.AttendanceFilter
import com.axelliant.hris.enums.AttendanceFilter.Custom
import com.axelliant.hris.enums.AttendanceFilter.MONTH
import com.axelliant.hris.enums.AttendanceFilter.WEEK
import com.axelliant.hris.enums.LeaveStatus
import com.axelliant.hris.enums.RequestFilter
import com.axelliant.hris.event.EventObserver
import com.axelliant.hris.extention.hideShimmer
import com.axelliant.hris.extention.showErrorMsg
import com.axelliant.hris.extention.showShimmer
import com.axelliant.hris.model.attendance.LeaveCountInput
import com.axelliant.hris.model.checkin.CheckInDetail
import com.axelliant.hris.model.dashboard.FilterModel
import com.axelliant.hris.navigation.AppNavigator
import com.axelliant.hris.network.ErrorMessages
import com.axelliant.hris.ui.designsystem.adapters.FilterAdapter
import com.axelliant.hris.ui.designsystem.adapters.FilterItem
import com.axelliant.hris.utils.Utils
import com.axelliant.hris.utils.Utils.utcToLocalDate
import com.axelliant.hris.viewmodel.AttendanceViewModel
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

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
    private lateinit var dateFilterAdapter: FilterAdapter


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentCheckInListBinding.inflate(inflater).also { _binding = it }
        binding?.lyContent?.isVisible = false
        binding?.shimmerLayout?.showShimmer(binding?.lyContent!!)
        return binding?.root
    }

    private var isDataLoaded = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerViews()

        // 1. Dialog spinner only triggers after initial shimmer load completes
        attendanceViewModel.getIsLoading()
            .observe(viewLifecycleOwner, EventObserver { isLoading ->
                if (isDataLoaded) {
                    if (isLoading) showDialog() else hideDialog()
                }
            })

        // 2. Trigger initial shimmer
        if (!isDataLoaded) {
            toggleShimmer(true)
        }

        attendanceViewModel.getCheckInList(getCurrentObject())
        setupDateFilterBar()

        attendanceViewModel.checkInListResponse.observe(
            viewLifecycleOwner,
            EventObserver { response ->
                // 3. Dismiss shimmer and reveal content layout
                toggleShimmer(false)
                isDataLoaded = true

                if (response?.meta?.status == true) {
                    response.checkin_status?.let { filters ->
                        subFilters = arrayListOf<FilterModel>().apply {
                            add(FilterModel().apply {
                                id = ""
                                title = "All"
                                count = "0"
                            })
                            addAll(filters.filterNot {
                                it.id.orEmpty().isBlank() && it.title.equals("All", ignoreCase = true)
                            })
                        }
                    } ?: run {
                        subFilters = arrayListOf(
                            FilterModel().apply {
                                this.id = ""
                                this.title = "All"
                                this.count = "0"
                            }
                        )
                    }
                    checkInList = response.checkin ?: arrayListOf()
                    noRecord = checkInList.isEmpty()
                    renderContent()
                } else {
                    requireContext().showErrorMsg(response?.meta?.message.toString())
                }
            })

        binding?.appTopBar?.setOnBackClickListener {
            previousFragmentNavigation()
        }
    }

    private fun toggleShimmer(show: Boolean) {
        binding?.shimmerLayout?.apply {
            if (show) startShimmer() else stopShimmer()
            isVisible = show
        }
        binding?.lyContent?.isVisible = !show
    }

    override fun onDestroyView() {
        binding?.shimmerLayout?.stopShimmer()
        super.onDestroyView()
        _binding = null
    }

    private fun setupRecyclerViews() {
        binding?.rvSubFilters?.layoutManager =
            LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL, false)
        binding?.rvCheckInList?.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun renderContent() {
        binding?.tvFromDate?.text = startDateString.orEmpty()
        binding?.tvToDate?.text = endDateString.orEmpty()
        binding?.tvPresentCount?.text = getPresentDaysCount().toString()
        binding?.tvWeekendCount?.text = getWeekendDaysCount().toString()
        binding?.tvTotalHours?.text = "%.1f".format(getTotalWorkingHours())

        binding?.rvSubFilters?.isVisible = subFilters.isNotEmpty()
        binding?.rvSubFilters?.adapter = SubFilterAdapter(
            selectedChipId,
            subFilters,
            requireContext(),
            object : AdapterItemClick {
                override fun onItemClick(customObject: Any, position: Int) {
                    val chip = customObject as FilterModel
                    selectedChipId = chip.id.toString()
                    filterId = selectedChipId
                    if (filterIdList == null) filterIdList = ArrayList()
                    filterIdList?.clear()
                    filterIdList?.add(filterId)
                    attendanceViewModel.getCheckInList(getCurrentObject(filterId))
                }
            }
        )

        binding?.rvCheckInList?.isVisible = !noRecord
        binding?.tvNoRecord?.isVisible = noRecord
        binding?.rvCheckInList?.adapter = CheckInListAdapter(
            checkInList,
            requireContext(),
            object : AdapterItemClick {
                override fun onItemClick(customObject: Any, position: Int) {
                    val attendanceDetail = customObject as CheckInDetail
                    openAttendanceRequest(attendanceDetail)
                }
            }
        )
    }

    private fun openAttendanceRequest(attendanceDetail: CheckInDetail) {
        if (attendanceDetail.requeststatus == LeaveStatus.PENDING.value) {
            AppNavigator.navigateToRequest(Bundle().apply {
                putString(AppRouteArgs.REQUEST_TYPE, RequestFilter.ATTENDANCE.name)
                putString(AppRouteArgs.ATTENDANCE_REQUEST, Gson().toJson(attendanceDetail))
            })
        } else {
            requireContext().showErrorMsg(
                ErrorMessages.CHECK_IN_PENDING_ONLY.toString().plus(" ")
                    .plus(attendanceDetail.requeststatus)
            )
        }
    }

    private fun getPresentDaysCount(): Int {
        return checkInList.mapNotNull { it.dateKey() }.distinct().size
    }

    private fun getTotalWorkingHours(): Double {
        return checkInList
            .groupBy { it.dateKey().orEmpty() }
            .values
            .sumOf { entries -> entries.maxOfOrNull { it.working_hours } ?: 0.0 }
    }

    private fun getWeekendDaysCount(): Int {
        val start = startDateString?.toDate() ?: return 0
        val end = endDateString?.toDate() ?: return 0
        val entryDates = checkInList.mapNotNull { it.dateKey() }.toSet()
        val calendar = Calendar.getInstance().apply { time = start }
        var count = 0

        while (!calendar.time.after(end)) {
            val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
            val dateKey = serverDateFormat().format(calendar.time)
            if ((dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY) &&
                !entryDates.contains(dateKey)
            ) {
                count++
            }
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        return count
    }

    private fun CheckInDetail.dateKey(): String? {
        return time.trim().split(Regex("\\s+")).firstOrNull()?.takeIf { it.isNotBlank() }
    }

    private fun String.toDate(): Date? {
        return runCatching { serverDateFormat().parse(this) }.getOrNull()
    }

    private fun serverDateFormat(): SimpleDateFormat {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
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
                    attendanceViewModel.getCheckInList(getCurrentObject())
                }
                1 -> {
                    currentFilter = MONTH
                    dateFilterAdapter.setSelected(position)
                    attendanceViewModel.getCheckInList(getCurrentObject())
                }
                2 -> datePickerDialog()
            }
        }
        binding?.rvDateFilters?.apply {
            layoutManager = GridLayoutManager(requireContext(), items.size)
            adapter = dateFilterAdapter
        }
    }

    private fun getCurrentObject(
        status: String? = null
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
            val startDate = utcToLocalDate(selection.first)
            val endDate = utcToLocalDate(selection.second)

            startDateString = Utils.getServerFormat(date = startDate)
            endDateString = Utils.getServerFormat(date = endDate)

            currentFilter = AttendanceFilter.Custom
            dateFilterAdapter.setSelected(2)
            attendanceViewModel.getCheckInList(getCurrentObject())
        }

        datePicker.show(activity?.supportFragmentManager!!, "DATE_PICKER")
    }

}
