package com.axelliant.hris.screens.payslip

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.axelliant.hris.R
import com.axelliant.hris.adapter.PayslipAdapter
import com.axelliant.hris.navigation.AppNavigator
import com.axelliant.hris.viewmodel.PayslipViewModel

class PayslipFragment : Fragment(R.layout.fragment_payslip) {

    private lateinit var adapter: PayslipAdapter
    private lateinit var viewModel: PayslipViewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<ImageView>(R.id.iv_back).setOnClickListener {
            AppNavigator.moveBackToPreviousFragment()
        }

        val recycler = view.findViewById<RecyclerView>(R.id.rv_payslips)

        adapter = PayslipAdapter(emptyList())

        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.adapter = adapter

        viewModel = ViewModelProvider(this)[PayslipViewModel::class.java]

        viewModel.payslips.observe(viewLifecycleOwner) {
            adapter.updateData(it)
        }

        viewModel.loadPayslips()
    }
}