package com.axelliant.hris.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.axelliant.hris.R
import com.axelliant.hris.databinding.CertItemCardBinding
import com.axelliant.hris.model.payslip.cleaned
import com.axelliant.hris.model.profile.CertificationDisplay
import com.axelliant.hris.model.profile.ProfileCertification
import com.axelliant.hris.ui.WsFormat
import com.axelliant.hris.ui.WsStatus
import com.axelliant.hris.ui.WsTone

/** Certification cards: title, coloured status, authority/expiry line, renewal hint and credential link. */
class CertificationAdapter(private val onOpenLink: (String) -> Unit) : FooterListAdapter<ProfileCertification>() {

    override fun isSameItem(old: ProfileCertification, new: ProfileCertification) =
        old.name == new.name && old.certification_name == new.certification_name

    override fun createItemHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        CardHolder(CertItemCardBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun bindItem(holder: RecyclerView.ViewHolder, item: ProfileCertification) {
        (holder as CardHolder).bind(item)
    }

    private inner class CardHolder(private val binding: CertItemCardBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ProfileCertification) {
            val context = binding.root.context
            WsStatus.iconBadge(binding.certIcon, WsTone.request(context))
            binding.certTitle.text = CertificationDisplay.title(item, context.getString(R.string.cert_default_title))

            val status = item.status.cleaned() ?: context.getString(R.string.cert_status_unknown)
            val label = if (item.needsRenewal) context.getString(R.string.cert_status_renewal_due, status) else status
            WsStatus.badge(binding.certStatus, label, toneColor(context, CertificationDisplay.tone(item)))

            binding.certMeta.text = meta(context, item)
            binding.certHint.text = hint(context, item)

            val link = CertificationDisplay.credentialLink(item)
            binding.certLink.visibility = if (link != null) View.VISIBLE else View.GONE
            binding.certLink.setOnClickListener { link?.let(onOpenLink) }
        }
    }

    private fun toneColor(context: Context, tone: CertificationDisplay.Tone): Int = context.getColor(
        when (tone) {
            CertificationDisplay.Tone.RED -> R.color.ws_red
            CertificationDisplay.Tone.AMBER -> R.color.ws_amber
            CertificationDisplay.Tone.GREEN -> R.color.ws_green
            CertificationDisplay.Tone.GREY -> R.color.ws_grey
        }
    )

    /** "Amazon Web Services · ID ABC123 · Expires 31-01-2027", or "No expiry date". */
    private fun meta(context: Context, item: ProfileCertification): String {
        val expiry = item.expiry_date.cleaned()?.let {
            context.getString(R.string.cert_meta_expires, WsFormat.reversedDay(it))
        } ?: context.getString(R.string.cert_no_expiry)
        return listOfNotNull(
            item.issuing_authority.cleaned(),
            item.credential_id.cleaned()?.let { context.getString(R.string.cert_meta_id, it) },
            expiry
        ).joinToString(" · ")
    }

    private fun hint(context: Context, item: ProfileCertification): String {
        val days = item.daysUntilExpiry
        return when {
            item.isExpired -> context.getString(R.string.cert_hint_overdue)
            item.isExpiringSoon && days != null ->
                context.resources.getQuantityString(R.plurals.cert_hint_renews_in, days, days)
            days != null && days > 0 ->
                context.resources.getQuantityString(R.plurals.cert_hint_until_expiry, days, days)
            item.renewalRequired -> context.getString(R.string.cert_hint_keep_up)
            else -> context.getString(R.string.cert_hint_none)
        }
    }
}
