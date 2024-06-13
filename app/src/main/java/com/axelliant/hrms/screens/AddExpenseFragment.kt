package com.axelliant.hrms.screens

import android.R.attr.data
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.axelliant.hrms.adapter.AddExpenseAdapter
import com.axelliant.hrms.adapter.expenseType
import com.axelliant.hrms.base.BaseFragment
import com.axelliant.hrms.callback.AdapterItemClick
import com.axelliant.hrms.config.AppConst
import com.axelliant.hrms.databinding.FragmentAddExpenseBinding
import com.axelliant.hrms.event.EventObserver
import com.axelliant.hrms.extention.showErrorMsg
import com.axelliant.hrms.extention.showSuccessMsg
import com.axelliant.hrms.model.expense.AddExpense
import com.axelliant.hrms.model.expense.CreateExpense
import com.axelliant.hrms.model.leave.SpinnerType
import com.axelliant.hrms.navigation.AppNavigator
import com.axelliant.hrms.utils.Utils.getServerFormat
import com.axelliant.hrms.viewmodel.ExpenseViewModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.koin.android.ext.android.inject


class AddExpenseFragment : BaseFragment(), AddExpenseAdapter.OnUpdateList {


    private var isUpdate = false
    private var _binding: FragmentAddExpenseBinding? = null
    private val binding get() = _binding
    private lateinit var addExpenseList: ArrayList<AddExpense>
    private val expenseViewModel: ExpenseViewModel by inject()

    var addExpenseAdapter: AddExpenseAdapter? = null
    private var expenseList: ArrayList<SpinnerType> = arrayListOf()


    private var forUpdateList: ArrayList<AddExpense> = arrayListOf()


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentAddExpenseBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        if (arguments != null && requireArguments().containsKey(AppConst.ExpenseRequestParam)) {
            val parsedData = arguments?.getString(AppConst.ExpenseRequestParam, "")

            if (parsedData != null) {
                forUpdateList =
                    Gson().fromJson(parsedData!!, object : TypeToken<List<AddExpense>>() {}.type)

                isUpdate = true

            }


        }

        binding?.tvDate?.text = getServerFormat()

        expenseViewModel.getIsLoading()
            .observe(viewLifecycleOwner, EventObserver { isLoading ->
                if (isLoading) {
                    showDialog()
                } else {
                    hideDialog()
                }
            })


        expenseViewModel.postExpenseResponse.observe(
            viewLifecycleOwner,
            EventObserver { response ->


                if (response?.meta?.status == true) {
                    requireActivity().showSuccessMsg(response.status_message)
                    Handler().postDelayed({
                        // do stuff
                        AppNavigator.moveBackToPreviousFragment()
                    }, 200)
                } else {
                    requireContext().showErrorMsg(response?.meta?.message.toString())
                }

            })


        expenseViewModel.getExpenseTypeList()
        expenseViewModel.expenseTypeResponse.observe(
            viewLifecycleOwner,
            EventObserver { response ->

                if (response?.meta?.status == true) {


                    if (response.expenses != null) {

                        expenseList.add(0, SpinnerType().apply {
                            this.type = expenseType
                        })
                        expenseList.addAll(response.expenses!!)



                        if (isUpdate) {
                            for (counter in 0..<forUpdateList.size) {
                                forUpdateList[counter].expenseTypeList = expenseList
                            }

                            addExpenseList = forUpdateList

                        } else {
                            addExpenseList = arrayListOf(AddExpense().apply {
                                this.expense_type = null
                                this.expense_date = null
                                this.amount = null
                                this.expenseTypeList = expenseList
                            })

                        }

                        binding?.rvLeaveCount?.layoutManager =
                            LinearLayoutManager(requireActivity())
                        addExpenseAdapter = AddExpenseAdapter(
                            addExpenseList, requireContext(), object : AdapterItemClick {
                                override fun onItemClick(customObject: Any, position: Int) {
                                    // Handle item click if needed
                                }
                            },
                            this // Pass the fragment as the OnUpdateList implementation
                        )
                        binding?.rvLeaveCount?.adapter = addExpenseAdapter

                    }

                } else {
                    requireContext().showErrorMsg(response?.meta?.message.toString())
                }

            })


        binding?.btnApply?.setOnClickListener {

            for (expenseItem in addExpenseList) {

                if (expenseItem.expense_type == expenseType) {
                    requireContext().showErrorMsg("Please select the type")
                    return@setOnClickListener
                } else if (expenseItem.expense_date == null) {
                    requireContext().showErrorMsg("Please choose expense date")
                    return@setOnClickListener
                } else if (expenseItem.amount == null || expenseItem.amount == 0.0) {
                    requireContext().showErrorMsg("Please enter expense amount")
                    return@setOnClickListener
                } else if (expenseItem.description == null || expenseItem.description.equals("")) {
                    requireContext().showErrorMsg("Please add expense reason")
                    return@setOnClickListener
                }

            }

            // assume all good

            expenseViewModel.postExpense(CreateExpense().apply {
                this.expense_details = addExpenseList
                this.posting_date = getServerFormat()
                this.total_amount = grandTotalCalculation().toString()
            })

        }

        binding?.ivBack?.setOnClickListener {
            AppNavigator.moveBackToPreviousFragment()
        }


        // Initial item list with one item

        binding?.tvAddNew?.setOnClickListener {
            addExpenseList.add(AddExpense().apply {
                this.expense_type = null
                this.expense_date = null
                this.amount = null
                this.expenseTypeList = expenseList

            })
            addExpenseAdapter?.notifyItemInserted(addExpenseList.size - 1)
            binding?.rvLeaveCount?.scrollToPosition(addExpenseList.size - 1)
        }

    }


    override fun onListUpdated(updatedList: ArrayList<AddExpense>) {
        // Handle the updated list here
        addExpenseList = updatedList

        binding?.tvAmount?.text = grandTotalCalculation().toString()
    }


    private fun grandTotalCalculation(): Double {
        var total = 0.0
        for (addExpense in addExpenseList) {
            if (addExpense.amount != null) {
                total += addExpense.amount!!
            }

        }

        return total
    }
}
