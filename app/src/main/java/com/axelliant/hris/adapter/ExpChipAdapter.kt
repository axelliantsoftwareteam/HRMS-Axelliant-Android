package com.axelliant.hris.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.axelliant.hris.R
import com.axelliant.hris.databinding.ExpItemChipBinding
import com.axelliant.hris.model.requests.ExpenseStatusChip

/** The status chips of the expense list ("All" plus one chip per status with its count). */
class ExpChipAdapter(
    private val onSelect: (ExpenseStatusChip) -> Unit
) : ListAdapter<ExpenseStatusChip, ExpChipAdapter.VH>(ChipDiff) {

    /** The id of the selected chip; "" is "All". */
    var selectedId: String = ""
        set(value) {
            if (field != value) {
                field = value
                notifyItemRangeChanged(0, itemCount)
            }
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ExpItemChipBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        val holder = VH(binding)
        binding.root.setOnClickListener {
            val position = holder.bindingAdapterPosition
            if (position != RecyclerView.NO_POSITION) onSelect(getItem(position))
        }
        return holder
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val chip = getItem(position)
        holder.bind(chip, chip.id.orEmpty() == selectedId)
    }

    class VH(private val binding: ExpItemChipBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(chip: ExpenseStatusChip, selected: Boolean) {
            val ctx = binding.root.context
            val allTitle = ctx.getString(R.string.exp_all)
            binding.expChipTitle.text = chip.title.orEmpty()
            binding.expChipTitle.setTextColor(ctx.getColor(if (selected) R.color.white else R.color.black))
            binding.expChip.setCardBackgroundColor(ctx.getColor(if (selected) R.color.ws_primary else R.color.white))
            val showCount = chip.title != allTitle && chip.id != null && chip.id.isNotEmpty()
            binding.expChipCount.visibility = if (showCount) View.VISIBLE else View.GONE
            binding.expChipCount.text = chip.count ?: "0"
        }
    }
}

private object ChipDiff : DiffUtil.ItemCallback<ExpenseStatusChip>() {
    override fun areItemsTheSame(oldItem: ExpenseStatusChip, newItem: ExpenseStatusChip) = oldItem.id == newItem.id
    override fun areContentsTheSame(oldItem: ExpenseStatusChip, newItem: ExpenseStatusChip) = oldItem == newItem
}
