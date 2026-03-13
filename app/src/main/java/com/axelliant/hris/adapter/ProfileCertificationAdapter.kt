package com.axelliant.hris.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.axelliant.hris.R
import com.axelliant.hris.databinding.ItemProfileCertificationBinding
import com.axelliant.hris.model.profile.ProfileCertification

class ProfileCertificationAdapter : RecyclerView.Adapter<ProfileCertificationAdapter.CertificationViewHolder>() {
    private val items = mutableListOf<ProfileCertification>()

    fun submitList(newItems: List<ProfileCertification>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CertificationViewHolder {
        val binding = ItemProfileCertificationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CertificationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CertificationViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class CertificationViewHolder(private val binding: ItemProfileCertificationBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ProfileCertification) {
            binding.tvCertificationName.text = item.certification_name ?: "Certification"
            binding.tvCertificationMeta.text = listOfNotNull(
                item.issuing_authority?.takeIf { it.isNotBlank() },
                item.credential_id?.takeIf { it.isNotBlank() }?.let { "ID $it" },
                item.expiry_date?.takeIf { it.isNotBlank() }?.let { "Expires $it" } ?: "No expiry"
            ).joinToString(" · ")
            binding.tvCertificationHint.text = when {
                item.is_expired == true -> "Renewal is overdue."
                item.is_expiring_soon == true -> "Renew soon to keep this credential active."
                item.renewal_required == true -> "Renewal tracking is enabled for this certification."
                else -> "No renewal action is needed right now."
            }
            binding.tvCertificationStatus.text = buildString {
                append(item.status ?: "Unknown")
                if (item.needs_renewal == true) {
                    append(" · renewal due")
                }
            }

            val context = binding.root.context
            val (backgroundRes, textColorRes) = when (item.tone) {
                "positive" -> Pair(R.drawable.bg_status_certification_positive, R.color.green)
                "warning" -> Pair(R.drawable.bg_status_certification_warning, R.color.orange)
                "critical" -> Pair(R.drawable.bg_status_certification_critical, R.color.red)
                else -> Pair(R.drawable.bg_status_certification_neutral, R.color.blue)
            }

            binding.tvCertificationStatus.background = ContextCompat.getDrawable(context, backgroundRes)
            binding.tvCertificationStatus.setTextColor(ContextCompat.getColor(context, textColorRes))
        }
    }
}
