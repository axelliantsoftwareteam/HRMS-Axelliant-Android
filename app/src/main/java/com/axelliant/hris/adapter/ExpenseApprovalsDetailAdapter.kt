package com.axelliant.hris.adapter

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.axelliant.hris.callback.AdapterItemClick
import com.axelliant.hris.databinding.MyTeamExpenseApprovalRowBinding
import com.axelliant.hris.extention.setUrlImage
import com.axelliant.hris.model.expense.Attachments
import com.axelliant.hris.model.expense.Expense
import com.axelliant.hris.model.expense.ImageType
import com.axelliant.hris.navigation.AppNavigator
import com.axelliant.hris.utils.Utils.hideShow
import com.google.gson.Gson

class ExpenseApprovalsDetailAdapter(
    private val mContext: Context,
    private val detailArrayList: ArrayList<Expense>,
    private val approvedItemClick: AdapterItemClick,
    private val rejectItemClick: AdapterItemClick,
) :
    RecyclerView.Adapter<ExpenseApprovalsDetailAdapter.AccountsVH>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AccountsVH {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = MyTeamExpenseApprovalRowBinding.inflate(layoutInflater, parent, false)
        return AccountsVH(binding)
    }

    override fun onBindViewHolder(holder: AccountsVH, position: Int) {
        val currentItem = detailArrayList[position]
        holder.bind(currentItem, mContext)

        holder.binding.rvLeaveCount.layoutManager = GridLayoutManager(mContext, 1)
        holder.binding.rvLeaveCount.adapter =
            currentItem.expenses_detail?.let { ExpenseRowAdapter(it, mContext) }
        holder.binding.rvLeaveCount.isNestedScrollingEnabled = false

        holder.binding.dropDown.setOnClickListener {
            holder.binding.lyAttendStatus.hideShow(it)
            holder.binding.divider.isVisible = holder.binding.lyAttendStatus.isVisible
        }

        holder.binding.tvApproved.setOnClickListener {
            val adapterPosition = holder.adapterPosition
            if (adapterPosition != RecyclerView.NO_POSITION) {
                approvedItemClick.onItemClick(detailArrayList[adapterPosition], adapterPosition)
            }
        }

        holder.binding.tvReject.setOnClickListener {
            val adapterPosition = holder.adapterPosition
            if (adapterPosition != RecyclerView.NO_POSITION) {
                rejectItemClick.onItemClick(detailArrayList[adapterPosition], adapterPosition)
            }
        }


        holder.binding.rvAttachments.layoutManager =
            LinearLayoutManager(mContext, RecyclerView.HORIZONTAL, false)
        holder.binding.rvAttachments.adapter =
            AttachmentsAdapter(
                false,
                mContext,
                attachmentTypeMapping(currentItem.attachments),
                object : AdapterItemClick {
                    override fun onItemClick(customObject: Any, pos: Int) {
                        val adapterPosition = holder.adapterPosition
                        if (adapterPosition != RecyclerView.NO_POSITION) {
                            AppNavigator.navigateToImageDetailFragment(Bundle().apply {
                                this.putString("images", Gson().toJson(detailArrayList[adapterPosition].attachments))
                            })
                        }

                    }

                })
    }
    private fun attachmentTypeMapping(attachmentArray: List<Attachments>?): ArrayList<ImageType> {

        val localItems: ArrayList<ImageType> = arrayListOf()

        if (attachmentArray != null) {
            for (serverItem in attachmentArray) {

                localItems.add(ImageType().apply {
                    this.isUploaded = true
                    this.isMediaQuery = false
                    this.uri = null
                    this.imageUrl = serverItem.file_url
                    this.file_id = serverItem.name

                })

            }
        }


        return localItems
    }

    override fun getItemCount(): Int {
        return detailArrayList.size
    }

    class AccountsVH(val binding: MyTeamExpenseApprovalRowBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Expense, mContext: Context)
        {
            binding.tvEmployeName.text = item.employee_name
            binding.tvEmployeDesignation.text = item.posting_date
            binding.tvPosting.text = item.department
            binding.profileImg.setUrlImage(item.image, mContext)

        }
    }

}
