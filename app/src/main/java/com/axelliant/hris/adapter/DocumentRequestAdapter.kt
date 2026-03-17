package com.axelliant.hris.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.axelliant.hris.R
import com.axelliant.hris.callback.AdapterItemClick
import com.axelliant.hris.databinding.MyDocumentRequestRowBinding
import com.axelliant.hris.enums.LeaveStatus
import com.axelliant.hris.extention.valueQualifier
import com.axelliant.hris.model.documentRequest.DocumentForm
import com.axelliant.hris.utils.Utils.hideShow

class DocumentRequestAdapter(
    private val list: ArrayList<DocumentForm>,
    private val mContext: Context,
    private val itemClick: AdapterItemClick
) :
    RecyclerView.Adapter<DocumentRequestAdapter.AccountsVH>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AccountsVH {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = MyDocumentRequestRowBinding.inflate(layoutInflater, parent, false)
        return AccountsVH(binding)

    }

    override fun onBindViewHolder(holder: AccountsVH, position: Int) {
        holder.bind(list[position])


        holder.binding.tvDropDown.setOnClickListener {
            holder.binding.lyDropDown.hideShow(it)
            holder.binding.lineDiv.isVisible = holder.binding.lyDropDown.isVisible

        }

        when (list[position].status) {
            LeaveStatus.PENDING.value -> {
                holder.binding.tvDate.backgroundTintList = ContextCompat.getColorStateList(mContext, R.color.light_yellow)
                holder.binding.tvDate.setTextColor(ContextCompat.getColorStateList(mContext, R.color.yellow))
            }
            LeaveStatus.APPROVED.value -> {
                holder.binding.tvDate.backgroundTintList = ContextCompat.getColorStateList(mContext, R.color.light_green)
                holder.binding.tvDate.setTextColor(ContextCompat.getColorStateList(mContext, R.color.green))
            }
            LeaveStatus.REJECTED.value -> {
                holder.binding.tvDate.backgroundTintList = ContextCompat.getColorStateList(mContext, R.color.color_third_light)
                holder.binding.tvDate.setTextColor(ContextCompat.getColorStateList(mContext, R.color.color_third))
            }
        }

    }

    override fun getItemCount(): Int {
        return list.size
    }


    class AccountsVH(val binding: MyDocumentRequestRowBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: DocumentForm) {

            if (item.status.toString() == "Draft")
                binding.tvDate.text = LeaveStatus.PENDING.value
            else
                binding.tvDate.text = item.status.toString()

            binding.status.text = item.creation.valueQualifier()
            binding.tvHour.text = item.subject.valueQualifier()
            binding.tvReasonTxt.text=item.documents_required.valueQualifier()
            binding.tvSubjectTxt.text=item.subject.valueQualifier()

        }
    }
}

