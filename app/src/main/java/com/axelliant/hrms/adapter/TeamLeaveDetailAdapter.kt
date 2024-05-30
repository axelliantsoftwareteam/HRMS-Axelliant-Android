package com.axelliant.hrms.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.axelliant.hrms.Test
import com.axelliant.hrms.callback.AdapterItemClick
import com.axelliant.hrms.databinding.MyAttendanceDetailRowBinding
import com.axelliant.hrms.databinding.MyTeamAttendRowBinding
import com.axelliant.hrms.databinding.MyTeamLeaveRowBinding
import com.axelliant.hrms.model.leave.TeamLeaveDetail

class TeamLeaveDetailAdapter(
    private val leaves: ArrayList<TeamLeaveDetail>
) :
    RecyclerView.Adapter<TeamLeaveDetailAdapter.AccountsVH>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AccountsVH {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = MyTeamLeaveRowBinding.inflate(layoutInflater, parent, false)
        return AccountsVH(binding)
    }

    override fun onBindViewHolder(holder: AccountsVH, position: Int) {
        holder.bind(leaves[position])
        holder.binding.lyDropDown.isVisible = false

        holder.binding.tvDropDown.setOnClickListener {

            holder.binding.lyDropDown.isVisible = !holder.binding.lyDropDown.isVisible
        }
    }

    override fun getItemCount(): Int {
        return leaves.size
    }

    class AccountsVH(val binding: MyTeamLeaveRowBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(teamLeaveDetail: TeamLeaveDetail) {
            binding.tvName.text = teamLeaveDetail.employee_name
            binding.tvPostingDate.text = teamLeaveDetail.post_date
            binding.tvDesignation.text = teamLeaveDetail.designation
            binding.tvAttendStatus.text = teamLeaveDetail.status
            binding.tvFromDateTxt.text = teamLeaveDetail.from_date
            binding.tvToDateTxt.text = teamLeaveDetail.to_date
            binding.tvLeaveTypeTxt.text = teamLeaveDetail.leave_type
            binding.tvLeaveApproverTxt.text = teamLeaveDetail.leave_approver
            binding.tvReasonTxt.text = teamLeaveDetail.leave_approver

        }
    }

}