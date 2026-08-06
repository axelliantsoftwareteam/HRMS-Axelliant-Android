package com.axelliant.hris.features.quotes.presentation

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import com.axelliant.hris.R
import com.axelliant.hris.databinding.BottomSheetQuoteProductActionsBinding
import com.axelliant.hris.databinding.ItemQuoteDeliveryScheduleRowBinding
import com.axelliant.hris.features.quotes.domain.model.QuoteAddressUi
import com.axelliant.hris.features.quotes.domain.model.QuoteCreationProductUi
import com.axelliant.hris.features.quotes.domain.model.QuoteProductDeliveryScheduleUi
import com.axelliant.hris.features.quotes.domain.model.QuoteProductScheduleHelper
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.UUID

class QuoteProductActionsBottomSheet(
    private val fragment: Fragment,
    private val initialProduct: QuoteCreationProductUi,
    private val shippingAddresses: List<QuoteAddressUi>,
    private val defaultShippingAddress: QuoteAddressUi?,
    private val quoteDeliveryDate: String,
    private val formatCurrency: (Double) -> String,
    private val onApply: (QuoteCreationProductUi) -> Unit,
    private val onDelete: () -> Unit
) {

    private var dialog: BottomSheetDialog? = null
    private lateinit var binding: BottomSheetQuoteProductActionsBinding
    private var workingProduct: QuoteCreationProductUi = initialProduct
    private var isBindingDeliveryRows = false

    fun show() {
        workingProduct = prepareWorkingProduct(initialProduct)
        val inflater = LayoutInflater.from(fragment.requireContext())
        binding = BottomSheetQuoteProductActionsBinding.inflate(inflater)
        val sheetDialog = BottomSheetDialog(fragment.requireContext())
        dialog = sheetDialog

        bindHeader()
        bindQuantityStepper()
        bindBasePriceInput()
        bindActions()
        refreshSummary()
        renderDeliverySchedules()

        sheetDialog.setContentView(binding.root)
        sheetDialog.window?.let { window ->
            WindowCompat.setDecorFitsSystemWindows(window, false)
            window.navigationBarColor = ContextCompat.getColor(
                fragment.requireContext(),
                R.color.ds_surface
            )
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

            val behavior = BottomSheetBehavior.from(bottomSheet)
            behavior.skipCollapsed = true
            behavior.isFitToContents = false
            behavior.expandedOffset = 0
            behavior.state = BottomSheetBehavior.STATE_EXPANDED

            val bottomInsetPadding = fragment.resources.getDimensionPixelSize(R.dimen.ds_space_16)
            ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, windowInsets ->
                val navBarInsets = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars())
                view.updatePadding(bottom = navBarInsets.bottom + bottomInsetPadding)
                windowInsets
            }
            ViewCompat.requestApplyInsets(binding.root)
        }
        sheetDialog.show()
    }

    private fun bindHeader() {
        binding.productTitleText.text = workingProduct.name
        binding.closeSheetButton.setOnClickListener { dialog?.dismiss() }
    }

    private fun bindQuantityStepper() {
        binding.decreaseQtyButton.setOnClickListener {
            if (workingProduct.quantity > 1) {
                workingProduct = workingProduct.copy(quantity = workingProduct.quantity - 1)
                syncPrimaryDeliveryQuantity()
                refreshSummary()
                refreshScheduleWarning()
            }
        }
        binding.increaseQtyButton.setOnClickListener {
            workingProduct = workingProduct.copy(quantity = workingProduct.quantity + 1)
            syncPrimaryDeliveryQuantity()
            refreshSummary()
            refreshScheduleWarning()
        }
    }

    private fun syncPrimaryDeliveryQuantity() {
        if (workingProduct.deliverySchedules.size != 1) return
        val schedule = workingProduct.deliverySchedules.first()
        workingProduct = workingProduct.copy(
            deliverySchedules = listOf(schedule.copy(quantity = workingProduct.quantity))
        )
        renderDeliverySchedules()
    }

    private fun bindBasePriceInput() {
        binding.basePriceInput.setText(formatBasePriceInput(workingProduct.effectiveBasePrice))
        binding.basePriceInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                binding.basePriceInputLayout.isErrorEnabled = false
                binding.basePriceInputLayout.error = null
            }
        })
    }

    private fun bindActions() {
        binding.addDeliveryScheduleButton.setOnClickListener { addDeliverySchedule() }
        binding.deleteProductButton.setOnClickListener {
            showDeleteConfirmationDialog()
        }
        binding.applyProductActionsButton.setOnClickListener { applyChanges() }
    }

    private fun showDeleteConfirmationDialog() {
        MaterialAlertDialogBuilder(fragment.requireContext())
            .setTitle(R.string.add_quote_delete_product_confirm_title)
            .setMessage(
                fragment.getString(
                    R.string.add_quote_delete_product_confirm_message,
                    workingProduct.name
                )
            )
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.add_quote_delete_product_confirm_action) { _, _ ->
                onDelete()
                dialog?.dismiss()
            }
            .show()
    }

    private fun refreshSummary() {
        binding.quantityText.text = workingProduct.quantity.toString()
        binding.unitPriceText.text = formatCurrency(workingProduct.unitPrice)
        binding.lineTotalText.text = formatCurrency(workingProduct.lineTotal)
        val scheduleCount = workingProduct.deliverySchedules.size
        binding.scheduleCountText.text = fragment.getString(
            R.string.add_quote_schedules_count_format,
            scheduleCount
        )
        refreshScheduleWarning()
    }

    private fun refreshScheduleWarning() {
        val product = workingProduct
        if (product.isOverScheduled) {
            binding.scheduleWarningText.isVisible = true
            binding.scheduleWarningText.text = fragment.getString(
                R.string.add_quote_over_scheduled_format,
                product.overScheduledUnits
            )
        } else {
            binding.scheduleWarningText.isVisible = false
        }
    }

    private fun renderDeliverySchedules() {
        isBindingDeliveryRows = true
        binding.deliverySchedulesContainer.removeAllViews()
        val schedules = workingProduct.deliverySchedules
        binding.deliveryScheduleEmptyText.isVisible = schedules.isEmpty()
        schedules.forEachIndexed { index, schedule ->
            val rowBinding = ItemQuoteDeliveryScheduleRowBinding.inflate(
                LayoutInflater.from(fragment.requireContext()),
                binding.deliverySchedulesContainer,
                false
            )
            bindDeliveryRow(rowBinding, index, schedule)
            binding.deliverySchedulesContainer.addView(rowBinding.root)
        }
        isBindingDeliveryRows = false
    }

    private fun bindDeliveryRow(
        rowBinding: ItemQuoteDeliveryScheduleRowBinding,
        index: Int,
        schedule: QuoteProductDeliveryScheduleUi
    ) {
        val isFirstRow = index == 0
        rowBinding.deliveryLabelText.text = fragment.getString(
            R.string.add_quote_delivery_number_format,
            index + 1
        )
        rowBinding.shippingAddressText.text = schedule.shippingAddress.displayTextWithLocation
        rowBinding.defaultAddressHintText.isVisible = isFirstRow
        rowBinding.deleteDeliveryButton.isVisible = !isFirstRow
        rowBinding.shippingAddressDropdownIcon.isVisible = !isFirstRow
        rowBinding.shippingAddressField.isEnabled = !isFirstRow

        if (!isFirstRow) {
            rowBinding.shippingAddressField.setOnClickListener {
                showAddressPicker(index)
            }
        } else {
            rowBinding.shippingAddressField.setOnClickListener(null)
        }

        rowBinding.deleteDeliveryButton.setOnClickListener {
            removeDeliverySchedule(index)
        }

        rowBinding.deliveryQtyInput.setText(schedule.quantity.toString())
        rowBinding.deliveryQtyInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                if (isBindingDeliveryRows) return
                val qty = s?.toString()?.toIntOrNull()?.coerceAtLeast(1) ?: 1
                updateDeliverySchedule(index) { it.copy(quantity = qty) }
                rowBinding.deliveryLineTotalText.text = formatCurrency(
                    workingProduct.unitPrice * qty
                )
                refreshScheduleWarning()
            }
        })

        rowBinding.deliveryDateInput.setText(schedule.estimatedDeliveryDate)
        rowBinding.deliveryDateInputLayout.setEndIconOnClickListener {
            showDeliveryDatePicker(index)
        }
        rowBinding.deliveryDateInput.setOnClickListener {
            showDeliveryDatePicker(index)
        }

        rowBinding.deliveryLineTotalText.text = formatCurrency(
            workingProduct.unitPrice * schedule.quantity
        )
    }

    private fun showAddressPicker(scheduleIndex: Int) {
        if (shippingAddresses.isEmpty()) return
        val pickerDialog = BottomSheetDialog(fragment.requireContext())
        val container = LinearLayout(fragment.requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.bg_filter_sheet)
            setPadding(
                fragment.resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._14sdp),
                fragment.resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._14sdp),
                fragment.resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._14sdp),
                fragment.resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._16sdp)
            )
        }
        val titleView = TextView(fragment.requireContext()).apply {
            text = fragment.getString(R.string.add_quote_shipping_address_label)
            setTextColor(ContextCompat.getColor(fragment.requireContext(), R.color.ds_text_primary))
            typeface = fragment.resources.getFont(R.font.poppins_semibold)
            textSize = 16f
        }
        container.addView(titleView)
        shippingAddresses.forEach { address ->
            val row = TextView(fragment.requireContext()).apply {
                text = address.displayTextWithLocation
                setTextColor(ContextCompat.getColor(fragment.requireContext(), R.color.ds_text_primary))
                typeface = fragment.resources.getFont(R.font.poppins_regular)
                textSize = 12f
                gravity = android.view.Gravity.CENTER_VERTICAL
                background = ContextCompat.getDrawable(fragment.requireContext(), R.drawable.bg_filter_field)
                setPadding(
                    fragment.resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._10sdp),
                    0,
                    fragment.resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._10sdp),
                    0
                )
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    fragment.resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._44sdp)
                ).apply {
                    topMargin = fragment.resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._6sdp)
                }
                setOnClickListener {
                    updateDeliverySchedule(scheduleIndex) {
                        it.copy(shippingAddress = address, isDefaultAddress = false)
                    }
                    renderDeliverySchedules()
                    pickerDialog.dismiss()
                }
            }
            container.addView(row)
        }
        pickerDialog.setContentView(container)
        pickerDialog.show()
    }

    private fun showDeliveryDatePicker(scheduleIndex: Int) {
        MaterialDatePicker.Builder.datePicker()
            .setTitleText(fragment.getString(R.string.add_quote_est_delivery_date))
            .build()
            .apply {
                addOnPositiveButtonClickListener { utcMillis ->
                    val formatted = DISPLAY_DATE_FORMAT.format(utcMillis)
                    updateDeliverySchedule(scheduleIndex) {
                        it.copy(estimatedDeliveryDate = formatted)
                    }
                    renderDeliverySchedules()
                }
            }
            .show(fragment.parentFragmentManager, "$DATE_PICKER_TAG$scheduleIndex")
    }

    private fun addDeliverySchedule() {
        val fallbackAddress = shippingAddresses.firstOrNull { address ->
            address.id != defaultShippingAddress?.id
        } ?: shippingAddresses.firstOrNull() ?: defaultShippingAddress ?: return

        val newSchedule = QuoteProductDeliveryScheduleUi(
            id = UUID.randomUUID().toString(),
            shippingAddress = fallbackAddress,
            quantity = 1,
            isDefaultAddress = false
        )
        workingProduct = workingProduct.copy(
            deliverySchedules = workingProduct.deliverySchedules + newSchedule
        )
        refreshSummary()
        renderDeliverySchedules()
    }

    private fun removeDeliverySchedule(index: Int) {
        if (index == 0) return
        workingProduct = workingProduct.copy(
            deliverySchedules = workingProduct.deliverySchedules.filterIndexed { i, _ -> i != index }
        )
        refreshSummary()
        renderDeliverySchedules()
    }

    private fun updateDeliverySchedule(
        index: Int,
        transform: (QuoteProductDeliveryScheduleUi) -> QuoteProductDeliveryScheduleUi
    ) {
        workingProduct = workingProduct.copy(
            deliverySchedules = workingProduct.deliverySchedules.mapIndexed { i, schedule ->
                if (i == index) transform(schedule) else schedule
            }
        )
    }

    private fun applyChanges() {
        val basePriceText = binding.basePriceInput.text?.toString()?.trim().orEmpty()
        val basePrice = basePriceText.toDoubleOrNull()
        if (basePrice == null || basePrice <= 0.0) {
            binding.basePriceInputLayout.isErrorEnabled = true
            binding.basePriceInputLayout.error =
                fragment.getString(R.string.add_quote_validation_base_price)
            return
        }

        onApply(
            QuoteProductScheduleHelper.ensureSchedules(
                product = workingProduct.copy(
                    basePrice = basePrice,
                    deliverySchedules = workingProduct.deliverySchedules.map { schedule ->
                        schedule.copy(quantity = schedule.quantity.coerceAtLeast(1))
                    }
                ),
                quoteShippingAddress = defaultShippingAddress,
                quoteDeliveryDate = quoteDeliveryDate
            )
        )
        dialog?.dismiss()
    }

    private fun formatBasePriceInput(value: Double): String {
        return if (value % 1.0 == 0.0) {
            value.toLong().toString()
        } else {
            value.toString()
        }
    }

    private fun prepareWorkingProduct(product: QuoteCreationProductUi): QuoteCreationProductUi {
        return QuoteProductScheduleHelper.ensureSchedules(
            product = product,
            quoteShippingAddress = defaultShippingAddress ?: shippingAddresses.firstOrNull(),
            quoteDeliveryDate = quoteDeliveryDate
        )
    }

    companion object {
        private const val DATE_PICKER_TAG = "quote_product_delivery_date_"
        private val DISPLAY_DATE_FORMAT = SimpleDateFormat("MM/dd/yyyy", Locale.US)
    }
}



