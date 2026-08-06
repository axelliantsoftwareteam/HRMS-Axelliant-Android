package com.axelliant.hris.features.purchaseorders.presentation

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextUtils
import android.text.TextWatcher
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.axelliant.hris.R
import com.axelliant.hris.core.extensions.enableClearTextButton
import com.axelliant.hris.core.ui.UiState
import com.axelliant.hris.databinding.FragmentAddPurchaseOrderBinding
import com.axelliant.hris.databinding.ItemAddPoProductBinding
import com.axelliant.hris.features.purchaseorders.domain.model.AddPoProductLineUi
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AddPurchaseOrderFragment : Fragment() {

    private val viewModel: AddPurchaseOrderViewModel by viewModels()
    private var _binding: FragmentAddPurchaseOrderBinding? = null
    private val binding get() = _binding!!

    private var quoteSearchDialog: BottomSheetDialog? = null
    private var quoteOptionsContainer: LinearLayout? = null
    private var quoteSearchProgress: ProgressBar? = null
    private var quoteEmptyText: TextView? = null

    private var saleOrderSearchDialog: BottomSheetDialog? = null
    private var saleOrderOptionsContainer: LinearLayout? = null
    private var saleOrderSearchProgress: ProgressBar? = null
    private var saleOrderEmptyText: TextView? = null

    private val quantityWatchers = mutableMapOf<String, TextWatcher>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddPurchaseOrderBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.backButton.setOnClickListener { findNavController().navigateUp() }
        binding.cancelButton.setOnClickListener { findNavController().navigateUp() }
        binding.quoteField.setOnClickListener { showQuoteSearchSheet() }
        binding.clearQuoteButton.setOnClickListener { viewModel.clearSelectedQuote() }
        binding.saleOrderField.setOnClickListener {
            if (viewModel.uiState.value.selectedQuote == null) {
                Toast.makeText(
                    requireContext(),
                    R.string.add_purchase_order_select_quote_helper,
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }
            showSaleOrderSearchSheet()
        }
        binding.saleOrderInfoHeader.setOnClickListener { viewModel.toggleSaleOrderInfoExpanded() }
        binding.createButton.setOnClickListener { viewModel.createPurchaseOrder() }
        observeState()
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { state ->
                        renderForm(state)
                        renderQuoteSearchResults(state)
                        renderSaleOrderSearchResults(state)
                    }
                }
                launch {
                    viewModel.createState.collect { state ->
                        binding.loadingOverlay.isVisible = state is UiState.Loading ||
                            viewModel.uiState.value.isSaleOrderInfoLoading
                        when (state) {
                            is UiState.Success -> {
                                Toast.makeText(requireContext(), state.data, Toast.LENGTH_SHORT).show()
                                viewModel.clearCreateState()
                                findNavController().previousBackStackEntry
                                    ?.savedStateHandle
                                    ?.set(PurchaseOrdersFragment.KEY_PURCHASE_ORDER_CREATED, true)
                                findNavController().navigateUp()
                            }
                            is UiState.Error -> {
                                Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                                viewModel.clearCreateState()
                            }
                            else -> Unit
                        }
                    }
                }
            }
        }
    }

    private fun renderForm(state: AddPurchaseOrderUiState) {
        val hasQuote = state.selectedQuote != null
        binding.quoteValueText.text = state.selectedQuote?.quoteId
            ?: getString(R.string.add_purchase_order_select_quote_hint)
        binding.quoteValueText.setTextColor(selectionTextColor(hasQuote))
        binding.clearQuoteButton.isVisible = hasQuote

        val hasSaleOrder = state.selectedSaleOrder != null
        binding.saleOrderValueText.text = state.selectedSaleOrder?.orderNumber
            ?: getString(R.string.add_purchase_order_select_sales_order_hint)
        binding.saleOrderValueText.setTextColor(selectionTextColor(hasSaleOrder))
        binding.saleOrderField.isEnabled = hasQuote
        binding.saleOrderField.isClickable = hasQuote
        binding.saleOrderField.alpha = if (hasQuote) 1f else 0.6f

        setFieldError(
            field = binding.quoteField,
            errorView = binding.quoteFieldError,
            hasError = state.fieldErrors.quote,
            messageRes = R.string.add_purchase_order_validation_quote
        )
        setFieldError(
            field = binding.saleOrderField,
            errorView = binding.saleOrderFieldError,
            hasError = state.fieldErrors.saleOrder,
            messageRes = R.string.add_purchase_order_validation_sale_order,
            isFieldEnabled = hasQuote
        )

        val info = state.saleOrderInfo
        val hasProducts = state.selectedItemCount > 0
        val hasProductLines = info?.products?.isNotEmpty() == true

        binding.loadingOverlay.isVisible =
            state.isSaleOrderInfoLoading || viewModel.createState.value is UiState.Loading

        binding.saleOrderInfoBody.isVisible = state.isSaleOrderInfoExpanded
        binding.saleOrderInfoChevron.setImageResource(
            if (state.isSaleOrderInfoExpanded) R.drawable.ic_chevron_up else R.drawable.ia_ic_filter_down
        )
        binding.saleOrderInfoChevron.contentDescription = getString(
            if (state.isSaleOrderInfoExpanded) {
                R.string.add_purchase_order_collapse_info_cd
            } else {
                R.string.add_purchase_order_expand_info_cd
            }
        )
        binding.saleOrderInfoEmptyText.isVisible = info == null
        binding.saleOrderInfoContent.isVisible = info != null

        if (info != null) {
            binding.infoSaleOrderNumberText.text = info.saleOrderNumber
            binding.infoQuoteNumberText.text = info.quoteNumber
            binding.infoCustomerText.text = info.customerName
            binding.infoAmountText.text = info.amount
            binding.billingAddressText.text = info.billingAddressLines.joinToString("\n")
            binding.shippingAddressText.text = info.shippingAddressLines.joinToString("\n")
        }

        binding.selectedItemsSummaryText.text = getString(
            R.string.add_purchase_order_selected_items_format,
            state.selectedItemCount,
            state.totalSelectedQty
        )
        binding.linesSummaryText.text = getString(
            R.string.add_purchase_order_lines_summary_format,
            if (info == null) 0 else 1,
            info?.products?.size ?: 0
        )
        binding.productsFieldError.isVisible = state.fieldErrors.products
        if (state.fieldErrors.products) {
            binding.productsFieldError.text =
                getString(R.string.add_purchase_order_validation_products)
        }

        binding.emptyProductsState.isVisible = !hasProductLines
        binding.productsContainer.isVisible = hasProductLines
        if (hasProductLines) {
            renderProducts(info!!.products, state)
        } else {
            quantityWatchers.clear()
            binding.productsContainer.removeAllViews()
        }

        binding.createButton.isEnabled = viewModel.createState.value !is UiState.Loading
        binding.createButton.alpha = ENABLED_CREATE_ALPHA

        binding.subtotalText.text = state.formatCurrency(state.subtotal)
        binding.taxText.text = state.formatCurrency(state.tax)
        binding.shippingText.text = state.formatCurrency(state.shipping)
        binding.grandTotalText.text = state.formatCurrency(state.grandTotal)

        state.errorMessage?.let { message ->
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
            viewModel.clearErrorMessage()
        }
    }

    private fun renderProducts(products: List<AddPoProductLineUi>, state: AddPurchaseOrderUiState) {
        val existingIds = products.map { it.id }.toSet()
        quantityWatchers.keys.filterNot { it in existingIds }.forEach { quantityWatchers.remove(it) }
        binding.productsContainer.removeAllViews()
        products.forEach { product ->
            val itemBinding = ItemAddPoProductBinding.inflate(
                layoutInflater,
                binding.productsContainer,
                false
            )
            itemBinding.productCheckBox.setOnCheckedChangeListener(null)
            itemBinding.productCheckBox.isChecked = product.isSelected
            itemBinding.productCheckBox.setOnCheckedChangeListener { _, _ ->
                viewModel.toggleProductSelected(product.id)
            }
            itemBinding.productNameText.text = product.name
            itemBinding.axePartText.text =
                getString(R.string.add_purchase_order_axe_part_format, product.axePart)
            itemBinding.lineTotalText.text = state.formatCurrency(product.lineTotal)
            itemBinding.quantityOfText.text =
                getString(R.string.add_purchase_order_qty_of_format, product.maxQuantity)
            itemBinding.vendorText.text = product.vendor
            itemBinding.unitCostText.text = state.formatCurrency(product.unitCost)
            itemBinding.uomText.text = product.uom

            val quantityEdit = itemBinding.quantityEditText
            quantityWatchers.remove(product.id)?.let { quantityEdit.removeTextChangedListener(it) }
            if (quantityEdit.text?.toString() != product.quantity.toString()) {
                quantityEdit.setText(product.quantity.toString())
            }
            val watcher = object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
                override fun afterTextChanged(s: Editable?) {
                    if (quantityEdit.hasFocus()) {
                        viewModel.updateProductQuantity(product.id, s?.toString().orEmpty())
                    }
                }
            }
            quantityEdit.addTextChangedListener(watcher)
            quantityWatchers[product.id] = watcher

            itemBinding.vendorField.setOnClickListener {
                showOptionSheet(
                    title = getString(R.string.add_purchase_order_vendor),
                    options = product.vendors,
                    label = { it },
                    selected = { it == product.vendor },
                    onSelected = { vendor -> viewModel.selectProductVendor(product.id, vendor) }
                )
            }
            val layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._8sdp)
            }
            binding.productsContainer.addView(itemBinding.root, layoutParams)
        }
    }

    private fun setFieldError(
        field: View,
        errorView: TextView,
        hasError: Boolean,
        messageRes: Int,
        isFieldEnabled: Boolean = true
    ) {
        errorView.isVisible = hasError
        if (hasError) {
            errorView.text = getString(messageRes)
            field.setBackgroundResource(R.drawable.bg_quote_field_error)
        } else if (isFieldEnabled) {
            field.setBackgroundResource(R.drawable.bg_quote_search)
        }
    }

    private fun renderQuoteSearchResults(state: AddPurchaseOrderUiState) {
        quoteSearchProgress?.isVisible = state.isQuoteSearchLoading
        quoteEmptyText?.isVisible = !state.isQuoteSearchLoading && state.quotes.isEmpty()
        val container = quoteOptionsContainer ?: return
        container.removeAllViews()
        val selectedId = state.selectedQuote?.id
        state.quotes.forEach { quote ->
            container.addView(
                createCompactOptionRow(
                    labelText = quote.quoteId,
                    selected = quote.id == selectedId
                ) {
                    viewModel.selectQuote(quote)
                    quoteSearchDialog?.dismiss()
                }
            )
        }
    }

    private fun renderSaleOrderSearchResults(state: AddPurchaseOrderUiState) {
        saleOrderSearchProgress?.isVisible = state.isSaleOrderSearchLoading
        saleOrderEmptyText?.isVisible = !state.isSaleOrderSearchLoading && state.saleOrders.isEmpty()
        val container = saleOrderOptionsContainer ?: return
        container.removeAllViews()
        val selectedId = state.selectedSaleOrder?.id
        state.saleOrders.forEach { saleOrder ->
            container.addView(
                createCompactOptionRow(
                    labelText = saleOrder.orderNumber,
                    selected = saleOrder.id == selectedId
                ) {
                    viewModel.selectSaleOrder(saleOrder)
                    saleOrderSearchDialog?.dismiss()
                }
            )
        }
    }

    private fun showQuoteSearchSheet() {
        val dialog = BottomSheetDialog(requireContext())
        val container = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.bg_filter_sheet)
            setPadding(
                resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._14sdp),
                resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._14sdp),
                resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._14sdp),
                resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._18sdp)
            )
        }
        val titleView = TextView(requireContext()).apply {
            text = getString(R.string.add_purchase_order_select_quote)
            setTextColor(ContextCompat.getColor(requireContext(), R.color.ds_text_primary))
            typeface = resources.getFont(R.font.poppins_semibold)
            textSize = 18f
        }
        val searchInput = EditText(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._38sdp)
            ).apply {
                topMargin = resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._8sdp)
            }
            background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_filter_field)
            hint = getString(R.string.add_purchase_order_search_quotes_hint)
            inputType = InputType.TYPE_CLASS_TEXT
            isSingleLine = true
            setPadding(
                resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._12sdp),
                0,
                resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._12sdp),
                0
            )
            setTextColor(ContextCompat.getColor(requireContext(), R.color.ds_text_primary))
            setHintTextColor(ContextCompat.getColor(requireContext(), R.color.ds_text_muted))
            setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(com.intuit.ssp.R.dimen._12ssp))
            enableClearTextButton()
        }
        val progressBar = ProgressBar(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._10sdp)
            }
            isVisible = false
        }
        val emptyText = TextView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._10sdp)
            }
            text = getString(R.string.add_purchase_order_no_quotes_found)
            setTextColor(ContextCompat.getColor(requireContext(), R.color.ds_text_muted))
            typeface = resources.getFont(R.font.poppins_regular)
            textSize = 14f
            isVisible = false
        }
        val optionsContainer = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.bg_filter_dropdown_panel)
            setPadding(
                resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._8sdp),
                resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._8sdp),
                resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._8sdp),
                resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._8sdp)
            )
        }
        val optionsScrollView = ScrollView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._36sdp) * MAX_VISIBLE_OPTIONS
            ).apply {
                topMargin = resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._6sdp)
            }
            addView(optionsContainer)
        }
        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                viewModel.onQuoteSearchQueryChanged(s?.toString().orEmpty())
            }
        })
        container.addView(titleView)
        container.addView(searchInput)
        container.addView(progressBar)
        container.addView(emptyText)
        container.addView(optionsScrollView)
        dialog.setContentView(container)
        dialog.setOnShowListener {
            val bottomSheet = dialog.findViewById<FrameLayout>(
                com.google.android.material.R.id.design_bottom_sheet
            )
            bottomSheet?.let { sheet ->
                sheet.background = ColorDrawable(Color.TRANSPARENT)
                BottomSheetBehavior.from(sheet).state = BottomSheetBehavior.STATE_EXPANDED
            }
            viewModel.onQuoteSearchQueryChanged(searchInput.text?.toString().orEmpty())
        }
        dialog.setOnDismissListener {
            quoteSearchDialog = null
            quoteOptionsContainer = null
            quoteSearchProgress = null
            quoteEmptyText = null
        }
        quoteSearchDialog = dialog
        quoteOptionsContainer = optionsContainer
        quoteSearchProgress = progressBar
        quoteEmptyText = emptyText
        dialog.show()
        renderQuoteSearchResults(viewModel.uiState.value)
    }

    private fun showSaleOrderSearchSheet() {
        val dialog = BottomSheetDialog(requireContext())
        val container = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.bg_filter_sheet)
            setPadding(
                resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._14sdp),
                resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._14sdp),
                resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._14sdp),
                resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._18sdp)
            )
        }
        val titleView = TextView(requireContext()).apply {
            text = getString(R.string.add_purchase_order_select_sales_orders)
            setTextColor(ContextCompat.getColor(requireContext(), R.color.ds_text_primary))
            typeface = resources.getFont(R.font.poppins_semibold)
            textSize = 18f
        }
        val searchInput = EditText(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._38sdp)
            ).apply {
                topMargin = resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._8sdp)
            }
            background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_filter_field)
            hint = getString(R.string.add_purchase_order_search_sales_orders_hint)
            inputType = InputType.TYPE_CLASS_TEXT
            isSingleLine = true
            setPadding(
                resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._12sdp),
                0,
                resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._12sdp),
                0
            )
            setTextColor(ContextCompat.getColor(requireContext(), R.color.ds_text_primary))
            setHintTextColor(ContextCompat.getColor(requireContext(), R.color.ds_text_muted))
            setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(com.intuit.ssp.R.dimen._12ssp))
            enableClearTextButton()
        }
        val progressBar = ProgressBar(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._10sdp)
            }
            isVisible = false
        }
        val emptyText = TextView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._10sdp)
            }
            text = getString(R.string.add_purchase_order_no_sales_orders_found)
            setTextColor(ContextCompat.getColor(requireContext(), R.color.ds_text_muted))
            typeface = resources.getFont(R.font.poppins_regular)
            textSize = 14f
            isVisible = false
        }
        val optionsContainer = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.bg_filter_dropdown_panel)
            setPadding(
                resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._8sdp),
                resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._8sdp),
                resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._8sdp),
                resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._8sdp)
            )
        }
        val optionsScrollView = ScrollView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._36sdp) * MAX_VISIBLE_OPTIONS
            ).apply {
                topMargin = resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._6sdp)
            }
            addView(optionsContainer)
        }
        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                viewModel.onSaleOrderSearchQueryChanged(s?.toString().orEmpty())
            }
        })
        container.addView(titleView)
        container.addView(searchInput)
        container.addView(progressBar)
        container.addView(emptyText)
        container.addView(optionsScrollView)
        dialog.setContentView(container)
        dialog.setOnShowListener {
            val bottomSheet = dialog.findViewById<FrameLayout>(
                com.google.android.material.R.id.design_bottom_sheet
            )
            bottomSheet?.let { sheet ->
                sheet.background = ColorDrawable(Color.TRANSPARENT)
                BottomSheetBehavior.from(sheet).state = BottomSheetBehavior.STATE_EXPANDED
            }
            viewModel.onSaleOrderSearchQueryChanged(searchInput.text?.toString().orEmpty())
        }
        dialog.setOnDismissListener {
            saleOrderSearchDialog = null
            saleOrderOptionsContainer = null
            saleOrderSearchProgress = null
            saleOrderEmptyText = null
        }
        saleOrderSearchDialog = dialog
        saleOrderOptionsContainer = optionsContainer
        saleOrderSearchProgress = progressBar
        saleOrderEmptyText = emptyText
        dialog.show()
        renderSaleOrderSearchResults(viewModel.uiState.value)
    }

    private fun <T> showOptionSheet(
        title: String,
        options: List<T>,
        label: (T) -> String,
        selected: (T) -> Boolean = { false },
        onSelected: (T) -> Unit
    ) {
        val dialog = BottomSheetDialog(requireContext())
        val container = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.bg_filter_sheet)
            setPadding(
                resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._14sdp),
                resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._14sdp),
                resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._14sdp),
                resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._18sdp)
            )
        }
        val titleView = TextView(requireContext()).apply {
            text = title
            setTextColor(ContextCompat.getColor(requireContext(), R.color.ds_text_primary))
            typeface = resources.getFont(R.font.poppins_semibold)
            textSize = 18f
        }
        container.addView(titleView)
        val optionsContainer = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.bg_filter_dropdown_panel)
            setPadding(
                resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._8sdp),
                resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._8sdp),
                resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._8sdp),
                resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._8sdp)
            )
        }
        val optionsScrollView = ScrollView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._36sdp) *
                    options.size.coerceAtMost(MAX_VISIBLE_OPTIONS).coerceAtLeast(1)
            ).apply {
                topMargin = resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._8sdp)
            }
            isVerticalScrollBarEnabled = options.size > MAX_VISIBLE_OPTIONS
            overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS
            addView(optionsContainer)
        }
        options.forEach { option ->
            optionsContainer.addView(
                createCompactOptionRow(
                    labelText = label(option),
                    selected = selected(option)
                ) {
                    onSelected(option)
                    dialog.dismiss()
                }
            )
        }
        container.addView(optionsScrollView)
        dialog.setContentView(container)
        dialog.setOnShowListener {
            val bottomSheet = dialog.findViewById<FrameLayout>(
                com.google.android.material.R.id.design_bottom_sheet
            )
            bottomSheet?.let { sheet ->
                sheet.background = ColorDrawable(Color.TRANSPARENT)
                BottomSheetBehavior.from(sheet).state = BottomSheetBehavior.STATE_EXPANDED
            }
        }
        dialog.show()
    }

    private fun createCompactOptionRow(
        labelText: String,
        selected: Boolean,
        onClick: () -> Unit
    ): View {
        return LinearLayout(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._32sdp)
            ).apply {
                topMargin = resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._2sdp)
            }
            if (selected) setBackgroundResource(R.drawable.bg_filter_option_selected)
            isClickable = true
            setOnClickListener { onClick() }
            gravity = android.view.Gravity.CENTER_VERTICAL
            orientation = LinearLayout.HORIZONTAL
            setPadding(
                resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._6sdp),
                0,
                resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._8sdp),
                0
            )
            addView(
                TextView(requireContext()).apply {
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    text = labelText
                    setTextColor(ContextCompat.getColor(requireContext(), R.color.ds_text_primary))
                    setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(com.intuit.ssp.R.dimen._12ssp))
                    maxLines = 1
                    ellipsize = TextUtils.TruncateAt.END
                }
            )
            if (selected) {
                addView(
                    ImageView(requireContext()).apply {
                        layoutParams = LinearLayout.LayoutParams(
                            resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._18sdp),
                            resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._18sdp)
                        )
                        setImageResource(R.drawable.ia_ic_filter_check)
                    }
                )
            }
        }
    }

    private fun selectionTextColor(hasValue: Boolean): Int {
        return ContextCompat.getColor(
            requireContext(),
            if (hasValue) R.color.ds_text_primary else R.color.ds_text_muted
        )
    }

    override fun onDestroyView() {
        quoteSearchDialog?.dismiss()
        quoteSearchDialog = null
        quoteOptionsContainer = null
        quoteSearchProgress = null
        quoteEmptyText = null
        saleOrderSearchDialog?.dismiss()
        saleOrderSearchDialog = null
        saleOrderOptionsContainer = null
        saleOrderSearchProgress = null
        saleOrderEmptyText = null
        quantityWatchers.clear()
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val MAX_VISIBLE_OPTIONS = 5
        private const val ENABLED_CREATE_ALPHA = 1f
        private const val DISABLED_CREATE_ALPHA = 0.45f
    }
}



