package com.axelliant.hris.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.axelliant.hris.R
import com.axelliant.hris.databinding.RowLeaveQuotaBinding
import com.axelliant.hris.extention.setUrlImage
import com.axelliant.hris.model.leave.QuotaEmployee

/**
 * Team "Leave Quota" list. Each row is an expandable member card:
 * collapsed shows avatar / name / (You chip) / designation / "<n> available" badge,
 * expanded reveals the used-vs-total usage grid.
 */
class LeaveQuotaAdapter(
    private val mContext: Context,
    private val allEmployees: List<QuotaEmployee>
) : RecyclerView.Adapter<LeaveQuotaAdapter.QuotaVH>() {

    // Working (filtered) list shown by the RecyclerView.
    private val employees: ArrayList<QuotaEmployee> = ArrayList(allEmployees)

    // Track expanded rows by employee id so state survives rebinds.
    private val expanded = HashSet<String>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QuotaVH {
        val binding = RowLeaveQuotaBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return QuotaVH(binding)
    }

    override fun onBindViewHolder(holder: QuotaVH, position: Int) {
        holder.bind(employees[position], mContext)

        val key = employees[position].employee ?: position.toString()
        val isOpen = expanded.contains(key)
        holder.binding.lyUsage.isVisible = isOpen
        holder.binding.ivChevron.setImageResource(
            if (isOpen) R.drawable.ic_up else R.drawable.ic_down
        )

        holder.binding.root.setOnClickListener {
            val pos = holder.adapterPosition
            if (pos == RecyclerView.NO_POSITION) return@setOnClickListener
            val k = employees[pos].employee ?: pos.toString()
            if (expanded.contains(k)) expanded.remove(k) else expanded.add(k)
            notifyItemChanged(pos)
        }
    }

    override fun getItemCount(): Int = employees.size

    /** Filter the list by employee name (case-insensitive). */
    fun filter(query: String?) {
        val q = query?.trim()?.lowercase().orEmpty()
        employees.clear()
        if (q.isEmpty()) {
            employees.addAll(allEmployees)
        } else {
            employees.addAll(allEmployees.filter {
                it.employee_name?.lowercase()?.contains(q) == true
            })
        }
        notifyDataSetChanged()
    }

    class QuotaVH(val binding: RowLeaveQuotaBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: QuotaEmployee, context: Context) {
            binding.tvEmployeName.text = item.employee_name
            binding.tvDesignation.text = item.designation
            binding.tvYouChip.isVisible = item.is_self
            binding.tvAvailableBadge.text =
                context.getString(
                    R.string.available_badge,
                    formatNumber(item.totalAvailable)
                )
            binding.profileImg.setUrlImage(item.image, context)

            val allocations = item.leaves?.leave_allocation ?: arrayListOf()
            binding.rvUsage.layoutManager = GridLayoutManager(context, 3)
            binding.rvUsage.adapter = LeaveUsageAdapter(allocations)
            binding.rvUsage.isNestedScrollingEnabled = false
        }

        private fun formatNumber(value: Double): String {
            return if (value % 1.0 == 0.0) value.toInt().toString() else value.toString()
        }
    }
}
