package com.axelliant.hris.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.axelliant.hris.callback.AdapterItemClick
import com.axelliant.hris.databinding.MyTeamExpenseApprovalRowBinding
import com.axelliant.hris.databinding.MyTeamResourceApprovalRowBinding
import com.axelliant.hris.enums.ResourceStatus
import com.axelliant.hris.extention.setUrlImage
import com.axelliant.hris.model.resourceManage.DocumentHours
import com.axelliant.hris.utils.Utils.hideShow

class ResourcesApprovalsDetailAdapter(
    private val mContext: Context,
    private val detailArrayList: ArrayList<DocumentHours>,
    private val itemClick: AdapterItemClick,
) :
    RecyclerView.Adapter<ResourcesApprovalsDetailAdapter.AccountsVH>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AccountsVH {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = MyTeamResourceApprovalRowBinding.inflate(layoutInflater, parent, false)
        return AccountsVH(binding)
    }

    override fun onBindViewHolder(holder: AccountsVH, position: Int) {
        holder.bind(detailArrayList[position], mContext)

        holder.binding.rvLeaveCount.layoutManager = GridLayoutManager(mContext, 1)
        holder.binding.rvLeaveCount.adapter =
            detailArrayList[position].resource_detail?.let { HoursRowAdapter(it, mContext) }
        holder.binding.rvLeaveCount.isNestedScrollingEnabled = false



        holder.binding.viewExpand.isChecked = detailArrayList[position].isSelected
        holder.binding.viewExpand.setOnCheckedChangeListener { _, isChecked ->
            detailArrayList[position].isSelected = isChecked
        }


        holder.binding.dropDown.setOnClickListener {
            holder.binding.lyAttendStatus.hideShow(it)
            holder.binding.divider.isVisible = holder.binding.lyAttendStatus.isVisible
        }

//        holder.binding.tvApproved.setOnClickListener{
//            approvedItemClick.onItemClick(detailArrayList[position],position)
//        }
//
//        holder.binding.tvReject.setOnClickListener{
//            rejectItemClick.onItemClick(detailArrayList[position],position)
//
//        }


    }

    override fun getItemCount(): Int {
        return detailArrayList.size
    }

    class AccountsVH(val binding: MyTeamResourceApprovalRowBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: DocumentHours, mContext: Context) {
            if (item.docstatus == ResourceStatus.Submit.value)
                binding.viewExpand.isVisible = false

            binding.tvEmployeName.text = item.employee_name
            binding.tvEmployeDesignation.text = item.employee
            binding.tvPosting.text = item.department
            binding.profileImg.setUrlImage(item.department, mContext)

        }
    }

}