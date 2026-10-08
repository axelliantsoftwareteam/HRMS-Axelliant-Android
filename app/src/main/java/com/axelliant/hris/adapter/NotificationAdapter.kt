package com.axelliant.hris.adapter

import android.content.Context
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorInt
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import com.axelliant.hris.R
import com.axelliant.hris.databinding.NotifItemRowBinding
import com.axelliant.hris.model.notification.NotificationKind
import com.axelliant.hris.model.notification.WorkflowNotification
import com.axelliant.hris.model.payslip.cleaned
import com.axelliant.hris.ui.WsFormat
import com.axelliant.hris.ui.WsStatus
import com.axelliant.hris.ui.WsTone

/** Notification rows: tinted icon, message (medium when unread), sender/time, document badge, unread dot. */
class NotificationAdapter(private val onClick: (WorkflowNotification) -> Unit) : FooterListAdapter<WorkflowNotification>() {

    override fun isSameItem(old: WorkflowNotification, new: WorkflowNotification) = old.name == new.name

    override fun createItemHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        RowHolder(NotifItemRowBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun bindItem(holder: RecyclerView.ViewHolder, item: WorkflowNotification) {
        (holder as RowHolder).bind(item)
    }

    @ColorInt
    fun toneColor(context: Context, kind: NotificationKind): Int = when (kind) {
        NotificationKind.LEAVE -> WsTone.leave(context)
        NotificationKind.ATTENDANCE -> WsTone.attendance(context)
        NotificationKind.EXPENSE -> WsTone.expense(context)
        NotificationKind.OTHER -> WsTone.notification(context)
    }

    private inner class RowHolder(private val binding: NotifItemRowBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: WorkflowNotification) {
            val context = binding.root.context
            val kind = NotificationKind.of(item.reference_document_type)
            val tone = toneColor(context, kind)

            binding.notifIcon.setImageResource(
                when (kind) {
                    NotificationKind.LEAVE -> R.drawable.ws_ic_event_busy
                    NotificationKind.ATTENDANCE -> R.drawable.ws_ic_schedule
                    NotificationKind.EXPENSE -> R.drawable.ws_ic_description
                    NotificationKind.OTHER -> R.drawable.ws_ic_bell
                }
            )
            WsStatus.iconBadge(binding.notifIcon, tone)

            binding.notifMessage.text = item.message.cleaned() ?: context.getString(R.string.notif_default_message)
            binding.notifMessage.typeface = ResourcesCompat.getFont(
                context,
                if (item.isRead) R.font.poppins_regular else R.font.poppins_medium
            ) ?: Typeface.DEFAULT

            val meta = listOf(item.from_user_name.cleaned().orEmpty(), WsFormat.dateAt(context, item.created_on))
                .filter { it.isNotEmpty() }.joinToString(" · ")
            binding.notifMeta.text = meta
            binding.notifMeta.visibility = if (meta.isEmpty()) View.GONE else View.VISIBLE

            val type = item.reference_document_type.cleaned()
            binding.notifType.visibility = if (type != null) View.VISIBLE else View.GONE
            if (type != null) WsStatus.badge(binding.notifType, type, tone)

            binding.notifUnreadDot.visibility = if (item.isRead) View.GONE else View.VISIBLE
            binding.root.setOnClickListener { onClick(item) }
        }
    }
}
