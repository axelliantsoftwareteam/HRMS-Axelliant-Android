package com.axelliant.hris.adapter

import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.axelliant.hris.R
import com.axelliant.hris.databinding.CalItemCountBinding
import com.axelliant.hris.databinding.CalItemDayBinding
import com.axelliant.hris.databinding.CalItemMemberBinding
import com.axelliant.hris.model.team.CalendarStatus
import com.axelliant.hris.model.team.TeamCalendarDay
import com.axelliant.hris.model.team.TeamCalendarMember
import com.axelliant.hris.model.team.TeamDates
import com.axelliant.hris.ui.WsStatus
import com.axelliant.hris.ui.setAvatar
import com.intuit.sdp.R as SdpR
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/** One team member card of the Team Calendar. */
data class CalendarRow(
    val key: String,
    val member: TeamCalendarMember,
    val counts: List<Pair<CalendarStatus, Int>>,
    val expanded: Boolean,
    val highlighted: Set<CalendarStatus>
)

class TeamCalendarAdapter(
    private val onToggle: (String) -> Unit,
    private val onDay: (TeamCalendarMember, TeamCalendarDay) -> Unit
) : ListAdapter<CalendarRow, TeamCalendarAdapter.VH>(Diff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(CalItemMemberBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

    inner class VH(private val b: CalItemMemberBinding) : RecyclerView.ViewHolder(b.root) {

        fun bind(row: CalendarRow) {
            val ctx = b.root.context
            val m = row.member
            val name = m.employee_name?.takeIf { it.isNotBlank() } ?: ctx.getString(R.string.team_employee_fallback)
            b.calMemAvatar.setAvatar(m.image, name)
            b.calMemName.text = name
            b.calMemSub.text = listOf(
                m.designation.orEmpty(),
                if (m.employee_code.isNullOrBlank()) "" else ctx.getString(R.string.cal_emp_id, m.employee_code)
            ).filter { it.isNotBlank() }.joinToString(" · ")

            bindCounts(row)

            b.calMemToggleLabel.setText(if (row.expanded) R.string.cal_hide_calendar else R.string.cal_show_calendar)
            b.calMemChevron.rotation = if (row.expanded) 180f else 0f
            b.calMemToggle.setOnClickListener { onToggle(row.key) }

            if (row.expanded) {
                b.calMemGrid.visibility = View.VISIBLE
                buildMonthGrid(row)
            } else {
                b.calMemGrid.visibility = View.GONE
                b.calMemGrid.removeAllViews()
            }
        }

        private fun bindCounts(row: CalendarRow) {
            val ctx = b.root.context
            b.calMemCounts.removeAllViews()
            if (row.counts.isEmpty()) {
                b.calMemCountsBlock.visibility = View.GONE
                return
            }
            b.calMemCountsBlock.visibility = View.VISIBLE
            val inflater = LayoutInflater.from(ctx)
            row.counts.forEach { (status, count) ->
                val cb = CalItemCountBinding.inflate(inflater, b.calMemCounts, false)
                cb.calCountValue.text = count.toString()
                cb.calCountValue.setTextColor(ContextCompat.getColor(ctx, status.colorRes))
                cb.calCountLabel.text = ctx.getString(status.labelRes).uppercase()
                b.calMemCounts.addView(cb.root, cellParams(0))
            }
        }

        private fun cellParams(marginPx: Int) = GridLayout.LayoutParams(
            GridLayout.spec(GridLayout.UNDEFINED, 1f),
            GridLayout.spec(GridLayout.UNDEFINED, 1f)
        ).apply {
            width = 0
            setMargins(marginPx, marginPx, marginPx, marginPx)
        }

        private fun buildMonthGrid(row: CalendarRow) {
            val ctx = b.root.context
            val grid = b.calMemGrid
            grid.removeAllViews()
            val days = row.member.days.orEmpty()
            val margin = ctx.resources.getDimensionPixelSize(SdpR.dimen._2sdp)
            val radius = ctx.resources.getDimensionPixelSize(SdpR.dimen._8sdp).toFloat()
            val density = ctx.resources.displayMetrics.density
            val hairline = maxOf(1, density.toInt())
            val thick = maxOf(2, (2 * density).toInt())
            val primary = ContextCompat.getColor(ctx, R.color.ws_primary)
            val grey = ContextCompat.getColor(ctx, R.color.ws_grey)
            val today = TeamDates.today()
            val inflater = LayoutInflater.from(ctx)

            // Weekday header, starting at the locale's first weekday.
            val first = Calendar.getInstance().firstDayOfWeek
            val narrow = SimpleDateFormat("EEEEE", Locale.getDefault())
            val probe = Calendar.getInstance()
            for (i in 0 until 7) {
                probe.set(Calendar.DAY_OF_WEEK, (first - 1 + i) % 7 + 1)
                val tv = TextView(ctx, null, 0, R.style.Ws_Text_Medium).apply {
                    text = narrow.format(probe.time)
                    gravity = Gravity.CENTER
                    setTextColor(grey)
                    setTextSize(TypedValue.COMPLEX_UNIT_PX, ctx.resources.getDimension(R.dimen.cal_weekday_text))
                }
                grid.addView(tv, cellParams(margin))
            }

            // Leading blanks so the first day falls under its weekday.
            val firstDay = days.firstOrNull()?.date
            if (firstDay != null) {
                val weekday = TeamDates.calendar(firstDay).get(Calendar.DAY_OF_WEEK)
                val blanks = (weekday - first + 7) % 7
                for (i in 0 until blanks) grid.addView(View(ctx), cellParams(margin))
            }

            val monthDay = SimpleDateFormat("MMM d", Locale.getDefault())
            days.forEach { day ->
                val status = CalendarStatus.of(day)
                val color = ContextCompat.getColor(ctx, status.colorRes)
                val isToday = day.date == today
                val cell = CalItemDayBinding.inflate(inflater, grid, false)
                val cal = TeamDates.calendar(day.date)
                cell.calDayNumber.text =
                    if (cal.get(Calendar.DAY_OF_MONTH) == 1) monthDay.format(cal.time) else cal.get(Calendar.DAY_OF_MONTH).toString()
                cell.calDayMark.text = status.mark
                cell.calDayNumber.setTextColor(color)
                cell.calDayMark.setTextColor(color)
                cell.root.background = GradientDrawable().apply {
                    cornerRadius = radius
                    setColor(if (status == CalendarStatus.NO_RECORD) WsStatus.tint(grey, 8) else WsStatus.tint(color, 16))
                    if (isToday) setStroke(thick, primary) else setStroke(hairline, WsStatus.tint(color, 15))
                }
                val dimmed = row.highlighted.isNotEmpty() && status !in row.highlighted
                cell.root.alpha = if (dimmed) 0.25f else 1f
                cell.root.contentDescription =
                    "${day.date}, ${day.label?.takeIf { it.isNotEmpty() } ?: ctx.getString(status.labelRes)}"
                cell.root.setOnClickListener { onDay(row.member, day) }
                grid.addView(cell.root, cellParams(margin))
            }
        }
    }

    private object Diff : DiffUtil.ItemCallback<CalendarRow>() {
        override fun areItemsTheSame(a: CalendarRow, b: CalendarRow) = a.key == b.key
        override fun areContentsTheSame(a: CalendarRow, b: CalendarRow) = a == b
    }
}
