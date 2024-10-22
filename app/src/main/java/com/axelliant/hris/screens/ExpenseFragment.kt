package com.axelliant.hris.screens

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.axelliant.hris.R
import com.axelliant.hris.adapter.ExpenseAdapter
import com.axelliant.hris.adapter.SubFilterAdapter
import com.axelliant.hris.base.BaseFragment
import com.axelliant.hris.callback.AdapterItemClick
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
                if (isLoading) {
                    showDialog()
                } else {
                    hideDialog()
                }
            })



        binding?.ivBack?.setOnClickListener {
            previousFragmentNavigation()
        }
        eventSelection()
        expenseViewModel.getMyExpenseDetail(getCurrentObject())


        expenseViewModel.expenseResponse.observe(
            viewLifecycleOwner,
            EventObserver { response ->

                if (response?.meta?.status == true && response.expenses != null) {
                    subFilterPopulations(response.expense_status)

                    if (response.expenses.size > 0) {
                        binding?.rvExpense?.visibility=View.VISIBLE
                        binding?.tvNoRecord?.visibility=View.GONE

                        dataPopulate(response.expenses)
                    } else {
                        binding?.rvExpense?.visibility=View.GONE
                        binding?.tvNoRecord?.visibility=View.VISIBLE
                    }

                } else {
                    requireContext().showErrorMsg(response?.meta?.message.toString())
                }

            })


        binding?.addExpense?.setOnClickListener {
            AppNavigator.navigateToAddExpenseFragment()
        }
    }


    private fun dataPopulate(expenseList: ArrayList<Expense>?) {

        binding?.rvExpense?.layoutManager = LinearLayoutManager(requireActivity())
        val expenseAdapter = ExpenseAdapter(
            expenseList!!, requireContext(), object : AdapterItemClick {
                override fun onItemClick(customObject: Any, position: Int) {

                    val expense = customObject as Expense
                    if (expense.status == "Draft") {
                        AppNavigator.navigateToAddExpenseFragment(Bundle().apply {
                            this.putString(AppConst.ExpenseRequestIDParam, expense.name)
                            this.putString(
                                AppConst.ExpenseRequestParam,
                                Gson().toJson(expense.expenses_detail)
                            )
                            this.putString(
                                AppConst.ExpenseRequestAttachments,
                                Gson().toJson(expense.attachments)
                            )
                        })
                    } else {
                        requireContext().showErrorMsg(
                            ErrorMessages.DRAFT_EXPENSE_ONLY.errorString.plus(
                                expense.approval_status
                            )
                        )
                    }


                }

            }
        )
        binding?.rvExpense?.adapter = expenseAdapter
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
                binding?.tvWeek?.background =
                    ContextCompat.getDrawable(requireContext(), R.drawable.rounded_enabled)
                binding?.tvWeek?.setTextColor(requireContext().getColor(R.color.white))

            }

            AttendanceFilter.MONTH -> {

                binding?.tvMonth?.background =
                    ContextCompat.getDrawable(requireContext(), R.drawable.rounded_enabled)
                binding?.tvMonth?.setTextColor(requireContext().getColor(R.color.white))
            }

            AttendanceFilter.Custom -> {

                binding?.tvCustom?.background =
                    ContextCompat.getDrawable(requireContext(), R.drawable.rounded_enabled)
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
                endDateString =
                    Utils.getServerFormat(date = Utils.getLastDayOfMonth())

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
            expenseViewModel.getMyExpenseDetail(getCurrentObject())
            eventSelection()
        }

        // Showing the date picker dialog
        datePicker.show(activity?.supportFragmentManager!!, "DATE_PICKER")
    }

    private fun subFilterPopulations(leaveStatus: ArrayList<FilterModel>?) {

        leaveStatus?.add(0, FilterModel().apply {
            this.id = ""
            this.title = "All"
            this.count = "0"
        })

        binding?.rvSubFilter?.layoutManager =
            LinearLayoutManager(requireActivity(), RecyclerView.HORIZONTAL, false)
        val weeklyAdapter = SubFilterAdapter(
            filterId,
            leaveStatus!!, requireContext(),
            object : AdapterItemClick {
                override fun onItemClick(customObject: Any, position: Int) {
                    val filterObject = customObject as FilterModel


                    filterId = filterObject.id.toString()
                    expenseViewModel.getMyExpenseDetail(getCurrentObject())
                }

            }
        )
        binding?.rvSubFilter?.adapter = weeklyAdapter

    }


}