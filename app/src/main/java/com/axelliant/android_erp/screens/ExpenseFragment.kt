package com.axelliant.android_erp.screens

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.axelliant.android_erp.R
import com.axelliant.android_erp.Test
import com.axelliant.android_erp.adapter.ExpenseAdapter
import com.axelliant.android_erp.base.BaseFragment
import com.axelliant.android_erp.callback.AdapterItemClick
import com.axelliant.android_erp.databinding.FragmentExpenseBinding
import com.axelliant.android_erp.extention.showSuccessMsg

enum class ExpenseEvents {
    Pending,
    Approved
}


class ExpenseFragment : BaseFragment() {
    private var expenseEvent = ExpenseEvents.Pending

    private var _binding: FragmentExpenseBinding? = null
    private val binding get() = _binding

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
        eventSelection()
    }

    private fun eventSelection() {

        dataPopulate()

        binding?.tvPending?.background =
            ContextCompat.getDrawable(requireContext(), R.drawable.rounded_disabled)

        binding?.tvTeamLeave?.background =
            ContextCompat.getDrawable(requireContext(), R.drawable.rounded_disabled)


        binding?.tvPending?.setTextColor(requireContext().getColor(R.color.btn_text_color))
        binding?.tvTeamLeave?.setTextColor(requireContext().getColor(R.color.btn_text_color))

        when (expenseEvent) {
            ExpenseEvents.Pending -> {
                binding?.tvPending?.background =
                    ContextCompat.getDrawable(requireContext(), R.drawable.rounded_enabled)
                binding?.tvPending?.setTextColor(requireContext().getColor(R.color.white))

            }

            ExpenseEvents.Approved -> {

                binding?.tvTeamLeave?.background =
                    ContextCompat.getDrawable(requireContext(), R.drawable.rounded_enabled)
                binding?.tvTeamLeave?.setTextColor(requireContext().getColor(R.color.white))
            }


        }

        binding?.tvPending?.setOnClickListener {
            expenseEvent = ExpenseEvents.Pending
            eventSelection()
        }


        binding?.tvTeamLeave?.setOnClickListener {
            expenseEvent = ExpenseEvents.Approved
            eventSelection()
        }

    }

    private fun dataPopulate() {
        binding?.rvExpense?.layoutManager = LinearLayoutManager(requireActivity())
        val expenseAdapter = ExpenseAdapter(
            listOf(
                Test("item1"),
                Test("item2"),
                Test("item3"),
                Test("item4"),
                Test("item5"),
                Test("item6")
            ),expenseEvent,
            object : AdapterItemClick {
                override fun onItemClick(customObject: Any, position: Int) {
                    val currentObject = customObject as Test
                    requireContext().showSuccessMsg(
                        currentObject.testString
                    )

                }

            })
        binding?.rvExpense?.adapter = expenseAdapter


    }
}