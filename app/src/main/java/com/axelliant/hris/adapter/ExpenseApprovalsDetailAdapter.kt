package com.axelliant.hris.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.axelliant.hris.R
import com.axelliant.hris.databinding.ExpItemApprovalBinding
import com.axelliant.hris.model.requests.ExpenseClaim
import com.axelliant.hris.ui.WsFormat
import com.axelliant.hris.ui.WsStatus
import com.axelliant.hris.ui.WsTone

/**
 * The team's expense claims for a manager: accordion cards (one open at a time) with the line items,
 * attachments and, while a claim is pending, Reject and Approve.
 */
class ExpenseApprovalsDetailAdapter(
    private val onDecision: (claim: ExpenseClaim, approve: Boolean) -> Unit,
    private val onPreview: (ExpenseClaim, Int) -> Unit
) : ListAdapter<ExpenseClaim, ExpenseApprovalsDetailAdapter.VH>(ApprovalDiff) {

    private var expandedName: String? = null

    private fun indexOfName(name: String?): Int =
        if (name == null) -1 else currentList.indexOfFirst { it.name == name }

    private fun toggle(position: Int) {
        val claim = getItem(position)
        val previous = indexOfName(expandedName)
        expandedName = if (expandedName == claim.name) null else claim.name
        if (previous >= 0) notifyItemChanged(previous, PAYLOAD_EXPANSION)
        if (previous != position) notifyItemChanged(position, PAYLOAD_EXPANSION)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ExpItemApprovalBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        val holder = VH(binding)
        binding.expApprHeaderRow.setOnClickListener {
            val position = holder.bindingAdapterPosition
            if (position != RecyclerView.NO_POSITION) toggle(position)
        }
        binding.expApprBtnReject.setOnClickListener {
            val position = holder.bindingAdapterPosition
            if (position != RecyclerView.NO_POSITION) onDecision(getItem(position), false)
        }
        binding.expApprBtnApprove.setOnClickListener {
            val position = holder.bindingAdapterPosition
            if (position != RecyclerView.NO_POSITION) onDecision(getItem(position), true)
        }
        return holder
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val claim = getItem(position)
        holder.bindHeader(claim)
        holder.bindExpansion(claim, claim.name == expandedName, onPreview)
    }

    override fun onBindViewHolder(holder: VH, position: Int, payloads: MutableList<Any>) {
        if (payloads.isEmpty()) {
            onBindViewHolder(holder, position)
        } else {
            val claim = getItem(position)
            holder.bindExpansion(claim, claim.name == expandedName, onPreview)
        }
    }

    class VH(private val binding: ExpItemApprovalBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bindHeader(claim: ExpenseClaim) {
            val ctx = binding.root.context
            WsStatus.iconBadge(binding.expApprIcon, WsTone.expense(ctx))
            binding.expApprName.text = claim.employee_name?.takeIf { it.isNotBlank() } ?: claim.name.orEmpty()
            binding.expApprLabel.text = ctx.getString(R.string.exp_appr_label_fmt, WsFormat.date(claim.posting_date))
            WsStatus.badge(binding.expApprStatus, claim.displayStatus(ctx.getString(R.string.exp_unknown_status)))
            binding.expApprAmount.text = claim.amountText()
        }

        fun bindExpansion(claim: ExpenseClaim, expanded: Boolean, onPreview: (ExpenseClaim, Int) -> Unit) {
            val pending = claim.displayStatus().equals("Pending", ignoreCase = true)
            binding.expApprChevron.setImageResource(
                if (expanded) R.drawable.ws_ic_expand_less else R.drawable.ws_ic_expand_more
            )
            binding.expDetail.root.visibility = if (expanded) View.VISIBLE else View.GONE
            binding.expApprActions.visibility = if (expanded && pending) View.VISIBLE else View.GONE
            if (expanded) {
                ExpenseDetailBinder.bind(binding.expDetail, claim) { index -> onPreview(claim, index) }
            }
        }
    }

    private companion object {
        const val PAYLOAD_EXPANSION = "expansion"
    }
}

private object ApprovalDiff : DiffUtil.ItemCallback<ExpenseClaim>() {
    override fun areItemsTheSame(oldItem: ExpenseClaim, newItem: ExpenseClaim) = oldItem.name == newItem.name
    override fun areContentsTheSame(oldItem: ExpenseClaim, newItem: ExpenseClaim) = oldItem == newItem
}
