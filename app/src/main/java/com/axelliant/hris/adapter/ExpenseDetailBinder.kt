package com.axelliant.hris.adapter

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.axelliant.hris.databinding.ExpItemLineBinding
import com.axelliant.hris.databinding.ExpViewClaimDetailBinding
import com.axelliant.hris.model.expense.ImageType
import com.axelliant.hris.model.requests.ExpenseClaim
import com.axelliant.hris.model.requests.toAmount
import com.axelliant.hris.navigation.AppNavigator
import com.axelliant.hris.ui.WsFormat
import com.google.gson.Gson
import java.util.Locale

/** Fills the line items and attachments of one expense claim (shared by the claim and approval lists). */
object ExpenseDetailBinder {

    private const val DASH = "--"

    fun bind(detail: ExpViewClaimDetailBinding, claim: ExpenseClaim, onAttachmentClick: (Int) -> Unit) {
        val inflater = LayoutInflater.from(detail.root.context)

        detail.expLines.removeAllViews()
        val lines = claim.expenses_detail.orEmpty()
        lines.forEachIndexed { index, line ->
            val row = ExpItemLineBinding.inflate(inflater, detail.expLines, false)
            row.expLineType.text = line.expense_type.capitalizedWords().ifEmpty { DASH }
            row.expLineDate.text = WsFormat.date(line.expense_date).ifEmpty { DASH }
            row.expLineAmount.text = amountText(line.amount)
            val description = line.description.capitalizedWords()
            row.expLineDesc.text = description
            row.expLineDescGroup.visibility = if (description.isEmpty()) View.GONE else View.VISIBLE
            row.expLineDivider.visibility = if (index == lines.lastIndex) View.GONE else View.VISIBLE
            detail.expLines.addView(row.root)
        }

        val attachments = claim.attachments.orEmpty().filter { !it.file_url.isNullOrBlank() }
        detail.expAttachGroup.visibility = if (attachments.isEmpty()) View.GONE else View.VISIBLE
        if (attachments.isNotEmpty()) {
            val thumbnails = attachments.map {
                ImageType(imageUrl = WsFormat.mediaUrl(it.file_url), file_id = it.name)
            }
            detail.expAttachList.layoutManager =
                LinearLayoutManager(detail.root.context, LinearLayoutManager.HORIZONTAL, false)
            detail.expAttachList.adapter = AttachmentsAdapter(
                showRemove = false,
                onClick = { _, position -> onAttachmentClick(position) }
            ).also { it.submitList(thumbnails) }
        }
    }

    /** Opens the attachments of [claim] in the image viewer, starting at [index]. */
    fun openPreview(claim: ExpenseClaim, index: Int) {
        val attachments = claim.attachmentList().filter { !it.file_url.isNullOrBlank() }
        if (attachments.isEmpty()) return
        AppNavigator.navigateToImageDetailFragment(Bundle().apply {
            putString("images", Gson().toJson(attachments))
            putInt("index", index)
        })
    }

    /** "500" for a whole amount, "12.50" otherwise. */
    fun amountText(value: Any?): String {
        val amount = value.toAmount()
        return if (amount % 1.0 == 0.0) amount.toLong().toString() else String.format(Locale.US, "%.2f", amount)
    }

    /** "office supplies" -> "Office Supplies". */
    fun String?.capitalizedWords(): String =
        this.orEmpty().trim().lowercase().split(Regex("\\s+")).joinToString(" ") { word ->
            word.replaceFirstChar { it.uppercase() }
        }
}
