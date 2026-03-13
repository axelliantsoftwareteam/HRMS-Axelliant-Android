package com.axelliant.hris.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.axelliant.hris.callback.AdapterItemClick
import com.axelliant.hris.databinding.AttachmentRowBinding
import com.axelliant.hris.extention.setLocalImage
import com.axelliant.hris.extention.setUrlImage
import com.axelliant.hris.model.expense.ImageType

class AttachmentsAdapter(
    private val isDeleteShow:Boolean =true,
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

        holder.binding.imgClose.isVisible = isDeleteShow

        if(isDeleteShow){
            holder.binding.imgClose.setOnClickListener {
                val adapterPosition = holder.adapterPosition
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    itemClick.onItemClick(list[adapterPosition], adapterPosition)
                }
            }
        }else{
            holder.binding.ivImg.setOnClickListener {
                val adapterPosition = holder.adapterPosition
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    itemClick.onItemClick(list[adapterPosition], adapterPosition)
                }
            }
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
