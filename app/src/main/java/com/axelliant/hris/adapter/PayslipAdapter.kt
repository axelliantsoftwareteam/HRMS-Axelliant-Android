package com.axelliant.hris.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.axelliant.hris.R
import com.axelliant.hris.databinding.PayItemSlipBinding
import com.axelliant.hris.model.payslip.PayslipFormat
import com.axelliant.hris.model.payslip.SalarySlip
import com.axelliant.hris.ui.WsStatus
import com.axelliant.hris.ui.WsTone

/** Pay slip cards: icon, period, status and a "View" button that shows a spinner while its PDF downloads. */
class PayslipAdapter(private val onView: (SalarySlip) -> Unit) : FooterListAdapter<SalarySlip>() {

    /** Slip whose PDF is downloading; all View buttons are disabled while this is set. */
    var openingName: String? = null
        set(value) {
            if (field == value) return
            field = value
            refreshItems()
        }

    override fun isSameItem(old: SalarySlip, new: SalarySlip) = old.name == new.name

    override fun createItemHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        SlipHolder(PayItemSlipBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun bindItem(holder: RecyclerView.ViewHolder, item: SalarySlip) {
        (holder as SlipHolder).bind(item)
    }

    private inner class SlipHolder(private val binding: PayItemSlipBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(slip: SalarySlip) {
            val context = binding.root.context
            val period = PayslipFormat.period(slip)
            WsStatus.iconBadge(binding.payIcon, WsTone.payslip(context))
            binding.payPeriod.text = period
            WsStatus.badge(binding.payStatus, PayslipFormat.status(slip, context.getString(R.string.pay_default_status)))

            val opening = openingName
            val isOpening = opening != null && opening == slip.name
            binding.payViewIcon.visibility = if (isOpening) View.GONE else View.VISIBLE
            binding.payViewProgress.visibility = if (isOpening) View.VISIBLE else View.GONE
            binding.payViewButton.isEnabled = opening == null
            binding.payViewButton.alpha = if (opening == null || isOpening) 1f else 0.5f
            binding.payViewButton.contentDescription = context.getString(R.string.pay_view_cd, period)
            binding.payViewButton.setOnClickListener { onView(slip) }
        }
    }
}
