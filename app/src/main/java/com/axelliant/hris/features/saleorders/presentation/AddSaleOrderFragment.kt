package com.axelliant.hris.features.saleorders.presentation

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
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.axelliant.hris.R
import com.axelliant.hris.ui.designsystem.components.AppProgressBarView
import com.axelliant.hris.ui.designsystem.components.AppTextView
import com.axelliant.hris.ui.designsystem.components.createAppBottomSheetDialog
import com.axelliant.hris.core.extensions.enableClearTextButton
import com.axelliant.hris.core.ui.UiState
import com.axelliant.hris.databinding.FragmentAddSaleOrderBinding
import com.axelliant.hris.databinding.ItemSelectedQuoteProductBinding
import com.axelliant.hris.features.quotes.domain.model.QuoteAddressUi
import com.axelliant.hris.features.quotes.domain.model.QuoteCreationProductUi
import com.axelliant.hris.features.quotes.domain.model.QuotePaymentTermUi
import com.axelliant.hris.features.quotes.domain.model.QuoteProductScheduleHelper
import com.axelliant.hris.features.quotes.presentation.AddQuoteAddressFragment
import com.axelliant.hris.features.quotes.presentation.AddQuoteViewModel
import com.axelliant.hris.features.quotes.presentation.AddressType
import com.axelliant.hris.features.quotes.presentation.QuoteProductActionsBottomSheet
import com.axelliant.hris.features.quotes.presentation.QuoteProductSelectionBundles.toQuoteCreationProducts
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.datepicker.MaterialDatePicker
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Locale
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AddSaleOrderFragment : Fragment() {

    private val viewModel: AddSaleOrderViewModel by viewModels()
    private var _binding: FragmentAddSaleOrderBinding? = null
    private val binding get() = _binding!!

    private var customerSearchDialog: BottomSheetDialog? = null
    private var customerOptionsContainer: LinearLayout? = null
    private var customerSearchProgress: AppProgressBarView? = null
    private var customerEmptyText: TextView? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddSaleOrderBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupInteractions()
        observeResults()
        observeState()
    }

    private fun setupInteractions() {
        binding.appTopBar.setOnBackClickListener { findNavController().navigateUp() }
        binding.customerField.setOnClickListener { showCustomerSearchSheet() }
        binding.billingAddressField.setOnClickListener {
            if (!isCustomerReady()) return@setOnClickListener
            viewModel.uiState.value.selectedCustomer?.billingAddresses?.let { addresses ->
                showAddressSheet(AddressType.Billing, addresses)
            }
        }
        binding.shippingAddressField.setOnClickListener {
            if (!isCustomerReady()) return@setOnClickListener
            viewModel.uiState.value.selectedCustomer?.shippingAddresses?.let { addresses ->
                showAddressSheet(AddressType.Shipping, addresses)
            }
        }
        binding.paymentTermField.setOnClickListener {
            if (!isCustomerReady()) return@setOnClickListener
            val state = viewModel.uiState.value
            if (state.isPaymentTermsLoading || state.paymentTerms.isEmpty()) return@setOnClickListener
            showOptionSheet(
                title = getString(R.string.add_quote_payment_term_required),
                options = state.paymentTerms,
                label = QuotePaymentTermUi::name,
                selected = { it.id == state.selectedPaymentTerm?.id },
                onSelected = viewModel::selectPaymentTerm
            )
        }
        binding.deliveryDateField.setOnClickListener {
            if (!isCustomerReady()) return@setOnClickListener
            showDeliveryDatePicker()
        }
        binding.shippingMethodField.setOnClickListener {
            if (!isCustomerReady()) return@setOnClickListener
            val state = viewModel.uiState.value
            if (state.shippingMethods.isEmpty()) return@setOnClickListener
            showOptionSheet(
                title = getString(R.string.add_sale_order_shipping_method),
                options = state.shippingMethods,
                label = { getString(it.nameResId) },
                selected = { it.id == state.selectedShippingMethod?.id },
                onSelected = viewModel::selectShippingMethod
            )
        }
        binding.addBillingAddressButton.setOnClickListener {
            if (!isCustomerReady()) return@setOnClickListener
            openAddressScreen(AddressType.Billing)
        }
        binding.addShippingAddressButton.setOnClickListener {
            if (!isCustomerReady()) return@setOnClickListener
            openAddressScreen(AddressType.Shipping)
        }
        binding.addProductButton.setOnClickListener {
            findNavController().navigate(R.id.iaAddQuoteProductFragment)
        }
        binding.changeDateButton.setOnClickListener {
            if (!isCustomerReady()) return@setOnClickListener
            showDeliveryDatePicker()
        }
        binding.saveDraftButton.setOnClickListener { viewModel.saveDraft() }
    }

    private fun observeResults() {
        observeAddressResult(AddressType.Billing)
        observeAddressResult(AddressType.Shipping)
        findNavController().currentBackStackEntry?.savedStateHandle
            ?.getLiveData<Bundle>(AddQuoteViewModel.RESULT_PRODUCTS)
            ?.observe(viewLifecycleOwner) { bundle ->
                findNavController().currentBackStackEntry?.savedStateHandle
                    ?.remove<Bundle>(AddQuoteViewModel.RESULT_PRODUCTS)
                viewModel.setProducts(bundle.toQuoteCreationProducts())
            }
    }

    private fun observeAddressResult(type: AddressType) {
        findNavController().currentBackStackEntry?.savedStateHandle
            ?.getLiveData<Bundle>(type.resultKey)
            ?.observe(viewLifecycleOwner) { bundle ->
                findNavController().currentBackStackEntry?.savedStateHandle
                    ?.remove<Bundle>(type.resultKey)
                viewModel.addAddress(type, bundle.toAddress())
            }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect(::renderState)
                }
                launch {
                    viewModel.saveState.collect { state ->
                        when (state) {
                            UiState.Loading -> {
                                binding.saveDraftButton.isEnabled = false
                                binding.loadingOverlay.isVisible = true
                            }
                            is UiState.Success -> {
                                binding.loadingOverlay.isVisible = false
                                Toast.makeText(
                                    requireContext(),
                                    state.data,
                                    Toast.LENGTH_SHORT
                                ).show()
                                findNavController().previousBackStackEntry?.savedStateHandle
                                    ?.set(AddSaleOrderViewModel.RESULT_SALE_ORDER_CREATED, true)
                                viewModel.onSaveHandled()
                                findNavController().popBackStack()
                            }
                            is UiState.Error -> {
                                binding.saveDraftButton.isEnabled = true
                                binding.loadingOverlay.isVisible = false
                                Toast.makeText(
                                    requireContext(),
                                    state.message,
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                            UiState.Unauthorized -> {
                                binding.saveDraftButton.isEnabled = true
                                binding.loadingOverlay.isVisible = false
                                Toast.makeText(
                                    requireContext(),
                                    R.string.quotes_error,
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            else -> {
                                binding.saveDraftButton.isEnabled =
                                    !viewModel.uiState.value.isFormLoading &&
                                    viewModel.uiState.value.canSave
                                binding.loadingOverlay.isVisible =
                                    viewModel.uiState.value.isFormLoading
                            }
                        }
                    }
                }
            }
        }
    }

    private fun renderState(state: AddSaleOrderUiState) {
        binding.appTopBar.setTitle(
            if (state.isEditMode) R.string.edit_sale_order_title else R.string.add_sale_order_title
        )
        binding.saveDraftButton.setText(
            if (state.isEditMode) R.string.edit_sale_order_save else R.string.add_sale_order_save_draft
        )
        binding.changeDateButton.isVisible = state.isEditMode

        val customer = state.selectedCustomer
        binding.customerNameText.text = customer?.name ?: getString(R.string.add_quote_select_customer)
        binding.customerNameText.setTextColor(selectionTextColor(customer != null))
        binding.customerEmailText.text = customer?.email.orEmpty()
        binding.priceProfileText.text = customer?.priceProfile.orEmpty()
        binding.creditLimitText.text = customer?.remainingCredit.orEmpty()

        binding.billingAddressText.text = state.selectedBillingAddress?.displayText
            ?: getString(R.string.add_quote_select_address)
        binding.billingAddressText.setTextColor(selectionTextColor(state.selectedBillingAddress != null))
        binding.shippingAddressText.text = state.selectedShippingAddress?.displayText
            ?: getString(R.string.add_quote_select_address)
        binding.shippingAddressText.setTextColor(selectionTextColor(state.selectedShippingAddress != null))
        binding.paymentTermText.text = state.selectedPaymentTerm?.name
            ?: getString(R.string.add_quote_select_payment_term)
        binding.paymentTermText.setTextColor(selectionTextColor(state.selectedPaymentTerm != null))
        binding.deliveryDateText.text = state.deliveryDate.ifBlank {
            getString(R.string.add_sale_order_delivery_date_hint)
        }
        binding.deliveryDateText.setTextColor(selectionTextColor(state.deliveryDate.isNotBlank()))
        binding.shippingMethodText.text = state.selectedShippingMethod?.let { getString(it.nameResId) }
            ?: getString(R.string.add_sale_order_select_shipping_method)
        binding.shippingMethodText.setTextColor(selectionTextColor(state.selectedShippingMethod != null))

        val isSaving = viewModel.saveState.value is UiState.Loading
        binding.loadingOverlay.isVisible = state.isFormLoading || isSaving
        binding.saveDraftButton.isEnabled =
            !state.isFormLoading && !isSaving && !state.isEditLoadFailed && state.canSave

        renderCustomerDependentFields(state, isSaving)
        renderCustomerSearchResults(state)
        state.errorMessage?.let { message ->
            viewModel.clearError()
            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
            if (state.isEditLoadFailed) {
                findNavController().navigateUp()
            }
        }

        renderProducts(state)
        renderFieldErrors(state)
        binding.estimatedSubtotalValue.text = state.formatCurrency(state.subtotal)
        if (state.scrollToFirstValidationError) {
            scrollToFirstValidationError(state.fieldErrors)
            viewModel.onValidationScrollHandled()
        }
    }

    private fun isCustomerReady(): Boolean {
        val state = viewModel.uiState.value
        return state.selectedCustomer != null && !state.isFormLoading
    }

    private fun renderCustomerDependentFields(state: AddSaleOrderUiState, isSaving: Boolean) {
        val customerReady = state.selectedCustomer != null &&
            !state.isFormLoading &&
            !isSaving
        binding.addBillingAddressButton.isEnabled = customerReady
        binding.addShippingAddressButton.isEnabled = customerReady
    }

    private fun renderFieldErrors(state: AddSaleOrderUiState) {
        val errors = state.fieldErrors
        val isSaving = viewModel.saveState.value is UiState.Loading
        val customerReady = state.selectedCustomer != null &&
            !state.isFormLoading &&
            !isSaving
        val paymentTermReady = customerReady &&
            !state.isPaymentTermsLoading &&
            state.paymentTerms.isNotEmpty()
        val customerFieldEnabled = !state.isFormLoading && !isSaving

        setSelectorFieldError(
            field = binding.customerField,
            errorView = binding.customerFieldError,
            hasError = errors.customer,
            messageRes = R.string.add_quote_validation_customer,
            isFieldEnabled = customerFieldEnabled
        )
        setSelectorFieldError(
            field = binding.billingAddressField,
            errorView = binding.billingAddressFieldError,
            hasError = errors.billingAddress,
            messageRes = R.string.add_quote_validation_billing_address,
            isFieldEnabled = customerReady
        )
        setSelectorFieldError(
            field = binding.shippingAddressField,
            errorView = binding.shippingAddressFieldError,
            hasError = errors.shippingAddress,
            messageRes = R.string.add_quote_validation_shipping_address,
            isFieldEnabled = customerReady
        )
        setSelectorFieldError(
            field = binding.paymentTermField,
            errorView = binding.paymentTermFieldError,
            hasError = errors.paymentTerm,
            messageRes = R.string.add_quote_validation_payment_term,
            isFieldEnabled = paymentTermReady
        )
        setSelectorFieldError(
            field = binding.deliveryDateField,
            errorView = binding.deliveryDateFieldError,
            hasError = errors.deliveryDate,
            messageRes = R.string.add_quote_validation_delivery_date,
            isFieldEnabled = customerReady
        )
        setSelectorFieldError(
            field = binding.shippingMethodField,
            errorView = binding.shippingMethodFieldError,
            hasError = errors.shippingMethod,
            messageRes = R.string.add_sale_order_validation_shipping_method,
            isFieldEnabled = customerReady
        )

        binding.productsFieldError.isVisible = errors.products
        if (errors.products) {
            binding.productsFieldError.text = getString(R.string.add_quote_validation_products)
        }
    }

    private fun setSelectorFieldError(
        field: View,
        errorView: AppTextView,
        hasError: Boolean,
        messageRes: Int,
        isFieldEnabled: Boolean = true
    ) {
        errorView.isVisible = hasError
        field.isEnabled = isFieldEnabled
        field.isClickable = isFieldEnabled
        if (hasError) {
            errorView.text = getString(messageRes)
            field.setBackgroundResource(R.drawable.bg_quote_field_error)
            field.alpha = 1f
        } else if (!isFieldEnabled) {
            field.setBackgroundResource(R.drawable.bg_quote_field_disabled)
            field.alpha = 0.55f
        } else {
            field.setBackgroundResource(R.drawable.bg_quote_search)
            field.alpha = 1f
        }
    }

    private fun scrollToFirstValidationError(errors: AddSaleOrderFieldErrors) {
        val target = when {
            errors.customer -> binding.customerField
            errors.billingAddress -> binding.billingAddressField
            errors.shippingAddress -> binding.shippingAddressField
            errors.paymentTerm -> binding.paymentTermField
            errors.deliveryDate -> binding.deliveryDateField
            errors.shippingMethod -> binding.shippingMethodField
            errors.products -> binding.productsFieldError
            else -> null
        } ?: return

        binding.contentScroll.post {
            binding.contentScroll.smoothScrollTo(0, target.top)
        }
    }

    private fun renderCustomerSearchResults(state: AddSaleOrderUiState) {
        customerSearchProgress?.isVisible = state.isCustomerSearchLoading
        customerEmptyText?.isVisible = !state.isCustomerSearchLoading && state.customers.isEmpty()
        val container = customerOptionsContainer ?: return
        container.removeAllViews()
        val selectedCustomerId = state.selectedCustomer?.id
        state.customers.forEach { customer ->
            container.addView(
                createCompactOptionRow(
                    labelText = customer.name,
                    selected = customer.id == selectedCustomerId
                ) {
                    viewModel.selectCustomer(customer)
                    customerSearchDialog?.dismiss()
                }
            )
        }
    }

    private fun showCustomerSearchSheet() {
        val dialog = requireContext().createAppBottomSheetDialog()
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
            text = getString(R.string.add_quote_customer_required)
            setTextColor(ContextCompat.getColor(requireContext(), R.color.ds_text_primary))
            typeface = ResourcesCompat.getFont(requireContext(), R.font.poppins_semibold)
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
            hint = getString(R.string.add_quote_search_customers_hint)
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
        val progressBar = AppProgressBarView(requireContext()).apply {
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
            text = getString(R.string.add_quote_no_customers_found)
            setTextColor(ContextCompat.getColor(requireContext(), R.color.ds_text_muted))
            typeface = ResourcesCompat.getFont(requireContext(), R.font.poppins_regular)
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
                resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._36sdp) * MAX_VISIBLE_QUOTE_OPTIONS
            ).apply {
                topMargin = resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._6sdp)
            }
            addView(optionsContainer)
        }

        container.addView(titleView)
        container.addView(searchInput)
        container.addView(progressBar)
        container.addView(emptyText)
        container.addView(optionsScrollView)

        customerSearchDialog = dialog
        customerOptionsContainer = optionsContainer
        customerSearchProgress = progressBar
        customerEmptyText = emptyText

        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.onCustomerSearchQueryChanged(s?.toString().orEmpty())
            }
            override fun afterTextChanged(s: Editable?) = Unit
        })

        dialog.setOnDismissListener {
            customerSearchDialog = null
            customerOptionsContainer = null
            customerSearchProgress = null
            customerEmptyText = null
        }
        dialog.setContentView(container)
        dialog.setOnShowListener {
            val bottomSheet = dialog.findViewById<FrameLayout>(
                com.google.android.material.R.id.design_bottom_sheet
            )
            bottomSheet?.let { sheet ->
                sheet.background = ColorDrawable(Color.TRANSPARENT)
                BottomSheetBehavior.from(sheet).state = BottomSheetBehavior.STATE_EXPANDED
            }
            searchInput.requestFocus()
            viewModel.onCustomerSearchQueryChanged("")
            renderCustomerSearchResults(viewModel.uiState.value)
        }
        dialog.show()
    }

    private fun renderProducts(state: AddSaleOrderUiState) {
        binding.selectedProductsContainer.removeAllViews()
        binding.emptyProductsState.isVisible = state.selectedProducts.isEmpty()
        state.selectedProducts.forEachIndexed { index, product ->
            val itemBinding = ItemSelectedQuoteProductBinding.inflate(
                layoutInflater,
                binding.selectedProductsContainer,
                false
            )
            itemBinding.rowNumberText.text = getString(
                R.string.add_quote_product_row_format,
                index + 1
            )
            itemBinding.productNameText.text = product.name
            val categoryLabel = product.category.ifBlank {
                getString(R.string.add_quote_product_category_fallback)
            }
            itemBinding.productMetaText.text = getString(
                R.string.quote_preview_sku_format,
                product.sku
            ) + " • " + categoryLabel
            itemBinding.unitPriceText.text = state.formatCurrency(product.unitPrice)
            itemBinding.quantityText.text = product.quantity.toString()
            itemBinding.lineTotalText.text = state.formatCurrency(product.lineTotal)
            itemBinding.thumbnailLabel.text = product.thumbnailLabel
            itemBinding.thumbnailFrame.setBackgroundResource(
                if (product.brandThumbnail) R.drawable.bg_product_thumb_cisco else R.drawable.bg_product_thumb_placeholder
            )
            itemBinding.thumbnailLabel.setTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    if (product.brandThumbnail) R.color.ds_on_primary else R.color.ds_text_muted
                )
            )
            val scheduleCount = product.deliverySchedules.size
            if (scheduleCount > 0) {
                itemBinding.deliveryScheduleSummaryText.text = getString(
                    R.string.add_quote_schedules_count_format,
                    scheduleCount
                )
                itemBinding.deliveryScheduleSummaryText.isVisible = true
            } else {
                itemBinding.deliveryScheduleSummaryText.text =
                    getString(R.string.add_quote_delivery_schedule_pending)
                itemBinding.deliveryScheduleSummaryText.isVisible = true
            }
            if (product.isOverScheduled) {
                itemBinding.deliveryScheduleSubtext.isVisible = true
                itemBinding.deliveryScheduleSubtext.text = getString(
                    R.string.add_quote_over_scheduled_format,
                    product.overScheduledUnits
                )
            } else {
                itemBinding.deliveryScheduleSubtext.isVisible = false
            }
            val openProductActions = View.OnClickListener {
                showProductActionsSheet(product, state)
            }
            itemBinding.root.setOnClickListener(openProductActions)
            itemBinding.root.isClickable = true
            itemBinding.productActionButton.setOnClickListener(openProductActions)
            itemBinding.productActionButton.isVisible = true
            binding.selectedProductsContainer.addView(itemBinding.root)
        }
    }

    private fun showProductActionsSheet(product: QuoteCreationProductUi, state: AddSaleOrderUiState) {
        val shippingAddresses = state.selectedCustomer?.shippingAddresses.orEmpty()
        val defaultShipping = state.selectedShippingAddress ?: shippingAddresses.firstOrNull()
        val productToEdit = QuoteProductScheduleHelper.ensureSchedules(
            product = product,
            quoteShippingAddress = defaultShipping,
            quoteDeliveryDate = state.deliveryDate
        )

        QuoteProductActionsBottomSheet(
            fragment = this,
            initialProduct = productToEdit,
            shippingAddresses = shippingAddresses,
            defaultShippingAddress = defaultShipping,
            quoteDeliveryDate = state.deliveryDate,
            formatCurrency = state::formatCurrency,
            onApply = viewModel::updateProduct,
            onDelete = { viewModel.removeProduct(product.id) }
        ).show()
    }

    private fun showAddressSheet(type: AddressType, addresses: List<QuoteAddressUi>) {
        val selectedAddressId = when (type) {
            AddressType.Billing -> viewModel.uiState.value.selectedBillingAddress?.id
            AddressType.Shipping -> viewModel.uiState.value.selectedShippingAddress?.id
        }
        showOptionSheet(
            title = when (type) {
                AddressType.Billing -> getString(R.string.add_quote_billing_address_required)
                AddressType.Shipping -> getString(R.string.add_quote_shipping_address_required)
            },
            options = addresses,
            label = QuoteAddressUi::displayText,
            selected = { it.id == selectedAddressId },
            onSelected = {
                when (type) {
                    AddressType.Billing -> viewModel.selectBillingAddress(it)
                    AddressType.Shipping -> viewModel.selectShippingAddress(it)
                }
            }
        )
    }

    private fun <T> showOptionSheet(
        title: String,
        options: List<T>,
        label: (T) -> String,
        selected: (T) -> Boolean = { false },
        onSelected: (T) -> Unit
    ) {
        val dialog = requireContext().createAppBottomSheetDialog()
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
            typeface = ResourcesCompat.getFont(requireContext(), R.font.poppins_semibold)
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
                    options.size.coerceAtMost(MAX_VISIBLE_QUOTE_OPTIONS).coerceAtLeast(1)
            ).apply {
                topMargin = resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._8sdp)
            }
            isVerticalScrollBarEnabled = options.size > MAX_VISIBLE_QUOTE_OPTIONS
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

    private fun showDeliveryDatePicker() {
        MaterialDatePicker.Builder.datePicker()
            .setTitleText(getString(R.string.add_quote_delivery_date_required))
            .build()
            .apply {
                addOnPositiveButtonClickListener { utcMillis ->
                    val formatted = SimpleDateFormat("MM-dd-yyyy", Locale.US).format(utcMillis)
                    viewModel.setDeliveryDate(formatted)
                }
            }
            .show(parentFragmentManager, DELIVERY_DATE_PICKER_TAG)
    }

    private fun openAddressScreen(type: AddressType) {
        if (!isCustomerReady()) return
        findNavController().navigate(
            R.id.iaAddQuoteAddressFragment,
            bundleOf(AddQuoteViewModel.ARG_ADDRESS_TYPE to type.navValue)
        )
    }

    private fun Bundle.toAddress(): QuoteAddressUi {
        return QuoteAddressUi(
            id = getString(AddQuoteAddressFragment.KEY_ID).orEmpty(),
            address = getString(AddQuoteAddressFragment.KEY_ADDRESS).orEmpty(),
            country = getString(AddQuoteAddressFragment.KEY_COUNTRY).orEmpty(),
            state = getString(AddQuoteAddressFragment.KEY_STATE).orEmpty(),
            city = getString(AddQuoteAddressFragment.KEY_CITY).orEmpty(),
            zipCode = getString(AddQuoteAddressFragment.KEY_ZIP).orEmpty()
        )
    }

    private fun selectionTextColor(hasValue: Boolean): Int {
        return ContextCompat.getColor(
            requireContext(),
            if (hasValue) R.color.ds_text_primary else R.color.ds_text_muted
        )
    }

    override fun onDestroyView() {
        customerSearchDialog?.dismiss()
        customerSearchDialog = null
        customerOptionsContainer = null
        customerSearchProgress = null
        customerEmptyText = null
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val DELIVERY_DATE_PICKER_TAG = "sale_order_delivery_date_picker"
        private const val MAX_VISIBLE_QUOTE_OPTIONS = 5
    }
}



