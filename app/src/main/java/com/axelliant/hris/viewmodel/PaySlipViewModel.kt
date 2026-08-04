package com.axelliant.hris.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.axelliant.hris.model.PayslipModel

class PayslipViewModel : ViewModel() {

    val payslips = MutableLiveData<List<PayslipModel>>()

    fun loadPayslips() {

        payslips.value = listOf(
            PayslipModel("January Payslip","2 MB","","2026-01-01"),
            PayslipModel("February Payslip","1.5 MB","","2026-02-01"),
            PayslipModel("March Payslip","1.9 MB","","2026-03-01"),
            PayslipModel("April Payslip","2.3 MB","","2026-03-01"),
            PayslipModel("May Payslip","0.98 MB","","2026-03-01"),
            PayslipModel("June Payslip","0.76 MB","","2026-03-01"),
            PayslipModel("July Payslip","1.1 MB","","2026-03-01"),
            PayslipModel("August Payslip","1.7 MB","","2026-03-01"),
            PayslipModel("September Payslip","0.12 MB","","2026-03-01"),
            PayslipModel("October Payslip","786 kB","","2026-03-01"),
            PayslipModel("November Payslip","2.1 MB","","2026-03-01"),
            PayslipModel("December Payslip","1.2 MB","","2026-03-01")


        )
    }
}