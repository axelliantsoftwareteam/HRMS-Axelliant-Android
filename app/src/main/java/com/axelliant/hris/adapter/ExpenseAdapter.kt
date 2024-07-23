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
import com.axelliant.hris.databinding.MyTeamExpenseRowBinding
import com.axelliant.hris.extention.valueQualifier
import com.axelliant.hris.model.expense.Attachments
import com.axelliant.hris.model.expense.Expense
import com.axelliant.hris.model.expense.ImageType
import com.axelliant.hris.navigation.AppNavigator
import com.axelliant.hris.utils.Utils.hideShow
import com.google.gson.Gson

class ExpenseAdapter(
    private val list: ArrayList<Expense>,
    private val mContext: Context,
    private val itemClick: AdapterItemClick
) :
    RecyclerView.Adapter<ExpenseAdapter.AccountsVH>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AccountsVH {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = MyTeamExpenseRowBinding.inflate(layoutInflater, parent, false)
        return AccountsVH(binding)
    }

    override fun onBindViewHolder(holder: AccountsVH, position: Int) {
        holder.bind(list[position], mContext)

//        holder.binding.tvViewDetail.setOnClickListener {
//            itemClick.onItemClick(list[position], position)
//        }
        holder.binding.rvLeaveCount.layoutManager = GridLayoutManager(mContext, 1)
        holder.binding.rvLeaveCount.adapter =
            list[position].expenses_detail?.let { ExpenseRowAdapter(it, mContext) }
        holder.binding.rvLeaveCount.isNestedScrollingEnabled = false


        holder.binding.lyWeekly.setOnClickListener {


            itemClick.onItemClick(list[position], position)
        }

        holder.binding.icDropDown.setOnClickListener {
            holder.binding.lyDropDown.hideShow(it)
            holder.binding.lineDiv.isVisible = holder.binding.rvLeaveCount.isVisible

        }

    }

    override fun getItemCount(): Int {
        return list.size
    }

    class AccountsVH(val binding: MyTeamExpenseRowBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Expense, mContext: Context) {
//            binding.tvTitle.text = item.title.toString()
//            when(expenseEvent){
//                Pending -> {
//                    binding.tvDelete.visibility = View.VISIBLE
//                }
//                Approved -> {
//                    binding.tvDelete.visibility = View.GONE
//                }
//            }
            if (item.approval_status.toString() == "Draft")
                binding.tvDate.text = "Pending"
            else
                binding.tvDate.text = item.approval_status.toString()

            binding.status.text = item.total_claimed_amount.toString().valueQualifier()
            binding.tvHour.text = item.posting_date.valueQualifier()
            if (item.approval_status == "Draft")
                binding.tvAttendStatus.text = "Pending"
            else
                binding.tvAttendStatus.text = item.approval_status.valueQualifier()
//            binding.profileImg.setUrlImage(item.image, mContext)


            binding.rvAttachments.layoutManager =
                LinearLayoutManager(mContext, RecyclerView.HORIZONTAL, false)
            binding.rvAttachments.adapter =
                AttachmentsAdapter(
                    false,
                    mContext,
                    attachmentTypeMapping(item.attachments),
                    object : AdapterItemClick {
                        override fun onItemClick(customObject: Any, position: Int) {

                            AppNavigator.navigateToImageDetailFragment(Bundle().apply {
                                this.putString("images",Gson().toJson(item.attachments))

                            })

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

    }


}

