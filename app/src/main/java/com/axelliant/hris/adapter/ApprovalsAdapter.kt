package com.axelliant.hris.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.axelliant.hris.R
import com.axelliant.hris.databinding.ApprItemCardBinding
import com.axelliant.hris.model.approvals.ApprovalItem
import com.axelliant.hris.ui.WsFormat
import com.axelliant.hris.ui.WsStatus
import com.axelliant.hris.ui.WsTone
import com.axelliant.hris.ui.setAvatar

/** One pending request card; [enabled] is false while an approve/reject call runs. */
data class ApprovalRow(val item: ApprovalItem, val enabled: Boolean)

class ApprovalsAdapter(
    private val onOpen: (ApprovalItem) -> Unit,
    private val onReject: (ApprovalItem) -> Unit,
    private val onApprove: (ApprovalItem) -> Unit
) : ListAdapter<ApprovalRow, ApprovalsAdapter.VH>(Diff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(ApprItemCardBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

    inner class VH(private val b: ApprItemCardBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(row: ApprovalRow) {
            val item = row.item
            val ctx = b.root.context
            val tone = toneFor(ctx, item.approval_type)
            b.apprAvatar.setAvatar(item.image, item.displayName)
            b.apprName.text = item.displayName
            b.apprCode.text = item.custom_employee_code?.takeIf { it.isNotBlank() } ?: item.employee_code.orEmpty()
            WsStatus.badge(b.apprStatus, item.displayStatus)

            b.apprTypeIcon.setImageResource(iconFor(item.approval_type))
            WsStatus.iconBadge(b.apprTypeIcon, tone)
            b.apprType.text = item.displayType
            b.apprType.setTextColor(tone)
            b.apprDate.text = WsFormat.dateTime(ctx, item.effective_on_iso ?: item.effective_on)

            val reason = item.subtitle?.takeIf { it.isNotBlank() } ?: item.title.orEmpty()
            b.apprReason.text = reason
            b.apprReason.visibility = if (reason.isBlank()) android.view.View.GONE else android.view.View.VISIBLE

            b.apprTop.setOnClickListener { onOpen(item) }
            b.apprBtnReject.isEnabled = row.enabled
            b.apprBtnApprove.isEnabled = row.enabled
            b.apprBtnReject.setOnClickListener { onReject(item) }
            b.apprBtnApprove.setOnClickListener { onApprove(item) }
        }
    }

    companion object {
        fun toneFor(ctx: Context, type: String?): Int = when (type.orEmpty().lowercase()) {
            "leave" -> WsTone.leave(ctx)
            "checkin" -> WsTone.attendance(ctx)
            "expense" -> WsTone.expense(ctx)
            else -> WsTone.request(ctx)
        }

        fun iconFor(type: String?): Int = when (type.orEmpty().lowercase()) {
            "leave" -> R.drawable.ws_ic_event_busy
            "checkin" -> R.drawable.ws_ic_schedule
            "expense" -> R.drawable.ws_ic_credit_card
            else -> R.drawable.ws_ic_verified
        }
    }

    private object Diff : DiffUtil.ItemCallback<ApprovalRow>() {
        override fun areItemsTheSame(a: ApprovalRow, b: ApprovalRow) = a.item.key == b.item.key
        override fun areContentsTheSame(a: ApprovalRow, b: ApprovalRow) = a == b
    }
}
