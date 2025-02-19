package com.axelliant.hris.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.axelliant.hris.R
import com.axelliant.hris.callback.CheckBoxAdapterItemClick
import com.axelliant.hris.databinding.MyResourceHourRowBinding
import com.axelliant.hris.enums.LeaveStatus
import com.axelliant.hris.enums.ResourceStatus
import com.axelliant.hris.extention.valueQualifier
import com.axelliant.hris.model.resourceManage.DocumentHours
import com.axelliant.hris.utils.Utils.formatTitleDate
import com.axelliant.hris.utils.Utils.hideShow

class ResourceHoursAdapter(
    private val list: ArrayList<DocumentHours>,
    private val mContext: Context,
    private val itemClick: CheckBoxAdapterItemClick
) :
    RecyclerView.Adapter<ResourceHoursAdapter.AccountsVH>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AccountsVH {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = MyResourceHourRowBinding.inflate(layoutInflater, parent, false)
        return AccountsVH(binding)

    }

    override fun onBindViewHolder(holder: AccountsVH, position: Int) {
        holder.bind(list[position], mContext)

//        holder.binding.tvViewDetail.setOnClickListener {
//            itemClick.onItemClick(list[position], position)
//        }
        holder.binding.rvLeaveCount.layoutManager = GridLayoutManager(mContext, 1)
        holder.binding.rvLeaveCount.adapter =
            list[position].resource_detail?.let { HoursRowAdapter(it, mContext) }
        holder.binding.rvLeaveCount.isNestedScrollingEnabled = false

        holder.binding.tvAttendStatus.setOnClickListener {
            itemClick.onItemClick(list[position], position)
        }

        holder.binding.viewExpand.isChecked = list[position].isSelected
        holder.binding.viewExpand.setOnCheckedChangeListener { _, isChecked ->
            list[position].isSelected = isChecked
        }

//        holder.binding.viewExpand.setOnClickListener {
//            itemClick.onCheckBoxItemClick(list[position], position)
//        }

        holder.binding.icDropDown.setOnClickListener {
            holder.binding.lyDropDown.hideShow(it)
            holder.binding.lineDiv.isVisible = holder.binding.rvLeaveCount.isVisible

        }

        when (list[position].docstatus)
        {
            ResourceStatus.DRAFT.value -> {
                holder.binding.viewExpand.isVisible=true
                holder.binding.tvDate.backgroundTintList =
                    ContextCompat.getColorStateList(mContext, R.color.light_yellow)
                holder.binding.tvDate.setTextColor(
                    ContextCompat.getColorStateList(
                        mContext,
                        R.color.yellow
                    )
                )
            }

            ResourceStatus.Submit.value -> {
                holder.binding.viewExpand.isVisible=false
                holder.binding.tvDate.backgroundTintList =
                    ContextCompat.getColorStateList(mContext, R.color.light_green)
                holder.binding.tvDate.setTextColor(
                    ContextCompat.getColorStateList(
                        mContext,
                        R.color.green
                    )
                )
            }

        }

    }

    override fun getItemCount(): Int {
        return list.size
    }


    class AccountsVH(val binding: MyResourceHourRowBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: DocumentHours, mContext: Context) {
            if (item.docstatus == ResourceStatus.DRAFT.value)
            {
                binding.tvDate.text = LeaveStatus.DRAFT.value
                binding.tvAttendStatus.isVisible=true

            }
            else
            {
                binding.tvDate.text = LeaveStatus.Submit.value
                binding.tvAttendStatus.isVisible=false
            }

            binding.tvAttendStatus.text = "Edit"
            binding.status.text = item.total_hours.toString().valueQualifier()
            binding.tvHour.text = item.creation?.let { formatTitleDate(it) }
        }

    }


}

