package com.axelliant.hris.adapter

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.axelliant.hris.R
import com.axelliant.hris.databinding.TeamItemArrivalBinding
import com.axelliant.hris.databinding.TeamItemMemberBinding
import com.axelliant.hris.databinding.TeamItemQuotaBinding
import com.axelliant.hris.databinding.TeamItemQuotaTileBinding
import com.axelliant.hris.model.team.TeamArrival
import com.axelliant.hris.model.team.TeamMember
import com.axelliant.hris.ui.WsStatus
import com.axelliant.hris.ui.setAvatar
import com.intuit.sdp.R as SdpR

/** What one row of the Team Activity list shows; built by the screen so the adapter stays dumb. */
sealed class TeamRow {
    abstract val key: String

    data class Arrival(
        override val key: String,
        val source: TeamArrival,
        val name: String,
        val designation: String,
        val time: String,
        val location: String
    ) : TeamRow()

    data class Member(
        override val key: String,
        val source: TeamMember,
        val name: String,
        val designation: String,
        val total: String,
        val avg: String,
        val present: String,
        val chips: List<Chip>,
        val balance: String?
    ) : TeamRow()

    data class Quota(
        override val key: String,
        val image: String?,
        val name: String,
        val designation: String,
        val isSelf: Boolean,
        val available: String,
        val expanded: Boolean,
        val tiles: List<Tile>
    ) : TeamRow()

    data class Chip(val text: String, val color: Int)

    data class Tile(val value: String, val name: String, val detail: String, val color: Int)
}

class TeamActivityAdapter(
    private val onArrival: (TeamArrival) -> Unit,
    private val onMember: (TeamMember) -> Unit,
    private val onQuota: (String) -> Unit
) : ListAdapter<TeamRow, RecyclerView.ViewHolder>(Diff) {

    override fun getItemViewType(position: Int): Int = when (getItem(position)) {
        is TeamRow.Arrival -> TYPE_ARRIVAL
        is TeamRow.Member -> TYPE_MEMBER
        is TeamRow.Quota -> TYPE_QUOTA
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_ARRIVAL -> ArrivalVH(TeamItemArrivalBinding.inflate(inflater, parent, false))
            TYPE_MEMBER -> MemberVH(TeamItemMemberBinding.inflate(inflater, parent, false))
            else -> QuotaVH(TeamItemQuotaBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val row = getItem(position)) {
            is TeamRow.Arrival -> (holder as ArrivalVH).bind(row)
            is TeamRow.Member -> (holder as MemberVH).bind(row)
            is TeamRow.Quota -> (holder as QuotaVH).bind(row)
        }
    }

    private inner class ArrivalVH(private val b: TeamItemArrivalBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(row: TeamRow.Arrival) {
            b.teamArrAvatar.setAvatar(row.source.image, row.name)
            b.teamArrName.text = row.name
            b.teamArrDesignation.text = row.designation
            b.teamArrTime.text = row.time
            b.teamArrLocation.text = row.location
            b.root.setOnClickListener { onArrival(row.source) }
        }
    }

    private inner class MemberVH(private val b: TeamItemMemberBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(row: TeamRow.Member) {
            b.teamMemAvatar.setAvatar(row.source.image, row.name)
            b.teamMemName.text = row.name
            b.teamMemDesignation.text = row.designation
            b.teamMemTotal.text = row.total
            b.teamMemAvg.text = row.avg
            b.teamMemPresent.text = row.present
            b.teamMemChips.removeAllViews()
            val inflater = LayoutInflater.from(b.root.context)
            row.chips.forEach { chip ->
                val view = inflater.inflate(R.layout.team_item_chip, b.teamMemChips, false) as TextView
                WsStatus.badge(view, chip.text, chip.color, 15)
                b.teamMemChips.addView(view)
            }
            if (row.balance == null) {
                b.teamMemBalanceRow.visibility = android.view.View.GONE
            } else {
                b.teamMemBalanceRow.visibility = android.view.View.VISIBLE
                b.teamMemBalance.text = row.balance
            }
            b.root.setOnClickListener { onMember(row.source) }
        }
    }

    private inner class QuotaVH(private val b: TeamItemQuotaBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(row: TeamRow.Quota) {
            val ctx = b.root.context
            b.teamQAvatar.setAvatar(row.image, row.name)
            b.teamQName.text = row.name
            b.teamQDesignation.text = row.designation
            if (row.isSelf) {
                b.teamQYou.visibility = android.view.View.VISIBLE
                WsStatus.badge(b.teamQYou, ctx.getString(R.string.team_you), ContextCompat.getColor(ctx, R.color.ws_primary))
            } else {
                b.teamQYou.visibility = android.view.View.GONE
            }
            WsStatus.badge(b.teamQAvailable, row.available, ContextCompat.getColor(ctx, R.color.ws_green))
            b.teamQChevron.setImageResource(if (row.expanded) R.drawable.ws_ic_expand_less else R.drawable.ws_ic_expand_more)
            b.teamQHeader.setOnClickListener { onQuota(row.key) }

            b.teamQBody.visibility = if (row.expanded) android.view.View.VISIBLE else android.view.View.GONE
            b.teamQGrid.removeAllViews()
            if (!row.expanded) return
            b.teamQNone.visibility = if (row.tiles.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
            val gap = ctx.resources.getDimensionPixelSize(SdpR.dimen._5sdp)
            val radius = ctx.resources.getDimensionPixelSize(SdpR.dimen._8sdp).toFloat()
            val hairline = maxOf(1, ctx.resources.displayMetrics.density.toInt())
            val inflater = LayoutInflater.from(ctx)
            row.tiles.forEach { tile ->
                val tb = TeamItemQuotaTileBinding.inflate(inflater, b.teamQGrid, false)
                tb.teamTileValue.text = tile.value
                tb.teamTileValue.setTextColor(tile.color)
                tb.teamTileName.text = tile.name
                tb.teamTileDetail.text = tile.detail
                tb.root.background = GradientDrawable().apply {
                    cornerRadius = radius
                    setColor(WsStatus.tint(tile.color, 8))
                    setStroke(hairline, WsStatus.tint(tile.color, 40))
                }
                val lp = GridLayout.LayoutParams(
                    GridLayout.spec(GridLayout.UNDEFINED, 1f),
                    GridLayout.spec(GridLayout.UNDEFINED, 1f)
                ).apply {
                    width = 0
                    setMargins(gap / 2, gap / 2, gap / 2, gap / 2)
                }
                b.teamQGrid.addView(tb.root, lp)
            }
        }
    }

    private object Diff : DiffUtil.ItemCallback<TeamRow>() {
        override fun areItemsTheSame(a: TeamRow, b: TeamRow) = a::class == b::class && a.key == b.key
        override fun areContentsTheSame(a: TeamRow, b: TeamRow) = a == b
    }

    private companion object {
        const val TYPE_ARRIVAL = 0
        const val TYPE_MEMBER = 1
        const val TYPE_QUOTA = 2
    }
}
