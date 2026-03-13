package com.axelliant.hris.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.axelliant.hris.callback.AdapterItemClick
import com.axelliant.hris.databinding.MyTeamLeaveRowBinding
import com.axelliant.hris.extention.setUrlImage
import com.axelliant.hris.model.leave.TeamLeaveDetail

class TeamLeaveDetailAdapter(
    private val mContext: Context,
    private val leaves: ArrayList<TeamLeaveDetail>,
    private val isForApproval: Boolean = false,
    private val approvedClick: AdapterItemClick? = null,
    private val rejectClick: AdapterItemClick? = null
) :
    RecyclerView.Adapter<TeamLeaveDetailAdapter.AccountsVH>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AccountsVH {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = MyTeamLeaveRowBinding.inflate(layoutInflater, parent, false)
        return AccountsVH(binding)
    }

    override fun onBindViewHolder(holder: AccountsVH, position: Int) {
        val item = leaves[position]
        holder.bind(item, mContext)
        holder.binding.lyDropDown.isVisible = false

        if (isForApproval) {
            holder.binding.btnDivider.isVisible = true
            holder.binding.tvApproved.isVisible = true
            holder.binding.tvReject.isVisible = true
            holder.binding.tvApproved.setOnClickListener {
                val adapterPosition = holder.adapterPosition
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    approvedClick?.onItemClick(leaves[adapterPosition], adapterPosition)
                }
            }
            holder.binding.tvReject.setOnClickListener {
                val adapterPosition = holder.adapterPosition
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    rejectClick?.onItemClick(leaves[adapterPosition], adapterPosition)
                }
            }
        } else {
            holder.binding.btnDivider.isVisible = false
            holder.binding.tvApproved.isVisible = false
            holder.binding.tvReject.isVisible = false
        }

        holder.binding.dropDown.setOnClickListener {
            holder.binding.lyDropDown.isVisible = !holder.binding.lyDropDown.isVisible
        }
    }

    override fun getItemCount(): Int {
        return leaves.size
    }

    class AccountsVH(val binding: MyTeamLeaveRowBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(teamLeaveDetail: TeamLeaveDetail,mContext: Context) {
            binding.tvEmployeName.text = teamLeaveDetail.employee_name
            binding.tvEmployeDesignation.text = teamLeaveDetail.post_date
            binding.tvPosting.text = teamLeaveDetail.designation
            binding.tvAttendStatus.text = teamLeaveDetail.status
            binding.tvFromDateTxt.text = teamLeaveDetail.from_date
            binding.tvToDateTxt.text = teamLeaveDetail.to_date
            binding.tvLeaveTypeTxt.text = teamLeaveDetail.leave_type
            binding.tvLeaveApproverTxt.text = teamLeaveDetail.leave_approver
            binding.tvReasonTxt.text = teamLeaveDetail.leave_reason
            binding.profileImg.setUrlImage(teamLeaveDetail.image, mContext)


        }
    }

}
