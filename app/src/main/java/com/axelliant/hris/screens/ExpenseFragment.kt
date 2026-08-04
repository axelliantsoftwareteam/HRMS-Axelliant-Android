package com.axelliant.hris.screens

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.dimensionResource
import androidx.core.content.ContextCompat
import com.axelliant.hris.R
import com.axelliant.hris.base.BaseFragment
import com.axelliant.hris.components.DateRangeBox
import com.axelliant.hris.components.ExpenseEmptyState
import com.axelliant.hris.components.ExpenseFilterChipItem
import com.axelliant.hris.components.ExpenseListCard
import com.axelliant.hris.components.ExpenseRow
import com.axelliant.hris.config.AppConst
import com.axelliant.hris.databinding.FragmentExpenseBinding
import com.axelliant.hris.enums.AttendanceFilter
import com.axelliant.hris.event.EventObserver
import com.axelliant.hris.extention.showErrorMsg
import com.axelliant.hris.model.attendance.AttendanceInput
import com.axelliant.hris.model.dashboard.FilterModel
import com.axelliant.hris.model.expense.Expense
import com.axelliant.hris.navigation.AppNavigator
import com.axelliant.hris.network.ErrorMessages
import com.axelliant.hris.utils.Utils
import com.axelliant.hris.viewmodel.ExpenseViewModel
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.gson.Gson
import com.intuit.sdp.R as SdpR
import org.koin.android.ext.android.inject
import java.util.Date

class ExpenseFragment : BaseFragment() {

    private var _binding: FragmentExpenseBinding? = null
    private val binding get() = _binding
    private var currentFilter = AttendanceFilter.WEEK
    private val expenseViewModel: ExpenseViewModel by inject()
    private var startDateString: String? = null
    private var endDateString: String? = null
    private var filterId = ""

    // Compose-observed state
    private var expenseListState = mutableStateOf<List<Expense>>(emptyList())
    private var filterListState = mutableStateOf<List<FilterModel>>(emptyList())
    private var selectedFilterIdState = mutableStateOf("")
    private var startDateState = mutableStateOf("")
    private var endDateState = mutableStateOf("")

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentExpenseBinding.inflate(inflater).also { _binding = it }
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        expenseViewModel.getIsLoading()
            .observe(viewLifecycleOwner, EventObserver { isLoading ->
                if (isLoading) showDialog() else hideDialog()
            })

        binding?.ivBack?.setOnClickListener { previousFragmentNavigation() }

        setupCompose()
        eventSelection()
        expenseViewModel.getMyExpenseDetail(getCurrentObject())

        expenseViewModel.expenseResponse.observe(
            viewLifecycleOwner,
            EventObserver { response ->
                response?.expense_status?.forEach {
                    Log.d(
                        "ExpenseStatus",
                        "id=${it.id}, title=${it.title}, count=${it.count}"
                    )
                }
                if (response?.meta?.status == true && response.expenses != null) {
                    subFilterPopulations(response.expense_status)
                    expenseListState.value = response.expenses ?: emptyList()
                } else {
                    requireContext().showErrorMsg(response?.meta?.message.toString())
                }
            })

