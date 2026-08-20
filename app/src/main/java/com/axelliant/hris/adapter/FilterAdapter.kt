package com.axelliant.hris.ui.designsystem.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.axelliant.hris.R
import com.axelliant.hris.databinding.ItemFilterPillBinding

data class FilterItem(val label: String, val id: Int)

class FilterAdapter(
    private val items: List<FilterItem>,
    private var selectedPosition: Int = 0,
    private val onFilterClick: (position: Int, item: FilterItem) -> Unit
) : RecyclerView.Adapter<FilterAdapter.FilterViewHolder>() {

    inner class FilterViewHolder(val binding: ItemFilterPillBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FilterViewHolder {
        val binding = ItemFilterPillBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return FilterViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FilterViewHolder, position: Int) {
        val item = items[position]
        holder.binding.tvFilterLabel.text = item.label

        val selected = position == selectedPosition
        holder.binding.tvFilterLabel.setBackgroundResource(
            if (selected) R.drawable.fluent_blue else R.drawable.rounded_disabled
        )
        holder.binding.tvFilterLabel.setTextAppearance(
            if (selected) R.style.Text_Fluent2_Caption_Inverse else R.style.Text_Fluent2_Caption_Primary
        )

        holder.binding.root.setOnClickListener {
            onFilterClick(position, item)
        }
    }

    override fun getItemCount() = items.size

    fun setSelected(position: Int) {
        val prev = selectedPosition
        selectedPosition = position
        notifyItemChanged(prev)
        notifyItemChanged(selectedPosition)
    }

    fun getSelected() = selectedPosition
}