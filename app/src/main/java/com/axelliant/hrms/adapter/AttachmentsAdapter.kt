package com.axelliant.hrms.adapter

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.axelliant.hrms.callback.AdapterItemClick
import com.axelliant.hrms.databinding.AttachmentRowBinding
import com.axelliant.hrms.databinding.BirthdayRowBinding
import com.axelliant.hrms.extention.setLocalImage
import com.axelliant.hrms.extention.setUrlImage
import com.axelliant.hrms.model.dashboard.Birthday
import com.axelliant.hrms.model.expense.ImageType

class AttachmentsAdapter(
    private val context: Context,
    private val list: List<ImageType>,
    private val itemClick: AdapterItemClick
) :
    RecyclerView.Adapter<AttachmentsAdapter.AccountsVH>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AccountsVH {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = AttachmentRowBinding.inflate(layoutInflater, parent, false)
        return AccountsVH(binding)
    }

    override fun onBindViewHolder(holder: AccountsVH, position: Int) {
        holder.bind(list[position], context)

        holder.binding.imgClose.setOnClickListener {
            itemClick.onItemClick(list[position], position)
        }
    }

    override fun getItemCount(): Int {
        return list.size
    }

    class AccountsVH(val binding: AttachmentRowBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ImageType, context: Context) {

            if(item.isMediaQuery)
                binding.ivImg.setLocalImage(item.uri!!,context)
            else
                binding.ivImg.setUrlImage(item.imageUrl!!,context)

        }
    }

}
