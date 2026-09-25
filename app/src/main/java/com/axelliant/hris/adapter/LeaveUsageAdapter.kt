package com.axelliant.hris.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.axelliant.hris.databinding.ItemLeaveUsageBinding
import com.axelliant.hris.model.leave.LeaveAllocation

/**
 * Renders the "LEAVE USAGE (USED / TOTAL)" grid inside an expanded quota card.
 * Each cell shows "<used>/<total>" with the leave type name beneath it.
 */
class LeaveUsageAdapter(
    private val allocations: List<LeaveAllocation>
) : RecyclerView.Adapter<LeaveUsageAdapter.UsageVH>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UsageVH {
        val binding = ItemLeaveUsageBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return UsageVH(binding)
    }

    override fun onBindViewHolder(holder: UsageVH, position: Int) {
        holder.bind(allocations[position])
    }

    override fun getItemCount(): Int = allocations.size

    class UsageVH(val binding: ItemLeaveUsageBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: LeaveAllocation) {
            binding.tvUsageValue.text =
                "${formatNumber(item.used)}/${formatNumber(item.total_leaves)}"
            binding.tvUsageLabel.text = item.name
        }

        /** Drop the ".0" for whole numbers so "7.0" shows as "7". */
        private fun formatNumber(value: Double): String {
            return if (value % 1.0 == 0.0) value.toInt().toString() else value.toString()
        }
    }
}
