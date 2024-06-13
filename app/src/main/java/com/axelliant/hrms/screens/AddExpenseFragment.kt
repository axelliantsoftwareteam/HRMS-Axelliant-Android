package com.axelliant.hrms.screens

import android.os.Bundle
import android.os.Handler
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.axelliant.hrms.R
import com.axelliant.hrms.adapter.AddExpenseAdapter
import com.axelliant.hrms.adapter.ExpenseAdapter
import com.axelliant.hrms.callback.AdapterItemClick
import com.axelliant.hrms.config.AppConst
import com.axelliant.hrms.databinding.FragmentAddExpenseBinding
import com.axelliant.hrms.databinding.FragmentRequestBinding
import com.axelliant.hrms.enums.RequestFilter
import com.axelliant.hrms.event.EventObserver
import com.axelliant.hrms.extention.nullToEmpty
import com.axelliant.hrms.extention.showErrorMsg
import com.axelliant.hrms.extention.showSuccessMsg
import com.axelliant.hrms.model.checkin.CheckInDetail
import com.axelliant.hrms.model.dashboard.FilterModel
import com.axelliant.hrms.model.expense.AddExpense
import com.axelliant.hrms.model.expense.Expense
import com.axelliant.hrms.model.leave.LeaveDetail
import com.axelliant.hrms.model.post.LeaveRequest
import com.axelliant.hrms.navigation.AppNavigator
import com.google.gson.Gson

class AddExpenseFragment : Fragment(), AddExpenseAdapter.OnUpdateList {

    private var grandTotal: Int = 0
    private var _binding: FragmentAddExpenseBinding? = null
    private val binding get() = _binding
    private lateinit var addExpenseList: ArrayList<AddExpense>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentAddExpenseBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding?.btnApply?.setOnClickListener {
            grandTotal=0
            for (addExpense in addExpenseList) {

                grandTotal = grandTotal?.plus(addExpense.amount!!)!!
            }
            binding?.tvAmount?.text = grandTotal.toString()
        }

        binding?.ivBack?.setOnClickListener {
            AppNavigator.moveBackToPreviousFragment()
        }

        // Initial item list with one item
        addExpenseList = arrayListOf(AddExpense().apply {
            this.expense_type = null
            this.expense_date = null
            this.amount = null
        })

        binding?.rvLeaveCount?.layoutManager = LinearLayoutManager(requireActivity())
        val addExpenseAdapter = AddExpenseAdapter(
            addExpenseList, requireContext(), object : AdapterItemClick {
                override fun onItemClick(customObject: Any, position: Int) {
                    // Handle item click if needed
                }
            },
            this // Pass the fragment as the OnUpdateList implementation
        )
        binding?.rvLeaveCount?.adapter = addExpenseAdapter

        binding?.tvAddNew?.setOnClickListener {
            addExpenseList.add(AddExpense().apply {
                this.expense_type = ""
                this.expense_date = ""
                this.amount = null
            })
            addExpenseAdapter.notifyItemInserted(addExpenseList.size - 1)
            binding?.rvLeaveCount?.scrollToPosition(addExpenseList.size - 1)
        }

    }

    override fun onListUpdated(updatedList: ArrayList<AddExpense>) {
        // Handle the updated list here
        addExpenseList = updatedList
        // You can also update your UI or perform other actions if needed
    }
}
