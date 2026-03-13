package com.axelliant.hris.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.axelliant.hris.R
import com.axelliant.hris.callback.AdapterItemClick
import com.axelliant.hris.databinding.MyLeaveDetailRowBinding
import com.axelliant.hris.enums.LeaveStatus
import com.axelliant.hris.extention.valueQualifier
import com.axelliant.hris.model.leave.LeaveDetail

class MyLeaveDetailAdapter(
    private val leaves: ArrayList<LeaveDetail>,
    private val mContext: Context,
    private val adapterItemClick: AdapterItemClick
) :
    RecyclerView.Adapter<MyLeaveDetailAdapter.AccountsVH>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AccountsVH {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = MyLeaveDetailRowBinding.inflate(layoutInflater, parent, false)
        return AccountsVH(binding)

    }

    override fun onBindViewHolder(holder: AccountsVH, position: Int) {
        val item = leaves[position]
        holder.bind(item, mContext)


        holder.binding.tvEdit.setOnClickListener {
            val adapterPosition = holder.adapterPosition
            if (adapterPosition != RecyclerView.NO_POSITION) {
                adapterItemClick.onItemClick(leaves[adapterPosition], adapterPosition)
            }
        }

        holder.binding.tvDropDown.setOnClickListener {
            val adapterPosition = holder.adapterPosition
            if (adapterPosition != RecyclerView.NO_POSITION) {
                val currentItem = leaves[adapterPosition]
                currentItem.isDetailVisible = !currentItem.isDetailVisible
                notifyItemChanged(adapterPosition)
            }
        }

        when (item.status) {
            LeaveStatus.REJECTED.value -> {
                holder.binding.tvEdit.isVisible=false
                holder.binding.tvAttendStatus.backgroundTintList = ContextCompat.getColorStateList(mContext, R.color.light_red)
                holder.binding.tvAttendStatus.setTextColor(ContextCompat.getColorStateList(mContext, R.color.color_third))
            }
            LeaveStatus.APPROVED.value -> {
                holder.binding.tvEdit.isVisible=false
                holder.binding.tvAttendStatus.backgroundTintList = ContextCompat.getColorStateList(mContext, R.color.light_green)
                holder.binding.tvAttendStatus.setTextColor(ContextCompat.getColorStateList(mContext, R.color.green))
            }
            LeaveStatus.DRAFT.value -> {
                holder.binding.tvAttendStatus.backgroundTintList = ContextCompat.getColorStateList(mContext, R.color.light_yellow)
                holder.binding.tvAttendStatus.setTextColor(ContextCompat.getColorStateList(mContext, R.color.yellow))
            }
            LeaveStatus.Open.value -> {
                holder.binding.tvAttendStatus.backgroundTintList = ContextCompat.getColorStateList(mContext, R.color.light_yellow)
                holder.binding.tvAttendStatus.setTextColor(ContextCompat.getColorStateList(mContext, R.color.yellow))
            }
        }



    }

    override fun getItemCount(): Int {
        return leaves.size
    }

    class AccountsVH(val binding: MyLeaveDetailRowBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(leaveDetail: LeaveDetail, mContext: Context) {
            binding.fromDate.text = leaveDetail.from_date
            binding.toDate.text = leaveDetail.to_date
            binding.tvLeaveDays.text = leaveDetail.total_leave_days.toString()
            if (leaveDetail.is_paid) {
                binding.tvPaid.text = "Paid"
                binding.tvPaid.setTextColor(mContext.getColor(R.color.black))

            } else {
                binding.tvPaid.text = "Un Paid"
                binding.tvPaid.setTextColor(mContext.getColor(R.color.red))

            }
            binding.tvType.text = leaveDetail.leave_type

            binding.tvReason.text = leaveDetail.leave_reason.valueQualifier()


            binding.lyDropDown.isVisible = leaveDetail.isDetailVisible



            binding.tvAttendStatus.text = leaveDetail.status


        }
    }

}
