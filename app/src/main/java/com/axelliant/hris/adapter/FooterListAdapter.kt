package com.axelliant.hris.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.axelliant.hris.R

/**
 * Base for the paged and plain card lists: items plus an optional footer row (a small spinner while
 * the next page loads, or an end-of-list note). [submit] diffs on the main thread; the lists are small.
 */
abstract class FooterListAdapter<T : Any> : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    enum class Footer { NONE, LOADING, END }

    var items: List<T> = emptyList()
        private set

    /** Text shown when [footer] is [Footer.END]. */
    var endText: String = ""

    var footer: Footer = Footer.NONE
        set(value) {
            if (field == value) return
            val had = field != Footer.NONE
            field = value
            val has = value != Footer.NONE
            val position = items.size
            when {
                !had && has -> notifyItemInserted(position)
                had && !has -> notifyItemRemoved(position)
                else -> notifyItemChanged(position)
            }
        }

    protected abstract fun isSameItem(old: T, new: T): Boolean
    protected abstract fun createItemHolder(parent: ViewGroup): RecyclerView.ViewHolder
    protected abstract fun bindItem(holder: RecyclerView.ViewHolder, item: T)

    fun submit(newItems: List<T>) {
        val old = items
        if (old == newItems) return
        val diff = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize() = old.size
            override fun getNewListSize() = newItems.size
            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int) =
                isSameItem(old[oldItemPosition], newItems[newItemPosition])

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int) =
                old[oldItemPosition] == newItems[newItemPosition]
        })
        items = newItems
        diff.dispatchUpdatesTo(this)
    }

    /** Rebinds every item (used when something outside the data changes, e.g. which row is busy). */
    fun refreshItems() {
        if (items.isNotEmpty()) notifyItemRangeChanged(0, items.size)
    }

    override fun getItemCount(): Int = items.size + if (footer != Footer.NONE) 1 else 0

    override fun getItemViewType(position: Int): Int = if (position < items.size) TYPE_ITEM else TYPE_FOOTER

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        if (viewType == TYPE_FOOTER) {
            FooterHolder(LayoutInflater.from(parent.context).inflate(R.layout.pay_item_footer, parent, false))
        } else {
            createItemHolder(parent)
        }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is FooterHolder) {
            holder.bind(footer, endText)
        } else {
            bindItem(holder, items[position])
        }
    }

    private class FooterHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val progress = view.findViewById<View>(R.id.pay_footer_progress)
        private val text = view.findViewById<TextView>(R.id.pay_footer_text)

        fun bind(footer: Footer, endText: String) {
            progress.visibility = if (footer == Footer.LOADING) View.VISIBLE else View.GONE
            text.visibility = if (footer == Footer.END) View.VISIBLE else View.GONE
            text.text = endText
        }
    }

    private companion object {
        const val TYPE_ITEM = 0
        const val TYPE_FOOTER = 1
    }
}
