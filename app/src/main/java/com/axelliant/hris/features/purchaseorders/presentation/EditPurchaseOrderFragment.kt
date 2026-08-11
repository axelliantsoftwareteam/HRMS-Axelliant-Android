package com.axelliant.hris.features.purchaseorders.presentation

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
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
import com.axelliant.hris.ui.designsystem.components.AppTextView
import com.axelliant.hris.ui.designsystem.components.createAppBottomSheetDialog
import com.axelliant.hris.core.ui.UiState
import com.axelliant.hris.databinding.FragmentEditPurchaseOrderBinding
import com.axelliant.hris.databinding.ItemEditPoProductBinding
import com.axelliant.hris.features.purchaseorders.domain.model.EditPoProductLineUi
import com.axelliant.hris.features.purchaseorders.domain.model.PoAddressUi
import com.axelliant.hris.features.purchaseorders.domain.model.PoVendorUi
import com.axelliant.hris.features.quotes.presentation.AddQuoteAddressFragment
import com.axelliant.hris.features.quotes.presentation.AddQuoteViewModel
import com.axelliant.hris.features.quotes.presentation.AddressType
import com.axelliant.hris.features.quotes.presentation.QuoteProductSelectionBundles.toQuoteCreationProducts
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class EditPurchaseOrderFragment : Fragment() {

    private val viewModel: EditPurchaseOrderViewModel by viewModels()
    private var _binding: FragmentEditPurchaseOrderBinding? = null
    private val binding get() = _binding!!

    private val qtyWatchers = mutableMapOf<String, TextWatcher>()
    private val costWatchers = mutableMapOf<String, TextWatcher>()
    private var shippingWatcher: TextWatcher? = null
    private var suppressShippingCallback = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditPurchaseOrderBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupInteractions()
        observeAddressResults()
        observeProductResults()
        observeState()
    }

    private fun setupInteractions() {
        binding.appTopBar.setOnBackClickListener { findNavController().navigateUp() }
        binding.cancelButton.setOnClickListener { findNavController().navigateUp() }
        binding.vendorField.setOnClickListener {
            val vendors = viewModel.uiState.value.vendors
            if (vendors.isEmpty()) {
                Toast.makeText(requireContext(), R.string.edit_po_no_vendors, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            showOptionSheet(
                title = getString(R.string.edit_po_vendor),
                options = vendors,
                label = PoVendorUi::name,
                selected = { it.id == viewModel.uiState.value.selectedVendor?.id },
                onSelected = viewModel::selectVendor
            )
        }
        binding.billingAddressField.setOnClickListener {
            val addresses = viewModel.uiState.value.billingAddresses
            if (addresses.isEmpty()) {
                Toast.makeText(requireContext(), R.string.edit_po_no_addresses, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            showOptionSheet(
                title = getString(R.string.edit_po_billing_address),
                options = addresses,
                label = PoAddressUi::displayText,
                selected = { it.id == viewModel.uiState.value.selectedBillingAddress?.id },
                onSelected = viewModel::selectBillingAddress
            )
        }
        binding.shippingAddressField.setOnClickListener {
            val addresses = viewModel.uiState.value.shippingAddresses
            if (addresses.isEmpty()) {
                Toast.makeText(requireContext(), R.string.edit_po_no_addresses, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            showOptionSheet(
                title = getString(R.string.edit_po_shipping_address),
                options = addresses,
                label = PoAddressUi::displayText,
                selected = { it.id == viewModel.uiState.value.selectedShippingAddress?.id },
                onSelected = viewModel::selectShippingAddress
            )
        }
        binding.editBillingDetails.setOnClickListener { openAddressScreen(AddressType.Billing) }
        binding.editShippingDetails.setOnClickListener { openAddressScreen(AddressType.Shipping) }
        binding.addItemButton.setOnClickListener {
            findNavController().navigate(R.id.iaAddQuoteProductFragment)
        }
        binding.saveChangesButton.setOnClickListener { viewModel.saveChanges() }

        shippingWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(editable: Editable?) {
                if (suppressShippingCallback) return
                viewModel.updateShipping(parseCurrencyInput(editable?.toString().orEmpty()))
            }
        }
        binding.shippingEditText.addTextChangedListener(shippingWatcher)
    }

    private fun observeProductResults() {
        findNavController().currentBackStackEntry?.savedStateHandle
            ?.getLiveData<Bundle>(AddQuoteViewModel.RESULT_PRODUCTS)
            ?.observe(viewLifecycleOwner) { bundle ->
                findNavController().currentBackStackEntry?.savedStateHandle
                    ?.remove<Bundle>(AddQuoteViewModel.RESULT_PRODUCTS)
                viewModel.addProducts(bundle.toQuoteCreationProducts())
            }
    }

    private fun observeAddressResults() {
        val handle = findNavController().currentBackStackEntry?.savedStateHandle ?: return
        handle.getLiveData<Bundle>(AddressType.Billing.resultKey).observe(viewLifecycleOwner) { bundle ->
            handle.remove<Bundle>(AddressType.Billing.resultKey)
            viewModel.addAddress(isBilling = true, address = bundle.toPoAddress())
        }
        handle.getLiveData<Bundle>(AddressType.Shipping.resultKey).observe(viewLifecycleOwner) { bundle ->
            handle.remove<Bundle>(AddressType.Shipping.resultKey)
            viewModel.addAddress(isBilling = false, address = bundle.toPoAddress())
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
                        binding.loadingOverlay.isVisible = state is UiState.Loading ||
                            viewModel.uiState.value.isLoading
                        when (state) {
                            is UiState.Success -> {
                                Toast.makeText(requireContext(), state.data, Toast.LENGTH_SHORT).show()
                                viewModel.clearSaveState()
                                findNavController().previousBackStackEntry
                                    ?.savedStateHandle
                                    ?.set(PurchaseOrdersFragment.KEY_PURCHASE_ORDER_CREATED, true)
                                findNavController().navigateUp()
                            }
                            is UiState.Error -> {
                                Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                                viewModel.clearSaveState()
                            }
                            else -> Unit
                        }
                    }
                }
            }
        }
    }

    private fun renderState(state: EditPurchaseOrderUiState) {
        binding.loadingOverlay.isVisible = state.isLoading ||
            viewModel.saveState.value is UiState.Loading

        state.errorMessage?.let { message ->
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
            return
        }

        binding.poNumberText.text = state.poNumber
        val canSave = state.hasChanges && !state.isLoading &&
            viewModel.saveState.value !is UiState.Loading
        binding.saveChangesButton.isEnabled = canSave
        binding.saveChangesButton.alpha = if (canSave) ENABLED_BUTTON_ALPHA else DISABLED_BUTTON_ALPHA
        bindDropdown(
            textView = binding.vendorText,
            value = state.selectedVendor?.name,
            placeholder = getString(R.string.edit_po_select_vendor)
        )
        bindDropdown(
            textView = binding.billingAddressText,
            value = state.selectedBillingAddress?.displayText,
            placeholder = getString(R.string.edit_po_select_address)
        )
        bindDropdown(
            textView = binding.shippingAddressText,
            value = state.selectedShippingAddress?.displayText,
            placeholder = getString(R.string.edit_po_select_address)
        )

        binding.subtotalValue.text = viewModel.formatCurrency(state.subtotal)
        binding.taxLabel.text = getString(R.string.edit_po_tax_format, state.taxRate * 100.0)
        binding.taxValue.text = viewModel.formatCurrency(state.tax)
        binding.grandTotalValue.text = viewModel.formatCurrency(state.grandTotal)

        val shippingText = viewModel.formatCurrencyInput(state.shipping)
        if (binding.shippingEditText.text?.toString() != shippingText &&
            !binding.shippingEditText.hasFocus()
        ) {
            suppressShippingCallback = true
            binding.shippingEditText.setText(shippingText)
            suppressShippingCallback = false
        }

        renderProducts(state.products)
    }

    private fun renderProducts(products: List<EditPoProductLineUi>) {
        val existingIds = products.map { it.id }.toSet()
        qtyWatchers.keys.filterNot { it in existingIds }.forEach { qtyWatchers.remove(it) }
        costWatchers.keys.filterNot { it in existingIds }.forEach { costWatchers.remove(it) }
        binding.productsContainer.removeAllViews()

        products.forEach { product ->
            val itemBinding = ItemEditPoProductBinding.inflate(
                layoutInflater,
                binding.productsContainer,
                false
            )
            itemBinding.productNameText.text = product.name
            itemBinding.skuText.text = product.sku
            itemBinding.deleteProductButton.setOnClickListener {
                viewModel.removeProduct(product.id)
            }

            qtyWatchers.remove(product.id)?.let { itemBinding.qtyEditText.removeTextChangedListener(it) }
            if (itemBinding.qtyEditText.text?.toString() != product.quantity.toString()) {
                itemBinding.qtyEditText.setText(product.quantity.toString())
            }
            val qtyWatcher = object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
                override fun afterTextChanged(editable: Editable?) {
                    val qty = editable?.toString()?.toIntOrNull() ?: 0
                    viewModel.updateQuantity(product.id, qty)
                }
            }
            qtyWatchers[product.id] = qtyWatcher
            itemBinding.qtyEditText.addTextChangedListener(qtyWatcher)

            costWatchers.remove(product.id)?.let {
                itemBinding.unitCostEditText.removeTextChangedListener(it)
            }
            val costText = viewModel.formatCurrencyInput(product.unitCost)
            if (itemBinding.unitCostEditText.text?.toString() != costText &&
                !itemBinding.unitCostEditText.hasFocus()
            ) {
                itemBinding.unitCostEditText.setText(costText)
            }
            val costWatcher = object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
                override fun afterTextChanged(editable: Editable?) {
                    viewModel.updateUnitCost(product.id, parseCurrencyInput(editable?.toString().orEmpty()))
                }
            }
            costWatchers[product.id] = costWatcher
            itemBinding.unitCostEditText.addTextChangedListener(costWatcher)

            binding.productsContainer.addView(itemBinding.root)
        }
    }

    private fun bindDropdown(textView: AppTextView, value: String?, placeholder: String) {
        val hasValue = !value.isNullOrBlank()
        textView.text = if (hasValue) value else placeholder
        textView.setTextColor(
            ContextCompat.getColor(
                requireContext(),
                if (hasValue) R.color.ds_text_primary else R.color.ds_text_muted
            )
        )
    }

    private fun openAddressScreen(type: AddressType) {
        findNavController().navigate(
            R.id.iaAddQuoteAddressFragment,
            bundleOf(AddQuoteViewModel.ARG_ADDRESS_TYPE to type.navValue)
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
                    options.size.coerceAtMost(MAX_VISIBLE_OPTIONS).coerceAtLeast(1)
            ).apply {
                topMargin = resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._8sdp)
            }
            addView(optionsContainer)
        }
        options.forEach { option ->
            optionsContainer.addView(
                createOptionRow(label(option), selected(option)) {
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

    private fun createOptionRow(
        labelText: String,
        selected: Boolean,
        onClick: () -> Unit
    ): View {
        return LinearLayout(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._36sdp)
            )
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            if (selected) setBackgroundResource(R.drawable.bg_filter_option_selected)
            setPadding(
                resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._10sdp),
                0,
                resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._10sdp),
                0
            )
            addView(
                TextView(requireContext()).apply {
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    text = labelText
                    maxLines = 2
                    ellipsize = TextUtils.TruncateAt.END
                    setTextColor(ContextCompat.getColor(requireContext(), R.color.ds_text_primary))
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                }
            )
            setOnClickListener { onClick() }
        }
    }

    private fun parseCurrencyInput(raw: String): Double {
        return raw.replace(Regex("[^0-9.]"), "").toDoubleOrNull() ?: 0.0
    }

    private fun Bundle.toPoAddress(): PoAddressUi {
        return PoAddressUi(
            id = getString(AddQuoteAddressFragment.KEY_ID).orEmpty(),
            label = getString(AddQuoteAddressFragment.KEY_ADDRESS).orEmpty(),
            address = getString(AddQuoteAddressFragment.KEY_ADDRESS).orEmpty(),
            country = getString(AddQuoteAddressFragment.KEY_COUNTRY).orEmpty(),
            state = getString(AddQuoteAddressFragment.KEY_STATE).orEmpty(),
            city = getString(AddQuoteAddressFragment.KEY_CITY).orEmpty(),
            zipCode = getString(AddQuoteAddressFragment.KEY_ZIP).orEmpty()
        )
    }

    override fun onDestroyView() {
        shippingWatcher?.let { binding.shippingEditText.removeTextChangedListener(it) }
        shippingWatcher = null
        qtyWatchers.clear()
        costWatchers.clear()
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val MAX_VISIBLE_OPTIONS = 6
        private const val ENABLED_BUTTON_ALPHA = 1f
        private const val DISABLED_BUTTON_ALPHA = 0.45f
    }
}