        binding?.addExpense?.setOnClickListener {
            AppNavigator.navigateToAddExpenseFragment()
        }
    }

    private fun setupCompose() {
        binding?.composeExpenseContent?.setContent {
            Column(Modifier.fillMaxSize()) {

                val startDate by startDateState
                val endDate by endDateState

                if (startDate.isNotBlank() && endDate.isNotBlank()) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(top = dimensionResource(SdpR.dimen._8sdp)),
                        horizontalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._8sdp))
                    ) {
                        DateRangeBox("From", startDate, Modifier.weight(1f))
                        DateRangeBox(
                            "To",
                            endDate,
                            Modifier.weight(1f),
                            onClick = { datePickerDialog() }
                        )
                    }
                }

                val filters by filterListState
                val selectedId by selectedFilterIdState
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._6sdp)),
                    contentPadding = PaddingValues(
                        vertical = dimensionResource(SdpR.dimen._6sdp)
                    )
                ) {
                    items(filters) { filter ->
                        ExpenseFilterChipItem(
                            filter = filter,
                            selected = filter.id.toString() == selectedId
                        ) {
                            selectedFilterIdState.value = filter.id.toString()
                            filterId = filter.id.toString()
                            expenseViewModel.getMyExpenseDetail(getCurrentObject())
                        }
                    }
                }

                Spacer(modifier = Modifier.height(dimensionResource(SdpR.dimen._10sdp)))

                val expenses by expenseListState
                if (expenses.isEmpty()) {
                    ExpenseEmptyState(getString(R.string.no_record_found))
                } else {
                    ExpenseListCard {
                        LazyColumn {
                            items(expenses) { expense ->
                                ExpenseRow(item = expense) {
                                    onExpenseClicked(expense)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun onExpenseClicked(expense: Expense) {
        if (expense.status == "Draft") {
            AppNavigator.navigateToAddExpenseFragment(Bundle().apply {
                putString(AppConst.ExpenseRequestIDParam, expense.name)
                putString(AppConst.ExpenseRequestParam, Gson().toJson(expense.expenses_detail))
                putString(AppConst.ExpenseRequestAttachments, Gson().toJson(expense.attachments))
            })
        } else {
            requireContext().showErrorMsg(
                ErrorMessages.DRAFT_EXPENSE_ONLY.errorString.plus(expense.approval_status)
            )
        }
    }

    private fun eventSelection() {
        binding?.tvWeek?.background = ContextCompat.getDrawable(requireContext(), R.drawable.rounded_disabled)
        binding?.tvMonth?.background = ContextCompat.getDrawable(requireContext(), R.drawable.rounded_disabled)
        binding?.tvCustom?.background = ContextCompat.getDrawable(requireContext(), R.drawable.rounded_disabled)

        binding?.tvWeek?.setTextColor(requireContext().getColor(R.color.btn_text_color))
        binding?.tvMonth?.setTextColor(requireContext().getColor(R.color.btn_text_color))
        binding?.tvCustom?.setTextColor(requireContext().getColor(R.color.btn_text_color))

        binding?.tvWeek?.setOnClickListener {
            currentFilter = AttendanceFilter.WEEK
            expenseViewModel.getMyExpenseDetail(getCurrentObject())
            eventSelection()
        }
        binding?.tvMonth?.setOnClickListener {
            currentFilter = AttendanceFilter.MONTH
            expenseViewModel.getMyExpenseDetail(getCurrentObject())
            eventSelection()
        }
        binding?.tvCustom?.setOnClickListener {
            datePickerDialog()
            currentFilter = AttendanceFilter.Custom
            expenseViewModel.getMyExpenseDetail(getCurrentObject())
            eventSelection()
        }

        when (currentFilter) {
            AttendanceFilter.WEEK -> {
                binding?.tvWeek?.background = ContextCompat.getDrawable(requireContext(), R.drawable.fluent_blue)
                binding?.tvWeek?.setTextColor(requireContext().getColor(R.color.white))
            }
            AttendanceFilter.MONTH -> {
                binding?.tvMonth?.background = ContextCompat.getDrawable(requireContext(), R.drawable.fluent_blue)
                binding?.tvMonth?.setTextColor(requireContext().getColor(R.color.white))
            }
            AttendanceFilter.Custom -> {
                binding?.tvCustom?.background = ContextCompat.getDrawable(requireContext(), R.drawable.fluent_blue)
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
                endDateString = Utils.getServerFormat(date = Utils.getLastDayOfMonth())
                setDateView()
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

    private fun setDateView() {
        if (startDateString != null && endDateString != null) {
            startDateState.value = startDateString!!
            endDateState.value = endDateString!!
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
            setDateView()
            currentFilter = AttendanceFilter.Custom
            expenseViewModel.getMyExpenseDetail(getCurrentObject())
            eventSelection()
        }
        datePicker.show(activity?.supportFragmentManager!!, "DATE_PICKER")
    }

    private fun subFilterPopulations(leaveStatus: ArrayList<FilterModel>?) {
        val list = ArrayList<FilterModel>().apply {
            add(FilterModel().apply {
                this.id = ""
                this.title = "All"
                this.count = "0"
            })
            leaveStatus?.let { addAll(it) }
        }
        filterListState.value = list
        if (selectedFilterIdState.value.isBlank()) selectedFilterIdState.value = ""
    }
}