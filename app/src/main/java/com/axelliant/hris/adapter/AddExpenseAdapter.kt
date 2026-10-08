package com.axelliant.hris.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.axelliant.hris.R
import com.axelliant.hris.databinding.ExpItemAddLineBinding
import com.axelliant.hris.model.requests.ExpenseLineDraft
import com.axelliant.hris.ui.WsFormat

/**
 * The expense lines of the create/edit screen. Each line edits its [ExpenseLineDraft] in place, so the
 * screen can read the form straight from its own list; the adapter only reports what needs the
 * screen (dialogs, total, delete).
 */
class AddExpenseAdapter(
    private val listener: Listener
) : ListAdapter<ExpenseLineDraft, AddExpenseAdapter.VH>(LineDiff) {

    interface Listener {
        fun onPickType(line: ExpenseLineDraft)
        fun onPickDate(line: ExpenseLineDraft)
        fun onDelete(line: ExpenseLineDraft)
        fun onAmountChanged()
    }

    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int): Long = getItem(position).uid

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ExpItemAddLineBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        val holder = VH(binding)

        fun current(): ExpenseLineDraft? {
            val position = holder.bindingAdapterPosition
            return if (position == RecyclerView.NO_POSITION) null else getItem(position)
        }

        binding.expEtType.setOnClickListener { current()?.let(listener::onPickType) }
        binding.expTilType.setEndIconOnClickListener { current()?.let(listener::onPickType) }
        binding.expEtDate.setOnClickListener { current()?.let(listener::onPickDate) }
        binding.expTilDate.setEndIconOnClickListener { current()?.let(listener::onPickDate) }
        binding.expLineDelete.setOnClickListener { current()?.let(listener::onDelete) }

        binding.expEtAmount.doAfterTextChanged { text ->
            if (holder.binding) return@doAfterTextChanged
            val line = current() ?: return@doAfterTextChanged
            line.amountText = text?.toString().orEmpty()
            if (line.amount > 0.0) binding.expTilAmount.error = null
            listener.onAmountChanged()
        }
        binding.expEtDescription.doAfterTextChanged { text ->
            if (holder.binding) return@doAfterTextChanged
            current()?.description = text?.toString().orEmpty()
        }
        return holder
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position), position)

    class VH(private val views: ExpItemAddLineBinding) : RecyclerView.ViewHolder(views.root) {

        /** True while [bind] fills the fields, so the text watchers ignore it. */
        var binding = false
            private set

        fun bind(line: ExpenseLineDraft, position: Int) {
            val ctx = views.root.context
            binding = true
            // The first line cannot be removed, so a claim always keeps at least one.
            views.expLineDelete.visibility = if (position > 0) View.VISIBLE else View.GONE
            views.expEtType.setText(line.type.orEmpty())
            views.expEtDate.setText(WsFormat.reversedDay(line.dateApi))
            if (views.expEtAmount.text?.toString() != line.amountText) views.expEtAmount.setText(line.amountText)
            if (views.expEtDescription.text?.toString() != line.description) views.expEtDescription.setText(line.description)
            views.expTilType.error =
                if (line.showErrors && line.type.isNullOrBlank()) ctx.getString(R.string.exp_err_type) else null
            views.expTilDate.error =
                if (line.showErrors && line.dateApi.isNullOrBlank()) ctx.getString(R.string.exp_err_date) else null
            views.expTilAmount.error =
                if (line.showErrors && line.amount <= 0.0) ctx.getString(R.string.exp_err_amount) else null
            binding = false
        }
    }
}

private object LineDiff : DiffUtil.ItemCallback<ExpenseLineDraft>() {
    override fun areItemsTheSame(oldItem: ExpenseLineDraft, newItem: ExpenseLineDraft) = oldItem.uid == newItem.uid

    // Lines are edited in place (old and new lists share the objects), so a line never needs a rebind
    // just because the list changed; the screen calls notifyItemChanged after a dialog sets a value.
    override fun areContentsTheSame(oldItem: ExpenseLineDraft, newItem: ExpenseLineDraft) = true
}
