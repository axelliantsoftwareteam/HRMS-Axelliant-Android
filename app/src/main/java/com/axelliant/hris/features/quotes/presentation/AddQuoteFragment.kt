package com.axelliant.hris.features.quotes.presentation

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
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.axelliant.hris.R
import com.axelliant.hris.core.extensions.enableClearTextButton
import com.axelliant.hris.core.ui.UiState
import com.axelliant.hris.databinding.FragmentAddQuoteBinding
import com.axelliant.hris.databinding.ItemSelectedQuoteProductBinding
import com.axelliant.hris.databinding.LayoutQuotePreviewAmountRowBinding
import com.axelliant.hris.features.quotes.domain.model.QuoteAddressUi
import com.axelliant.hris.features.quotes.domain.model.QuoteCreationProductUi
import com.axelliant.hris.features.quotes.domain.model.QuotePaymentTermUi
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.datepicker.MaterialDatePicker
import com.axelliant.hris.features.quotes.domain.model.QuoteProductScheduleHelper
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Locale
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AddQuoteFragment : Fragment() {

    private val viewModel: AddQuoteViewModel by viewModels()
    private var _binding: FragmentAddQuoteBinding? = null
    private val binding get() = _binding!!

    private var customerSearchDialog: BottomSheetDialog? = null
    private var customerOptionsContainer: LinearLayout? = null
    private var customerSearchProgress: ProgressBar? = null
    private var customerEmptyText: TextView? = null
    private var isQuoteTitleWatcherActive = true

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddQuoteBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupInteractions()
        observeResults()
        observeState()
    }

    private fun setupInteractions() {
        binding.backButton.setOnClickListener { findNavController().navigateUp() }
        binding.quoteTitleInput.addTextChangedListener { editable ->
            if (isQuoteTitleWatcherActive) {
                viewModel.setQuoteTitle(editable?.toString().orEmpty())
            }
        }
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
        binding.dealRegistrationCheckbox.setOnCheckedChangeListener { _, checked ->
            viewModel.setDealRegistration(checked)
        }
        binding.createQuoteButton.setOnClickListener { viewModel.saveDraft() }
    }

    private fun observeResults() {
        observeAddressResult(AddressType.Billing)
        observeAddressResult(AddressType.Shipping)
        findNavController().currentBackStackEntry?.savedStateHandle
            ?.getLiveData<Bundle>(AddQuoteViewModel.RESULT_PRODUCTS)
            ?.observe(viewLifecycleOwner) { bundle ->
                findNavController().currentBackStackEntry?.savedStateHandle
                    ?.remove<Bundle>(AddQuoteViewModel.RESULT_PRODUCTS)
                viewModel.setProducts(bundle.toProducts())
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
                                binding.createQuoteButton.isEnabled = false
                                binding.loadingOverlay.isVisible = true
                            }
                            is UiState.Success -> {
                                binding.loadingOverlay.isVisible = false
                                Toast.makeText(
                                    requireContext(),
                                    state.data.message.ifBlank {
                                        getString(R.string.add_quote_created)
                                    },
                                    Toast.LENGTH_SHORT
                                ).show()
                                findNavController().previousBackStackEntry?.savedStateHandle
                                    ?.set(AddQuoteViewModel.RESULT_QUOTE_CREATED, true)
                                findNavController().popBackStack(R.id.iaQuotesFragment, false)
                            }
                            is UiState.Error -> {
                                binding.createQuoteButton.isEnabled = true
                                binding.loadingOverlay.isVisible = false
                                Toast.makeText(
                                    requireContext(),
                                    resolveValidationMessage(state.message),
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                            UiState.Unauthorized -> {
                                binding.createQuoteButton.isEnabled = true
                                binding.loadingOverlay.isVisible = false
                                Toast.makeText(
                                    requireContext(),
                                    R.string.quotes_error,
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            else -> {
                                val readOnly = isDuplicateReadOnly(viewModel.uiState.value)
                                binding.createQuoteButton.isEnabled =
                                    !readOnly && !viewModel.uiState.value.isFormLoading
                                binding.loadingOverlay.isVisible = viewModel.uiState.value.isFormLoading
                            }
                        }
                    }
                }
            }
        }
    }

    private fun renderState(state: AddQuoteUiState) {
        binding.screenTitle.setText(
            when {
                state.isDuplicateMode -> R.string.duplicate_quote_title
                state.isReviseMode -> R.string.revise_quote_title
                state.isEditMode -> R.string.edit_quote
                else -> R.string.add_quote_title
            }
        )

        if (binding.quoteTitleInput.text?.toString() != state.quoteTitle) {
            isQuoteTitleWatcherActive = false
            binding.quoteTitleInput.setText(state.quoteTitle)
            isQuoteTitleWatcherActive = true
        }

        val customer = state.selectedCustomer
        binding.customerNameText.text = customer?.name ?: getString(R.string.add_quote_select_customer)
        binding.customerNameText.setTextColor(selectionTextColor(customer != null))
        binding.customerEmailText.text = customer?.email.orEmpty()
        binding.creditLimitText.text = customer?.creditHoldLabel.orEmpty()
        binding.remainingCreditText.text = customer?.remainingCredit.orEmpty()
        binding.accountExecutiveText.text = customer?.accountExecutive.orEmpty()
        binding.priceProfileText.text = customer?.priceProfile.orEmpty()

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
            getString(R.string.add_quote_select_delivery_date)
        }
        binding.deliveryDateText.setTextColor(selectionTextColor(state.deliveryDate.isNotBlank()))
        binding.dealRegistrationCheckbox.isChecked = state.dealRegistration

        val isSaving = viewModel.saveState.value is UiState.Loading
        val readOnlyDuplicate = isDuplicateReadOnly(state)
        binding.loadingOverlay.isVisible = state.isFormLoading || isSaving
        binding.createQuoteButton.isEnabled =
            !readOnlyDuplicate && !state.isFormLoading && !isSaving && !state.isEditLoadFailed
        binding.createQuoteButton.isVisible = !readOnlyDuplicate
        binding.createQuoteButton.setText(
            when {
                state.isEditMode || state.isReviseMode -> R.string.save_quote
                else -> R.string.add_quote_create_quote
            }
        )

        applyDuplicateReadOnlyState(state, readOnlyDuplicate)

        renderCustomerDependentFields(state, isSaving, readOnlyDuplicate)
        renderCustomerSearchResults(state)
        state.errorMessage?.let { message ->
            viewModel.clearError()
            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
            if (state.isEditLoadFailed) {
                findNavController().navigateUp()
            }
        }

        renderProducts(state, readOnlyDuplicate)
        renderFieldErrors(state, readOnlyDuplicate)
        if (state.scrollToFirstValidationError) {
            scrollToFirstValidationError(state.fieldErrors)
            viewModel.onValidationScrollHandled()
        }
        setAmountRow(binding.subtotalRow, getString(R.string.quote_preview_subtotal), state.formatCurrency(state.subtotal))
        setAmountRow(binding.taxRow, getString(R.string.quote_preview_tax), state.formatCurrency(state.tax))
        setAmountRow(binding.shippingRow, getString(R.string.quote_preview_shipping), "Calculated at checkout")
        binding.grandTotalText.text = state.formatCurrency(state.grandTotal)
    }

    private fun isDuplicateReadOnly(state: AddQuoteUiState): Boolean {
        return state.isDuplicateMode && !state.isEditLoading && !state.isEditLoadFailed
    }

    private fun applyDuplicateReadOnlyState(state: AddQuoteUiState, readOnly: Boolean) {
        binding.quoteTitleInput.isEnabled = !readOnly
        binding.quoteTitleInput.isFocusable = !readOnly
        binding.quoteTitleInput.isFocusableInTouchMode = !readOnly
        binding.quoteTitleInputLayout.isEnabled = !readOnly
        binding.quoteTitleInputLayout.alpha = if (readOnly) 0.85f else 1f

        binding.dealRegistrationCheckbox.isEnabled = !readOnly
        binding.addProductButton.isEnabled = !readOnly
        binding.addProductButton.isVisible = !readOnly

        listOf(
            binding.customerField,
            binding.billingAddressField,
            binding.shippingAddressField,
            binding.paymentTermField,
            binding.deliveryDateField
        ).forEach { field ->
            if (readOnly) {
                field.isEnabled = false
                field.isClickable = false
                field.setBackgroundResource(R.drawable.bg_quote_field_disabled)
                field.alpha = 0.85f
            }
        }
    }

    private fun isCustomerReady(): Boolean {
        val state = viewModel.uiState.value
        if (isDuplicateReadOnly(state)) return false
        return state.selectedCustomer != null &&
            !state.isCustomerDetailsLoading &&
            !state.isFormLoading &&
            viewModel.saveState.value !is UiState.Loading
    }

    private fun renderCustomerDependentFields(
        state: AddQuoteUiState,
        isSaving: Boolean,
        readOnlyDuplicate: Boolean
    ) {
        val customerReady = state.selectedCustomer != null &&
            !state.isCustomerDetailsLoading &&
            !state.isFormLoading &&
            !isSaving &&
            !readOnlyDuplicate

        binding.addBillingAddressButton.isEnabled = customerReady
        binding.addShippingAddressButton.isEnabled = customerReady
        binding.addBillingAddressButton.isVisible = !readOnlyDuplicate
        binding.addShippingAddressButton.isVisible = !readOnlyDuplicate
    }

    private fun renderFieldErrors(state: AddQuoteUiState, readOnlyDuplicate: Boolean) {
        val errors = state.fieldErrors
        val isSaving = viewModel.saveState.value is UiState.Loading
        val customerReady = state.selectedCustomer != null &&
            !state.isCustomerDetailsLoading &&
            !state.isFormLoading &&
            !isSaving
        val paymentTermReady = customerReady &&
            !state.isPaymentTermsLoading &&
            state.paymentTerms.isNotEmpty()

        val customerFieldEnabled = !state.isFormLoading && !isSaving && !readOnlyDuplicate

        binding.quoteTitleInputLayout.isErrorEnabled = errors.quoteTitle
        binding.quoteTitleInputLayout.error = if (errors.quoteTitle) {
            getString(R.string.add_quote_validation_quote_title)
        } else {
            null
        }

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
            isFieldEnabled = customerReady && !readOnlyDuplicate
        )
        setSelectorFieldError(
            field = binding.shippingAddressField,
            errorView = binding.shippingAddressFieldError,
            hasError = errors.shippingAddress,
            messageRes = R.string.add_quote_validation_shipping_address,
            isFieldEnabled = customerReady && !readOnlyDuplicate
        )
        setSelectorFieldError(
            field = binding.paymentTermField,
            errorView = binding.paymentTermFieldError,
            hasError = errors.paymentTerm,
            messageRes = R.string.add_quote_validation_payment_term,
            isFieldEnabled = paymentTermReady && !readOnlyDuplicate
        )
        setSelectorFieldError(
            field = binding.deliveryDateField,
            errorView = binding.deliveryDateFieldError,
            hasError = errors.deliveryDate,
            messageRes = R.string.add_quote_validation_delivery_date,
            isFieldEnabled = customerReady && !readOnlyDuplicate
        )

        binding.productsFieldError.isVisible = errors.products
        if (errors.products) {
            binding.productsFieldError.text = getString(R.string.add_quote_validation_products)
        }
    }

    private fun setSelectorFieldError(
        field: View,
        errorView: TextView,
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

    private fun scrollToFirstValidationError(errors: AddQuoteFieldErrors) {
        val target = when {
            errors.quoteTitle -> binding.quoteTitleInputLayout
            errors.customer -> binding.customerField
            errors.billingAddress -> binding.billingAddressField
            errors.shippingAddress -> binding.shippingAddressField
            errors.paymentTerm -> binding.paymentTermField
            errors.deliveryDate -> binding.deliveryDateField
            errors.products -> binding.productsFieldError
            else -> null
        } ?: return

        binding.contentScroll.post {
            binding.contentScroll.smoothScrollTo(0, target.top)
        }
    }

    private fun renderCustomerSearchResults(state: AddQuoteUiState) {
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
            text = getString(R.string.add_quote_customer_required)
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
            text = getString(R.string.add_quote_no_customers_found)
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

    private fun renderProducts(state: AddQuoteUiState, readOnlyDuplicate: Boolean) {
        binding.selectedProductsContainer.removeAllViews()
        binding.noProductsText.isVisible = state.selectedProducts.isEmpty()
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
            val openProductActions = if (readOnlyDuplicate) {
                null
            } else {
                View.OnClickListener {
                    showProductActionsSheet(product, state)
                }
            }
            itemBinding.root.setOnClickListener(openProductActions)
            itemBinding.root.isClickable = openProductActions != null
            itemBinding.productActionButton.setOnClickListener(openProductActions)
            itemBinding.productActionButton.isVisible = !readOnlyDuplicate
            itemBinding.root.alpha = if (readOnlyDuplicate) 0.9f else 1f
            binding.selectedProductsContainer.addView(itemBinding.root)
        }
    }

    private fun showProductActionsSheet(product: QuoteCreationProductUi, state: AddQuoteUiState) {
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

    private fun setAmountRow(
        row: LayoutQuotePreviewAmountRowBinding,
        label: String,
        value: String
    ) {
        row.amountLabel.text = label
        row.amountValue.text = value
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

    private fun resolveValidationMessage(message: String): String {
        return when (message) {
            AddQuoteViewModel.VALIDATION_QUOTE_TITLE -> getString(R.string.add_quote_validation_quote_title)
            AddQuoteViewModel.VALIDATION_CUSTOMER -> getString(R.string.add_quote_validation_customer)
            AddQuoteViewModel.VALIDATION_PRICE_PROFILE -> getString(R.string.add_quote_validation_price_profile)
            AddQuoteViewModel.VALIDATION_BILLING_ADDRESS -> getString(R.string.add_quote_validation_billing_address)
            AddQuoteViewModel.VALIDATION_SHIPPING_ADDRESS -> getString(R.string.add_quote_validation_shipping_address)
            AddQuoteViewModel.VALIDATION_PAYMENT_TERM -> getString(R.string.add_quote_validation_payment_term)
            AddQuoteViewModel.VALIDATION_DELIVERY_DATE -> getString(R.string.add_quote_validation_delivery_date)
            AddQuoteViewModel.VALIDATION_PRODUCTS -> getString(R.string.add_quote_validation_products)
            else -> message
        }
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

    private fun Bundle.toProducts(): List<QuoteCreationProductUi> {
        val ids = getStringArrayList(AddQuoteProductFragment.KEY_PRODUCT_IDS).orEmpty()
        val names = getStringArrayList(AddQuoteProductFragment.KEY_PRODUCT_NAMES).orEmpty()
        val skus = getStringArrayList(AddQuoteProductFragment.KEY_PRODUCT_SKUS).orEmpty()
        val categories = getStringArrayList(AddQuoteProductFragment.KEY_PRODUCT_CATEGORIES).orEmpty()
        val thumbnails = getStringArrayList(AddQuoteProductFragment.KEY_PRODUCT_THUMBNAILS).orEmpty()
        val brandThumbnails = getBooleanArray(AddQuoteProductFragment.KEY_PRODUCT_BRAND_THUMBNAILS)
        val prices = getDoubleArray(AddQuoteProductFragment.KEY_PRODUCT_PRICES)
        return ids.mapIndexed { index, id ->
            QuoteCreationProductUi(
                id = id,
                name = names.getOrNull(index).orEmpty(),
                sku = skus.getOrNull(index).orEmpty(),
                category = categories.getOrNull(index).orEmpty(),
                thumbnailLabel = thumbnails.getOrNull(index).orEmpty(),
                brandThumbnail = brandThumbnails?.getOrNull(index) ?: false,
                unitPrice = prices?.getOrNull(index) ?: 0.0
            )
        }
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
        private const val DELIVERY_DATE_PICKER_TAG = "quote_delivery_date_picker"
        private const val MAX_VISIBLE_QUOTE_OPTIONS = 6
    }
}



