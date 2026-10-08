package com.axelliant.hris.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.axelliant.hris.R
import com.axelliant.hris.databinding.ExpItemClaimBinding
import com.axelliant.hris.model.requests.ExpenseClaim
import com.axelliant.hris.ui.WsFormat
import com.axelliant.hris.ui.WsStatus
import com.axelliant.hris.ui.WsTone

/**
 * The employee's expense claims as accordion cards: one is expanded at a time and shows its line items,
 * attachments and, for a draft claim, Delete and Edit.
 */
class ExpenseAdapter(
    private val onEdit: (ExpenseClaim) -> Unit,
    private val onDelete: (ExpenseClaim) -> Unit,
    private val onPreview: (ExpenseClaim, Int) -> Unit
) : ListAdapter<ExpenseClaim, ExpenseAdapter.VH>(ClaimDiff) {

    private var expandedName: String? = null

    /** Collapses whichever card is open. */
    fun collapseAll() {
        val previous = indexOfName(expandedName)
        expandedName = null
        if (previous >= 0) notifyItemChanged(previous, PAYLOAD_EXPANSION)
    }

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
        val binding = ExpItemClaimBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        val holder = VH(binding)
        binding.expHeader.setOnClickListener {
            val position = holder.bindingAdapterPosition
            if (position != RecyclerView.NO_POSITION) toggle(position)
        }
        binding.expBtnDelete.setOnClickListener {
            val position = holder.bindingAdapterPosition
            if (position != RecyclerView.NO_POSITION) onDelete(getItem(position))
        }
        binding.expBtnEdit.setOnClickListener {
            val position = holder.bindingAdapterPosition
            if (position != RecyclerView.NO_POSITION) onEdit(getItem(position))
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

    class VH(private val binding: ExpItemClaimBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bindHeader(claim: ExpenseClaim) {
            val ctx = binding.root.context
            WsStatus.iconBadge(binding.expIcon, WsTone.expense(ctx))
            binding.expDate.text = WsFormat.date(claim.posting_date)
            WsStatus.badge(binding.expStatus, claim.displayStatus(ctx.getString(R.string.exp_unknown_status)))
            binding.expAmount.text = claim.amountText()
        }

        fun bindExpansion(claim: ExpenseClaim, expanded: Boolean, onPreview: (ExpenseClaim, Int) -> Unit) {
            binding.expChevron.setImageResource(if (expanded) R.drawable.ws_ic_expand_less else R.drawable.ws_ic_expand_more)
            binding.expDetail.root.visibility = if (expanded) View.VISIBLE else View.GONE
            binding.expActions.visibility = if (expanded && claim.isDraft) View.VISIBLE else View.GONE
            if (expanded) {
                ExpenseDetailBinder.bind(binding.expDetail, claim) { index -> onPreview(claim, index) }
            }
        }
    }

    private companion object {
        const val PAYLOAD_EXPANSION = "expansion"
    }
}

private object ClaimDiff : DiffUtil.ItemCallback<ExpenseClaim>() {
    override fun areItemsTheSame(oldItem: ExpenseClaim, newItem: ExpenseClaim) = oldItem.name == newItem.name
    override fun areContentsTheSame(oldItem: ExpenseClaim, newItem: ExpenseClaim) = oldItem == newItem
}
