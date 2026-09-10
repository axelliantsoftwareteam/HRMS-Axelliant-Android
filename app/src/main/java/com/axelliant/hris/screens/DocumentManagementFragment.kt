package com.axelliant.hris.screens

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.axelliant.hris.R
import com.axelliant.hris.adapter.DocumentRequestAdapter
import com.axelliant.hris.adapter.SubFilterAdapter
import com.axelliant.hris.base.BaseFragment
import com.axelliant.hris.callback.AdapterItemClick
import com.axelliant.hris.databinding.DialogRequestDocumentBinding
import com.axelliant.hris.databinding.FragmentDocumentManagementBinding
import com.axelliant.hris.enums.AttendanceFilter
import com.axelliant.hris.event.EventObserver
import com.axelliant.hris.extention.showErrorMsg
import com.axelliant.hris.model.attendance.AttendanceInput
import com.axelliant.hris.model.dashboard.FilterModel
import com.axelliant.hris.model.documentRequest.CreateDocument
import com.axelliant.hris.model.documentRequest.DocumentForm
import com.axelliant.hris.model.leave.ExpenseApprovalStatus
import com.axelliant.hris.utils.Utils
import com.axelliant.hris.viewmodel.ExpenseViewModel
import com.google.android.material.datepicker.MaterialDatePicker
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import java.util.Date
import androidx.recyclerview.widget.GridLayoutManager
import com.axelliant.hris.extention.hideShimmer
import com.axelliant.hris.extention.showShimmer
import com.axelliant.hris.ui.designsystem.adapters.FilterAdapter
import com.axelliant.hris.ui.designsystem.adapters.FilterItem


@AndroidEntryPoint
class DocumentManagementFragment : BaseFragment() {

    private var _binding: FragmentDocumentManagementBinding? = null
    private val binding get() = _binding
    private var currentFilter = AttendanceFilter.WEEK
    private val expenseViewModel: ExpenseViewModel by viewModels()
    private var startDateString: String? = null
    private var endDateString: String? = null
    private var filterId = ""

