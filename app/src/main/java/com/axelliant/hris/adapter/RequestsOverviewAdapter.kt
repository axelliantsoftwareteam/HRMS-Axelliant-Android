package com.axelliant.hris.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.axelliant.hris.R
import com.axelliant.hris.databinding.RqItemRequestBinding
import com.axelliant.hris.model.requests.RequestRow
import com.axelliant.hris.ui.WsFormat
import com.axelliant.hris.ui.WsStatus
import com.axelliant.hris.ui.WsTone
import java.util.Locale

/** The Requests overview list: one card per check-in, leave application or expense claim. */
class RequestsOverviewAdapter(
    private val onClick: (RequestRow) -> Unit
) : ListAdapter<RequestRow, RequestsOverviewAdapter.VH>(RequestRowDiff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = RqItemRequestBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        val holder = VH(binding)
        binding.root.setOnClickListener {
            val position = holder.bindingAdapterPosition
            if (position != RecyclerView.NO_POSITION) onClick(getItem(position))
        }
        return holder
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

    class VH(private val binding: RqItemRequestBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(row: RequestRow) {
            val ctx = binding.root.context
            val tone = RequestRowFormat.tone(ctx, row)
            binding.rqIcon.setImageResource(RequestRowFormat.icon(row))
            WsStatus.iconBadge(binding.rqIcon, tone)
            binding.rqTitle.text = RequestRowFormat.title(ctx, row)
            binding.rqDate.text = RequestRowFormat.dateLine(row)
            WsStatus.badge(binding.rqStatus, RequestRowFormat.status(ctx, row))
            val headline = RequestRowFormat.headline(ctx, row)
            binding.rqHeadline.text = headline
            binding.rqHeadline.setTextColor(tone)
            binding.rqHeadline.visibility = if (headline.isEmpty()) View.GONE else View.VISIBLE
        }
    }

}

private object RequestRowDiff : DiffUtil.ItemCallback<RequestRow>() {
    override fun areItemsTheSame(oldItem: RequestRow, newItem: RequestRow) = oldItem.key == newItem.key
    override fun areContentsTheSame(oldItem: RequestRow, newItem: RequestRow) = oldItem == newItem
}

/** How a [RequestRow] reads on its card and in its detail sheet (mirrors the iOS `RequestActivityFormatter`). */
object RequestRowFormat {

    /** "Check-in" / "Check-out" (the time is shown beside it), the leave type, "Expense claim". */
    fun title(ctx: Context, row: RequestRow): String = when (row.kind) {
        RequestRow.Kind.CHECKIN -> checkinAction(ctx, row.rawTitle)
        RequestRow.Kind.LEAVE -> row.rawTitle.ifBlank { ctx.getString(R.string.rq_title_leave) }
        RequestRow.Kind.EXPENSE -> ctx.getString(R.string.rq_title_expense)
    }

    private fun checkinAction(ctx: Context, rawTitle: String): String {
        val label = rawTitle.uppercase(Locale.ROOT)
        return when {
            label.contains(" OUT") -> ctx.getString(R.string.rq_title_checkout)
            label.contains(" IN") -> ctx.getString(R.string.rq_title_checkin)
            else -> ctx.getString(R.string.rq_title_attendance_update)
        }
    }

    /** The date a request is about: a check-in's day, a leave's dates or a claim's posting date. */
    fun dateLine(row: RequestRow): String = when (row.kind) {
        RequestRow.Kind.CHECKIN -> WsFormat.date(row.effectiveOn).ifEmpty { row.effectiveOn }
        RequestRow.Kind.LEAVE -> WsFormat.dateRange(row.fromDate.ifEmpty { row.effectiveOn }, row.toDate)
        RequestRow.Kind.EXPENSE -> WsFormat.date(row.effectiveOn)
    }

    /** The main figure of a request: a check-in's time, a leave's days, a claim's amount (empty when none). */
    fun headline(ctx: Context, row: RequestRow): String = when (row.kind) {
        RequestRow.Kind.CHECKIN -> WsFormat.time(ctx, row.effectiveOn)
        RequestRow.Kind.LEAVE -> WsFormat.days(row.totalDays)
        RequestRow.Kind.EXPENSE ->
            if (row.totalAmount > 0) {
                ctx.getString(
                    R.string.rq_headline_amount_fmt,
                    ctx.getString(R.string.rq_currency_default),
                    String.format(Locale.US, "%.0f", row.totalAmount)
                )
            } else ""
    }

    /** The status in the badge; "Open" when HRIS sends none. */
    fun status(ctx: Context, row: RequestRow): String = row.status.ifBlank { ctx.getString(R.string.rq_status_open) }

    /** "Checkin", "Leave" or "Expense". */
    fun typeLabel(ctx: Context, row: RequestRow): String = ctx.getString(
        when (row.kind) {
            RequestRow.Kind.CHECKIN -> R.string.rq_type_checkin
            RequestRow.Kind.LEAVE -> R.string.rq_type_leave
            RequestRow.Kind.EXPENSE -> R.string.rq_type_expense
        }
    )

    @ColorInt
    fun tone(ctx: Context, row: RequestRow): Int = when (row.kind) {
        RequestRow.Kind.CHECKIN -> WsTone.attendance(ctx)
        RequestRow.Kind.LEAVE -> WsTone.leave(ctx)
        RequestRow.Kind.EXPENSE -> WsTone.expense(ctx)
    }

    @DrawableRes
    fun icon(row: RequestRow): Int = when (row.kind) {
        RequestRow.Kind.CHECKIN ->
            if (row.rawTitle.uppercase(Locale.ROOT).contains(" OUT")) R.drawable.ws_ic_arrow_out else R.drawable.ws_ic_arrow_in
        RequestRow.Kind.LEAVE -> R.drawable.ws_ic_event_busy
        RequestRow.Kind.EXPENSE -> R.drawable.ws_ic_credit_card
    }
}
