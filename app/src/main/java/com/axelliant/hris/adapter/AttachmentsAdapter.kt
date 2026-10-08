package com.axelliant.hris.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.axelliant.hris.R
import com.axelliant.hris.databinding.ExpItemAttachmentBinding
import com.axelliant.hris.model.expense.ImageType
import com.bumptech.glide.Glide

/**
 * Attachment thumbnails (80 x 80, rounded). With [showRemove] each thumbnail has a small round "x"
 * (create/edit screen); otherwise tapping a thumbnail opens it (claim lists).
 */
class AttachmentsAdapter(
    private val showRemove: Boolean,
    private val onClick: (ImageType, Int) -> Unit = { _, _ -> },
    private val onRemove: (ImageType, Int) -> Unit = { _, _ -> }
) : ListAdapter<ImageType, AttachmentsAdapter.VH>(AttachmentDiff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ExpItemAttachmentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        val holder = VH(binding)
        binding.root.setOnClickListener {
            val position = holder.bindingAdapterPosition
            if (position != RecyclerView.NO_POSITION) onClick(getItem(position), position)
        }
        binding.expAttRemove.setOnClickListener {
            val position = holder.bindingAdapterPosition
            if (position != RecyclerView.NO_POSITION) onRemove(getItem(position), position)
        }
        binding.expAttRemove.visibility = if (showRemove) View.VISIBLE else View.GONE
        return holder
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

    class VH(private val binding: ExpItemAttachmentBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ImageType) {
            val source: Any? = item.uri ?: item.imageUrl
            Glide.with(binding.expAttImage)
                .load(source)
                .placeholder(R.drawable.ic_place_holder)
                .error(R.drawable.ic_place_holder)
                .centerCrop()
                .into(binding.expAttImage)
        }
    }
}

/** Thumbnails are the same object for as long as they exist, so identity is enough. */
private object AttachmentDiff : DiffUtil.ItemCallback<ImageType>() {
    override fun areItemsTheSame(oldItem: ImageType, newItem: ImageType) = oldItem === newItem
    override fun areContentsTheSame(oldItem: ImageType, newItem: ImageType) = true
}