    private lateinit var dateFilterAdapter: FilterAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentDocumentManagementBinding.inflate(inflater).also { _binding = it }
        binding?.lyContent?.isVisible = false
        binding?.shimmerLayout?.showShimmer(binding?.lyContent!!)
        return binding?.root
    }

    private var isDataLoaded = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Show dialog spinner ONLY for subsequent fetches (filter switches)
        expenseViewModel.getIsLoading()
            .observe(viewLifecycleOwner, EventObserver { isLoading ->
                if (isDataLoaded) {
                    if (isLoading) showDialog() else hideDialog()
                }
            })

        // 2. Start initial shimmer only on first launch
        if (!isDataLoaded) {
            toggleShimmer(true)
        }

        binding?.appTopBar?.setOnBackClickListener {
            previousFragmentNavigation()
        }
        setupDateFilterBar()
        expenseViewModel.getDocumentReqDetail(getCurrentObject())

        expenseViewModel.documentResponse.observe(
            viewLifecycleOwner,
            EventObserver { response ->
                toggleShimmer(false)
                isDataLoaded = true

                if (response?.meta?.status == true && response.employee_forms != null) {
                    subFilterPopulations(response.employee_form_status)

                    if (response.employee_forms.size > 0) {
                        binding?.rvExpense?.visibility = View.VISIBLE
                        binding?.tvNoRecord?.visibility = View.GONE

                        dataPopulate(response.employee_forms)
                    } else {
                        binding?.rvExpense?.visibility = View.GONE
                        binding?.tvNoRecord?.visibility = View.VISIBLE
                    }

                } else {
                    requireContext().showErrorMsg(response?.meta?.message.toString())
                }
            })

        binding?.addExpense?.setOnClickListener {
            customDialog(requireContext(), expenseViewModel)
        }
    }

    private fun toggleShimmer(show: Boolean) {
        if (show) {
            binding?.shimmerLayout?.showShimmer(binding?.lyContent!!)
        } else {
            binding?.shimmerLayout?.hideShimmer(binding?.lyContent!!)
        }
    }

    private fun customDialog(
        context: Context,
        expenseViewModel: ExpenseViewModel
    ): AlertDialog {
        val dialogBinding = DialogRequestDocumentBinding.inflate(LayoutInflater.from(context))
        dialogBinding.lifecycleOwner = this
        dialogBinding.expenseViewModel = expenseViewModel

        // Create AlertDialog.Builder instance with custom theme
        val dialog = AlertDialog.Builder(context, R.style.alert_dialog_round_corners).apply {
            setView(dialogBinding.root)
        }.create()


        expenseViewModel.subjectError.observe(viewLifecycleOwner) { error ->
            dialogBinding.tilDocId.error = error
        }

        expenseViewModel.descriptionError.observe(viewLifecycleOwner) { error ->
            dialogBinding.tilAttendanceReason.error = error
        }

        dialogBinding.llBack.setOnClickListener {
            dialog.dismiss()
        }
        dialogBinding.btnOkNo.setOnClickListener {
//            dialog.dismiss()
            expenseViewModel.postDocument()

        }
        expenseViewModel.expenseApprovalResponse.observe(
            viewLifecycleOwner,
            EventObserver { response ->
                if (response?.meta?.status == true) {
                    dialog.dismiss()
                    requireContext().showErrorMsg(response.status_message)
                    expenseViewModel.getDocumentReqDetail(getCurrentObject())

                } else {
                    requireContext().showErrorMsg(response?.meta?.message.toString())
                    dialog.dismiss()
                }
            }
        )
        dialog.show()
        return dialog
    }

    private fun dataPopulate(expenseList: ArrayList<DocumentForm>?) {

        binding?.rvExpense?.layoutManager = LinearLayoutManager(requireActivity())
        val expenseAdapter = DocumentRequestAdapter(
            expenseList!!, requireContext(), object : AdapterItemClick {
                override fun onItemClick(customObject: Any, position: Int) {
//                    if (expense.status == "Draft") {
//                        AppNavigator.navigateToAddExpenseFragment(Bundle().apply {
//                            this.putString(AppRouteArgs.EXPENSE_REQUEST_ID, expense.name)
//                            this.putString(
//                                AppRouteArgs.EXPENSE_REQUEST,
//                                Gson().toJson(expense.expenses_detail)
//                            )
//                            this.putString(
//                                AppRouteArgs.EXPENSE_REQUEST_ATTACHMENTS,
//                                Gson().toJson(expense.attachments)
//                            )
//                        })
//                    } else {
//                        requireContext().showErrorMsg(
//                            ErrorMessages.DRAFT_EXPENSE_ONLY.errorString.plus(
//                                expense.approval_status
//                            )
//                        )
//                    }


                }

            }
        )
        binding?.rvExpense?.adapter = expenseAdapter
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
        val builder = MaterialDatePicker.Builder.dateRangePicker()
        builder.setTitleText("Select a date range")
        builder.setTheme(R.style.MyDatePickerTheme)

        val datePicker = builder.build()
        datePicker.addOnPositiveButtonClickListener { selection ->
            val startDate = Utils.utcToLocalDate(selection.first)
            val endDate = Utils.utcToLocalDate(selection.second)

            startDateString = Utils.getServerFormat(date = startDate)
            endDateString = Utils.getServerFormat(date = endDate)

            setDateView()

            currentFilter = AttendanceFilter.Custom
            dateFilterAdapter.setSelected(2)
            expenseViewModel.getDocumentReqDetail(getCurrentObject())
        }

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
                    expenseViewModel.getDocumentReqDetail(getCurrentObject())
                }

            }
        )
        binding?.rvSubFilter?.adapter = weeklyAdapter

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
                    currentFilter = AttendanceFilter.WEEK
                    dateFilterAdapter.setSelected(position)
                    expenseViewModel.getDocumentReqDetail(getCurrentObject())
                }
                1 -> {
                    currentFilter = AttendanceFilter.MONTH
                    dateFilterAdapter.setSelected(position)
                    expenseViewModel.getDocumentReqDetail(getCurrentObject())
                }
                2 -> datePickerDialog()
            }
        }
        binding?.rvDateFilters?.apply {
            layoutManager = GridLayoutManager(requireContext(), items.size)
            adapter = dateFilterAdapter
        }
    }

    override fun onDestroyView() {
        binding?.shimmerLayout?.stopShimmer()
        super.onDestroyView()
        _binding = null
    }
}
