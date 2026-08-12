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
import com.axelliant.hris.adapter.MyExpenseAdapter
import com.axelliant.hris.adapter.SubFilterAdapter
import com.axelliant.hris.base.BaseFragment
import com.axelliant.hris.callback.AdapterItemClick
import com.axelliant.hris.core.constants.AppRouteArgs
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
import dagger.hilt.android.AndroidEntryPoint
import java.util.Date

@AndroidEntryPoint
class ExpenseFragment : BaseFragment() {

    private var _binding: FragmentExpenseBinding? = null
    private val binding get() = _binding
    private var currentFilter = AttendanceFilter.WEEK
    private val expenseViewModel: ExpenseViewModel by viewModels()
    private var startDateString: String? = null
    private var endDateString: String? = null
    private var filterId = ""
    private var selectedFilterId = ""
    private var expenseList: List<Expense> = emptyList()
    private var filterList: List<FilterModel> = emptyList()

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

        binding?.appTopBar?.setOnBackClickListener { previousFragmentNavigation() }

        setupRecyclerViews()
        eventSelection()
        expenseViewModel.getMyExpenseDetail(getCurrentObject())

        expenseViewModel.expenseResponse.observe(
            viewLifecycleOwner,
            EventObserver { response ->
                if (response?.meta?.status == true && response.expenses != null) {
                    subFilterPopulations(response.expense_status)
                    expenseList = response.expenses
                    renderContent()
                } else {
                    requireContext().showErrorMsg(response?.meta?.message.toString())
                }
            })

        binding?.addExpense?.setOnClickListener {
            AppNavigator.navigateToAddExpenseFragment()
        }
    }

    private fun setupRecyclerViews() {
        binding?.rvExpenseFilters?.layoutManager =
            LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL, false)
        binding?.rvExpenses?.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun renderContent() {
        binding?.tvFromDate?.text = startDateString.orEmpty()
        binding?.tvToDate?.text = endDateString.orEmpty()
        binding?.tvToDate?.setOnClickListener { datePickerDialog() }

        binding?.rvExpenseFilters?.isVisible = filterList.isNotEmpty()
        binding?.rvExpenseFilters?.adapter = SubFilterAdapter(
            selectedFilterId,
            filterList,
            requireContext(),
            object : AdapterItemClick {
                override fun onItemClick(customObject: Any, position: Int) {
                    val filter = customObject as FilterModel
                    selectedFilterId = filter.id.toString()
                    filterId = selectedFilterId
                    expenseViewModel.getMyExpenseDetail(getCurrentObject())
                }
            }
        )

        binding?.rvExpenses?.isVisible = expenseList.isNotEmpty()
        binding?.tvNoRecord?.isVisible = expenseList.isEmpty()
        binding?.rvExpenses?.adapter = MyExpenseAdapter(
            expenseList,
            requireContext(),
            object : AdapterItemClick {
                override fun onItemClick(customObject: Any, position: Int) {
                    onExpenseClicked(customObject as Expense)
                }
            }
        )
    }

    private fun onExpenseClicked(expense: Expense) {
        if (expense.status == "Draft") {
            AppNavigator.navigateToAddExpenseFragment(Bundle().apply {
                putString(AppRouteArgs.EXPENSE_REQUEST_ID, expense.name)
                putString(AppRouteArgs.EXPENSE_REQUEST, Gson().toJson(expense.expenses_detail))
                putString(AppRouteArgs.EXPENSE_REQUEST_ATTACHMENTS, Gson().toJson(expense.attachments))
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
            currentFilter = AttendanceFilter.Custom
            datePickerDialog()
            eventSelection()
        }

        when (currentFilter) {
            AttendanceFilter.WEEK -> {
                binding?.tvWeek?.background = ContextCompat.getDrawable(requireContext(), R.drawable.fluent_blue)
                binding?.tvWeek?.setTextColor(requireContext().getColor(R.color.ds_neutral_white))
            }
            AttendanceFilter.MONTH -> {
                binding?.tvMonth?.background = ContextCompat.getDrawable(requireContext(), R.drawable.fluent_blue)
                binding?.tvMonth?.setTextColor(requireContext().getColor(R.color.ds_neutral_white))
            }
            AttendanceFilter.Custom -> {
                binding?.tvCustom?.background = ContextCompat.getDrawable(requireContext(), R.drawable.fluent_blue)
                binding?.tvCustom?.setTextColor(requireContext().getColor(R.color.ds_neutral_white))
            }
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
            AttendanceFilter.Custom -> {}
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
            binding?.tvFromDate?.text = startDateString.orEmpty()
            binding?.tvToDate?.text = endDateString.orEmpty()
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
            leaveStatus?.let {
                addAll(it.filterNot { filter ->
                    filter.id.orEmpty().isBlank() && filter.title.equals("All", ignoreCase = true)
                })
            }
        }
        filterList = list
        if (selectedFilterId.isBlank()) selectedFilterId = ""
    }
}
