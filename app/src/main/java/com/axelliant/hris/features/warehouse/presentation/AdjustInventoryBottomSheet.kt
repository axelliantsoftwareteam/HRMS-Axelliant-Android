package com.axelliant.hris.features.warehouse.presentation

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.text.Editable
import android.text.TextWatcher
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.res.use
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import com.axelliant.hris.R
import com.axelliant.hris.databinding.BottomSheetAdjustInventoryBinding
import com.axelliant.hris.features.warehouse.domain.model.InventoryModel
import com.axelliant.hris.ui.designsystem.components.createAppBottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import java.util.Locale

class AdjustInventoryBottomSheet(
    private val fragment: Fragment
) {
    private var dialog: BottomSheetDialog? = null
    private var binding: BottomSheetAdjustInventoryBinding? = null
    private var selectedReason: AdjustmentReason? = null
    private var activeReasonPopup: PopupWindow? = null

    private val reasons: List<AdjustmentReason>
        get() = listOf(
            AdjustmentReason(1, fragment.getString(R.string.inventory_adjust_reason_lost)),
            AdjustmentReason(2, fragment.getString(R.string.inventory_adjust_reason_found)),
            AdjustmentReason(3, fragment.getString(R.string.inventory_adjust_reason_count_correction)),
            AdjustmentReason(4, fragment.getString(R.string.inventory_adjust_reason_damage)),
            AdjustmentReason(5, fragment.getString(R.string.inventory_adjust_reason_data_correction)),
            AdjustmentReason(6, fragment.getString(R.string.inventory_adjust_reason_opening_balance)),
            AdjustmentReason(7, fragment.getString(R.string.inventory_adjust_reason_other))
        )

    fun show(inventory: InventoryModel) {
        if (dialog?.isShowing == true) return

        val inflater = LayoutInflater.from(fragment.requireContext())
        val sheetBinding = BottomSheetAdjustInventoryBinding.inflate(inflater)
        binding = sheetBinding
        selectedReason = null

        val sheetDialog = fragment.requireContext().createAppBottomSheetDialog()
        dialog = sheetDialog

        bindInventoryContext(sheetBinding, inventory)
        bindActions(sheetBinding, inventory, sheetDialog)
        bindInputWatchers(sheetBinding, inventory)
        renderReason(sheetBinding, hasError = false)
        renderSerialSection(sheetBinding, inventory)

        sheetDialog.setContentView(sheetBinding.root)
        sheetDialog.window?.let { window ->
            WindowCompat.setDecorFitsSystemWindows(window, false)
            window.navigationBarColor = ContextCompat.getColor(
                fragment.requireContext(),
                R.color.ds_surface
            )
        }
        sheetDialog.setOnDismissListener {
            activeReasonPopup?.dismiss()
            activeReasonPopup = null
            binding = null
            dialog = null
            selectedReason = null
        }
        sheetDialog.setOnShowListener {
            val bottomSheet = sheetDialog.findViewById<FrameLayout>(
                com.google.android.material.R.id.design_bottom_sheet
            ) ?: return@setOnShowListener

            bottomSheet.background = ContextCompat.getDrawable(
                fragment.requireContext(),
                R.drawable.bg_filter_sheet
            )
            bottomSheet.layoutParams = bottomSheet.layoutParams.apply {
                height = ViewGroup.LayoutParams.MATCH_PARENT
            }
            BottomSheetBehavior.from(bottomSheet).apply {
                state = BottomSheetBehavior.STATE_EXPANDED
                skipCollapsed = true
            }

            ViewCompat.setOnApplyWindowInsetsListener(sheetBinding.root) { view, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                view.updatePadding(bottom = systemBars.bottom)
                insets
            }
        }
        sheetDialog.show()
    }

    fun dismiss() {
        dialog?.dismiss()
    }

    private fun bindInventoryContext(
        binding: BottomSheetAdjustInventoryBinding,
        inventory: InventoryModel
    ) {
        binding.productText.text = inventory.productName.displayValue()
        binding.currentOnHandText.text = inventory.quantityOnHand.formatQuantity()
    }

    private fun bindActions(
        binding: BottomSheetAdjustInventoryBinding,
        inventory: InventoryModel,
        sheetDialog: BottomSheetDialog
    ) {
        binding.closeButton.setOnClickListener { sheetDialog.dismiss() }
        binding.cancelButton.setOnClickListener { sheetDialog.dismiss() }
        binding.reasonFieldInclude.selectField.setOnClickListener {
            showReasonDropdown(binding)
        }
        binding.saveButton.setOnClickListener {
            if (validate(binding, inventory)) {
                Toast.makeText(
                    fragment.requireContext(),
                    R.string.inventory_adjust_save_not_connected,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun bindInputWatchers(
        binding: BottomSheetAdjustInventoryBinding,
        inventory: InventoryModel
    ) {
        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(editable: Editable?) {
                clearInputErrors(binding)
                renderSerialSection(binding, inventory)
            }
        }
        binding.quantityDeltaInput.addTextChangedListener(watcher)
        binding.notesInput.addTextChangedListener(watcher)
        binding.serialNumbersInput.addTextChangedListener(watcher)
    }

    private fun showReasonDropdown(binding: BottomSheetAdjustInventoryBinding) {
        activeReasonPopup?.dismiss()
        binding.reasonFieldInclude.selectField.setBackgroundResource(R.drawable.bg_filter_field_focused)
        val optionsContainer = LinearLayout(fragment.requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.bg_filter_dropdown_panel)
            setPadding(
                fragment.resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._8sdp),
                fragment.resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._6sdp),
                fragment.resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._8sdp),
                fragment.resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._6sdp)
            )
        }
        reasons.forEach { reason ->
            optionsContainer.addView(createReasonRow(reason) {
                selectedReason = reason
                activeReasonPopup?.dismiss()
                renderReason(binding, hasError = false)
            })
        }
        val popupHeight = fragment.resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._36sdp) *
            reasons.size.coerceAtMost(MAX_VISIBLE_REASON_OPTIONS)
        val popup = PopupWindow(
            optionsContainer,
            binding.reasonFieldInclude.selectField.width,
            popupHeight,
            true
        ).apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            elevation = fragment.resources.getDimension(com.intuit.sdp.R.dimen._8sdp)
            isOutsideTouchable = true
            setOnDismissListener {
                if (activeReasonPopup === this) activeReasonPopup = null
                if (binding.reasonErrorText.isVisible) {
                    binding.reasonFieldInclude.selectField.setBackgroundResource(R.drawable.bg_filter_field_error)
                } else {
                    binding.reasonFieldInclude.selectField.setBackgroundResource(R.drawable.bg_filter_field)
                }
            }
        }
        activeReasonPopup = popup
        popup.showAsDropDown(
            binding.reasonFieldInclude.selectField,
            0,
            fragment.resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._4sdp)
        )
    }

    private fun createReasonRow(
        reason: AdjustmentReason,
        onClick: () -> Unit
    ): View {
        return TextView(fragment.requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                fragment.resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._36sdp)
            )
            gravity = Gravity.CENTER_VERTICAL
            text = reason.label
            setTextColor(ContextCompat.getColor(fragment.requireContext(), R.color.ds_text_primary))
            setTextSize(
                TypedValue.COMPLEX_UNIT_PX,
                fragment.resources.getDimension(com.intuit.ssp.R.dimen._12ssp)
            )
            setPadding(
                fragment.resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._10sdp),
                0,
                fragment.resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._10sdp),
                0
            )
            foreground = context.obtainStyledAttributes(intArrayOf(android.R.attr.selectableItemBackground))
                .use { it.getDrawable(0) }
            setOnClickListener { onClick() }
        }
    }

    private fun validate(
        binding: BottomSheetAdjustInventoryBinding,
        inventory: InventoryModel
    ): Boolean {
        clearInputErrors(binding)
        val delta = binding.quantityDeltaInput.text?.toString().orEmpty().trim().toIntOrNull()
        val notes = binding.notesInput.text?.toString().orEmpty().trim()
        var isValid = true

        if (selectedReason == null) {
            renderReason(binding, hasError = true)
            isValid = false
        }
        if (delta == null || delta == 0) {
            binding.quantityDeltaInputLayout.error =
                fragment.getString(R.string.inventory_adjust_quantity_delta_error)
            isValid = false
        }
        if (notes.isBlank()) {
            binding.notesInputLayout.error =
                fragment.getString(R.string.inventory_adjust_notes_error)
            isValid = false
        }
        val requiredSerialCount = delta?.let { kotlin.math.abs(it) } ?: 0
        if (inventory.isSerialTracked && requiredSerialCount > 0) {
            serialValidationError(binding, requiredSerialCount)?.let { error ->
                binding.serialNumbersInputLayout.error = error
                isValid = false
            }
        }
        renderSerialSection(binding, inventory)
        return isValid
    }

    private fun clearInputErrors(binding: BottomSheetAdjustInventoryBinding) {
        binding.quantityDeltaInputLayout.error = null
        binding.notesInputLayout.error = null
        binding.serialNumbersInputLayout.error = null
    }

    private fun renderReason(
        binding: BottomSheetAdjustInventoryBinding,
        hasError: Boolean
    ) {
        binding.reasonFieldInclude.selectFieldText.text =
            selectedReason?.label ?: fragment.getString(R.string.inventory_adjust_reason_placeholder)
        binding.reasonFieldInclude.selectFieldText.setTextColor(
            ContextCompat.getColor(
                fragment.requireContext(),
                if (selectedReason == null) R.color.ds_text_muted else R.color.ds_text_primary
            )
        )
        binding.reasonErrorText.isVisible = hasError
        binding.reasonFieldInclude.selectField.setBackgroundResource(
            if (hasError) R.drawable.bg_filter_field_error else R.drawable.bg_filter_field
        )
    }

    private fun renderSerialSection(
        binding: BottomSheetAdjustInventoryBinding,
        inventory: InventoryModel
    ) {
        val delta = binding.quantityDeltaInput.text?.toString().orEmpty().trim().toIntOrNull()
        val requiredCount = delta?.let { kotlin.math.abs(it) } ?: 0
        val shouldShowSerials = inventory.isSerialTracked && requiredCount > 0
        binding.serialSection.isVisible = shouldShowSerials
        if (!shouldShowSerials) return

        binding.serialSectionTitle.text = when {
            delta == null -> fragment.getString(R.string.inventory_adjust_serial_numbers_title)
            delta > 0 -> fragment.getString(R.string.inventory_adjust_serial_numbers_add_title)
            else -> fragment.getString(R.string.inventory_adjust_serial_numbers_remove_title)
        }
        val enteredCount = parseSerialNumbers(binding).size
        val isExactCount = enteredCount == requiredCount && serialValidationError(binding, requiredCount) == null
        binding.serialCountBadge.text = fragment.getString(
            R.string.inventory_adjust_serial_count_format,
            enteredCount,
            requiredCount
        )
        binding.serialHelpText.text = fragment.resources.getQuantityString(
            R.plurals.inventory_adjust_serial_exact_count,
            requiredCount,
            requiredCount
        )
        binding.serialCountBadge.setBackgroundResource(
            if (isExactCount) R.drawable.bg_quote_status_approved else R.drawable.bg_quote_status_draft
        )
        binding.serialCountBadge.backgroundTintList = ColorStateList.valueOf(
            ContextCompat.getColor(
                fragment.requireContext(),
                if (isExactCount) R.color.quotes_status_approved_bg else R.color.quotes_status_draft_bg
            )
        )
        binding.serialCountBadge.setTextColor(
            ContextCompat.getColor(
                fragment.requireContext(),
                if (isExactCount) R.color.quotes_status_approved_text else R.color.quotes_status_draft_text
            )
        )
    }

    private fun serialValidationError(
        binding: BottomSheetAdjustInventoryBinding,
        requiredCount: Int
    ): String? {
        val serials = parseSerialNumbers(binding)
        val uniqueCount = serials.map { it.lowercase(Locale.US) }.toSet().size
        return when {
            serials.size != uniqueCount -> fragment.getString(R.string.inventory_adjust_serial_unique_error)
            serials.size != requiredCount -> fragment.resources.getQuantityString(
                R.plurals.inventory_adjust_serial_count_error,
                requiredCount,
                requiredCount,
                serials.size
            )
            else -> null
        }
    }

    private fun parseSerialNumbers(binding: BottomSheetAdjustInventoryBinding): List<String> {
        return binding.serialNumbersInput.text
            ?.toString()
            .orEmpty()
            .split(Regex("[,\\n\\r]+"))
            .map { it.trim() }
            .filter { it.isNotBlank() }
    }

    private fun String.displayValue(): String {
        return takeIf { it.isNotBlank() && it != "-" }
            ?: fragment.getString(R.string.warehouse_not_available)
    }

    private fun Double.formatQuantity(): String {
        return if (this % 1.0 == 0.0) {
            toInt().toString()
        } else {
            String.format(Locale.US, "%.2f", this)
        }
    }

    private data class AdjustmentReason(
        val id: Int,
        val label: String
    )

    private companion object {
        const val MAX_VISIBLE_REASON_OPTIONS = 6
    }
}
