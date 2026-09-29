package com.axelliant.hris.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

import com.axelliant.hris.R
import com.axelliant.hris.databinding.RemaningLeaveRowBinding
import com.axelliant.hris.model.leave.LeaveType

class RemainingLeaveAdapter(
    private val leaves: ArrayList<LeaveType>
) :
    RecyclerView.Adapter<RemainingLeaveAdapter.AccountsVH>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AccountsVH {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = RemaningLeaveRowBinding.inflate(layoutInflater, parent, false)
        return AccountsVH(binding)
    }

    override fun onBindViewHolder(holder: AccountsVH, position: Int) {
        holder.bind(leaves[position])
    }

    override fun getItemCount(): Int {
        return leaves.size
    }

    class AccountsVH(val binding: RemaningLeaveRowBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: LeaveType) {
            binding.tvTitle.text = item.title
            binding.tvValue.text = item.value.toString()

            val context = binding.root.context
            val title = item.title?.lowercase().orEmpty()

            val (iconRes, bgRes, tintColor) = when {
                title.contains("sick") -> Triple(
                    R.drawable.ic_calendar_leave,
                    R.drawable.ic_bg_circle_green,
                    R.color.green
                )
                title.contains("casual") -> Triple(
                    R.drawable.ic_umbrella,
                    R.drawable.ic_bg_circle_purple,
                    R.color.purple
                )
                title.contains("bereavement") -> Triple(
                    R.drawable.ic_leaf,
                    R.drawable.ic_bg_circle_pink,
                    R.color.red
                )
                else -> Triple(
                    R.drawable.ic_calendar_leave,
                    R.drawable.ic_bg_circle_blue,
                    R.color.ds_primary
                )
            }

            binding.ivIcon.setImageResource(iconRes)
            binding.ivIcon.setBackgroundResource(bgRes)
            binding.ivIcon.imageTintList =
                android.content.res.ColorStateList.valueOf(context.getColor(tintColor))
        }
    }

}