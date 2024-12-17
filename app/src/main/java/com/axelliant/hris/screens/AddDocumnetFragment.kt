package com.axelliant.hris.screens

import android.Manifest
import android.os.Bundle
import android.os.Handler
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.axelliant.hris.R
import com.axelliant.hris.adapter.AddExpenseAdapter
import com.axelliant.hris.adapter.AttachmentsAdapter
import com.axelliant.hris.base.BaseFragment
import com.axelliant.hris.callback.AdapterItemClick
import com.axelliant.hris.config.AppConst
import com.axelliant.hris.databinding.FragmentAddDocumnetBinding
import com.axelliant.hris.databinding.FragmentAddExpenseBinding
import com.axelliant.hris.event.EventObserver
import com.axelliant.hris.extention.showErrorMsg
import com.axelliant.hris.extention.showSuccessMsg
import com.axelliant.hris.model.expense.AddExpense
import com.axelliant.hris.model.expense.Attachments
import com.axelliant.hris.model.expense.CreateExpense
import com.axelliant.hris.model.expense.DeleteAttachment
import com.axelliant.hris.model.expense.ImageType
import com.axelliant.hris.model.leave.SpinnerType
import com.axelliant.hris.navigation.AppNavigator
import com.axelliant.hris.utils.Utils.getServerFormat
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken


class AddDocumnetFragment : BaseFragment() {

    private var _binding: FragmentAddDocumnetBinding? = null
    private val binding get() = _binding


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentAddDocumnetBinding.inflate(inflater, container, false)
        return binding?.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

//
//        if (arguments != null && requireArguments().containsKey(AppConst.ExpenseRequestParam)) {
//            val parsedData = arguments?.getString(AppConst.ExpenseRequestParam, "")
//            val expenseID = arguments?.getString(AppConst.ExpenseRequestIDParam, "")
//            val attachments = arguments?.getString(AppConst.ExpenseRequestAttachments, "")
//
//            if (parsedData != null) {
//                forUpdateList =
//                    Gson().fromJson(parsedData, object : TypeToken<List<AddExpense>>() {}.type)
//            }
//
//
//        }
//
//        binding?.tvDate?.text = getServerFormat()
//
//        expenseViewModel.getIsLoading()
//            .observe(viewLifecycleOwner, EventObserver { isLoading ->
//                if (isLoading) {
//                    showDialog()
//                } else {
//                    hideDialog()
//                }
//            })
//
//        expenseViewModel.expenseTypeResponse.observe(
//            viewLifecycleOwner,
//            EventObserver { response ->
//
//                if (response?.meta?.status == true)
//                {
//                    if (response.expenses != null)
//                    {
//
//                    }
//
//                } else {
//                    requireContext().showErrorMsg(response?.meta?.message.toString())
//                }
//
//            })
//
//        binding?.btnApply?.setOnClickListener {
//                if (expenseItem.expense_type == expenseType) {
//                    requireContext().showErrorMsg("Please select the type")
//                    return@setOnClickListener
//                } else if (expenseItem.expense_date == null) {
//                    requireContext().showErrorMsg("Please choose expense date")
//                    return@setOnClickListener
//                } else if (expenseItem.amount == null || expenseItem.amount == 0.0) {
//                    requireContext().showErrorMsg("Please enter expense amount")
//                    return@setOnClickListener
//                } else if (expenseItem.description == null || expenseItem.description.equals("")) {
//                    requireContext().showErrorMsg("Please add expense reason")
//                    return@setOnClickListener
//                }
//
//            expenseViewModel.postExpense(isUpdate, CreateExpense().apply {
//                this.expense_details = addExpenseList
//                this.posting_date = getServerFormat()
//                this.total_amount = addExpenseAdapter?.grandTotalCalculation().toString()
//            })
//
//        }
//
//        binding?.ivBack?.setOnClickListener {
//            AppNavigator.moveBackToPreviousFragment()
//        }

    }


}