package com.axelliant.hris.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.axelliant.hris.R
import com.axelliant.hris.callback.AdapterItemClick
import com.axelliant.hris.databinding.SubFilterRowBinding
import com.axelliant.hris.model.dashboard.FilterModel

/**
 * Horizontal status chips (iOS look): the selected chip is filled with the primary colour and white
 * text, the others are white with a grey stroke; a count bubble follows every chip except "All"
 * (the chip whose id is empty).
 */
class SubFilterAdapter(
    private var filterId: String? = null,
    private val list: List<FilterModel>,
    private val context: Context,
    private val itemClick: AdapterItemClick
) :
    RecyclerView.Adapter<SubFilterAdapter.AccountsVH>() {

    private val primary = ContextCompat.getColor(context, R.color.ws_primary)
    private val white = ContextCompat.getColor(context, R.color.white)
    private val black = ContextCompat.getColor(context, R.color.black)
    private val stroke = ContextCompat.getColor(context, R.color.ws_card_stroke)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AccountsVH {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = SubFilterRowBinding.inflate(layoutInflater, parent, false)
        return AccountsVH(binding)
    }

    override fun onBindViewHolder(holder: AccountsVH, position: Int) {
        val item = list[position]
        val selected = filterId == item.id
        val chip = holder.binding.lyWorkHome

        holder.binding.tvWorkFrom.text = item.title
        holder.binding.tvWorkFrom.setTextColor(if (selected) white else black)
        chip.setCardBackgroundColor(if (selected) primary else white)
        chip.strokeColor = if (selected) primary else stroke

        holder.binding.tvWorkTxt.isVisible = item.id != ""
        holder.binding.tvWorkTxt.text = item.count

        chip.setOnClickListener {
            @Suppress("DEPRECATION")
            val adapterPosition = holder.adapterPosition
            if (adapterPosition != RecyclerView.NO_POSITION) {
                val currentItem = list[adapterPosition]
                filterId = currentItem.id.toString()
                notifyDataSetChanged()
                itemClick.onItemClick(currentItem, adapterPosition)
            }
        }
    }

    override fun getItemCount(): Int {
        return list.size
    }

    class AccountsVH(val binding: SubFilterRowBinding) :
        RecyclerView.ViewHolder(binding.root)

}
