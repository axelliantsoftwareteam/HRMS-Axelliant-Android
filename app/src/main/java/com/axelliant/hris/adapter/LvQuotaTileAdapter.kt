package com.axelliant.hris.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.axelliant.hris.R
import com.axelliant.hris.databinding.LvItemQuotaBinding
import com.axelliant.hris.model.leaveatt.LeaveQuota
import com.axelliant.hris.ui.WsFormat
import com.axelliant.hris.ui.WsTone

/** Two-column grid tiles of the Leave screen: remaining / total days of one leave type. */
class LvQuotaTileAdapter : ListAdapter<LeaveQuota, LvQuotaTileAdapter.Holder>(DIFF) {

    class Holder(val binding: LvItemQuotaBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        Holder(LvItemQuotaBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val quota = getItem(position)
        val ctx = holder.itemView.context
        val color = WsTone.cycle(ctx, position)
        val total = quota.total
        val remaining = quota.remaining

        with(holder.binding) {
            lvQuotaName.text = (quota.name?.takeIf { it.isNotBlank() } ?: ctx.getString(R.string.lv_quota_fallback)).uppercase()
            lvQuotaRemaining.text = WsFormat.number(remaining)
            lvQuotaRemaining.setTextColor(color)
            lvQuotaTotal.text =
                if (total > 0) ctx.getString(R.string.lv_quota_of, WsFormat.number(total))
                else ctx.getString(R.string.lv_quota_allocated)

            val fraction = if (total > 0) (remaining / total).coerceIn(0.0, 1.0) else 0.0
            lvQuotaProgress.setIndicatorColor(color)
            lvQuotaProgress.setProgressCompat((fraction * lvQuotaProgress.max).toInt(), false)
        }
    }

    private companion object {
        val DIFF = object : DiffUtil.ItemCallback<LeaveQuota>() {
            // Quotas are re-fetched as a whole, so the name is the identity.
            override fun areItemsTheSame(a: LeaveQuota, b: LeaveQuota) = a.name == b.name
            override fun areContentsTheSame(a: LeaveQuota, b: LeaveQuota) = a == b
        }
    }
}
