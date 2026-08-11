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
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.axelliant.hris.R
import com.axelliant.hris.ui.designsystem.components.createAppBottomSheetDialog
import com.axelliant.hris.core.ui.UiState
import com.axelliant.hris.databinding.FragmentCreateManualPurchaseOrderBinding
import com.axelliant.hris.databinding.ItemManualPoProductBinding
import com.axelliant.hris.features.purchaseorders.domain.model.ManualPoProductLineUi
import com.axelliant.hris.features.quotes.presentation.AddQuoteViewModel
import com.axelliant.hris.features.quotes.presentation.QuoteProductSelectionBundles.toQuoteCreationProducts
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class CreateManualPurchaseOrderFragment : Fragment() {

    private val viewModel: CreateManualPurchaseOrderViewModel by viewModels()
    private var _binding: FragmentCreateManualPurchaseOrderBinding? = null
    private val binding get() = _binding!!

    private val quantityWatchers = mutableMapOf<String, TextWatcher>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreateManualPurchaseOrderBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.appTopBar.setOnBackClickListener { findNavController().navigateUp() }
        binding.cancelButton.setOnClickListener { findNavController().navigateUp() }
        binding.addProductButton.setOnClickListener { openProductPicker() }
        binding.appTopBar.setOnSearchClickListener { openProductPicker() }
        binding.addMoreItemsRow.setOnClickListener { openProductPicker() }
        binding.createButton.setOnClickListener { viewModel.createPurchaseOrder() }
        observeProductResults()
        observeState()
    }

    private fun openProductPicker() {
        findNavController().navigate(R.id.iaAddQuoteProductFragment)
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

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect(::renderState)
                }
                launch {
                    viewModel.createState.collect { state ->
                        binding.loadingOverlay.isVisible = state is UiState.Loading
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

    private fun renderState(state: CreateManualPurchaseOrderUiState) {
        val hasProducts = state.hasProducts
        binding.emptyProductsState.isVisible = !hasProducts
        binding.productsContainer.isVisible = hasProducts
        binding.addMoreItemsRow.isVisible = hasProducts

        binding.createButton.isEnabled = viewModel.createState.value !is UiState.Loading
        binding.createButton.alpha = ENABLED_BUTTON_ALPHA

        binding.subtotalText.text = state.formatCurrency(state.subtotal)
        binding.shippingText.text = state.formatCurrency(state.shipping)
        binding.taxText.text = state.formatCurrency(state.tax)
        binding.grandTotalText.text = state.formatCurrency(state.grandTotal)

        if (state.showProductsValidation) {
            Toast.makeText(
                requireContext(),
                R.string.create_manual_po_validation_products,
                Toast.LENGTH_SHORT
            ).show()
            viewModel.consumeProductsValidation()
        }

        if (hasProducts) {
            renderProducts(state.products, state)
        } else {
            quantityWatchers.clear()
            binding.productsContainer.removeAllViews()
        }
    }

    private fun renderProducts(
        products: List<ManualPoProductLineUi>,
        state: CreateManualPurchaseOrderUiState
    ) {
        val existingIds = products.map { it.id }.toSet()
        quantityWatchers.keys.filterNot { it in existingIds }.forEach { quantityWatchers.remove(it) }
        binding.productsContainer.removeAllViews()
        products.forEach { product ->
            val itemBinding = ItemManualPoProductBinding.inflate(
                layoutInflater,
                binding.productsContainer,
                false
            )
            itemBinding.productNameText.text = product.name
            itemBinding.skuText.text = product.sku
            itemBinding.uomText.text = product.uom.ifBlank {
                getString(R.string.create_manual_po_uom_na)
            }
            itemBinding.vendorText.text = product.vendor
            itemBinding.unitCostText.text = state.formatCurrency(product.unitCost)
            itemBinding.totalPriceText.text = state.formatCurrency(product.lineTotal)
            itemBinding.thumbnailLabel.text = product.thumbnailLabel
            itemBinding.thumbnailFrame.setBackgroundResource(
                if (product.brandThumbnail) {
                    R.drawable.bg_product_thumb_cisco
                } else {
                    R.drawable.bg_product_thumb_placeholder
                }
            )
            itemBinding.thumbnailLabel.setTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    if (product.brandThumbnail) R.color.ds_on_primary else R.color.ds_text_muted
                )
            )
            itemBinding.deleteProductButton.setOnClickListener {
                viewModel.removeProduct(product.id)
            }
            itemBinding.vendorField.setOnClickListener {
                showOptionSheet(
                    title = getString(R.string.create_manual_po_vendor),
                    options = product.vendors,
                    label = { it },
                    selected = { it == product.vendor },
                    onSelected = { vendor -> viewModel.selectProductVendor(product.id, vendor) }
                )
            }

            val quantityEdit = itemBinding.quantityEditText
            quantityWatchers.remove(product.id)?.let { quantityEdit.removeTextChangedListener(it) }
            val quantityValue = product.quantity.toString()
            if (quantityEdit.text?.toString() != quantityValue) {
                quantityEdit.setText(quantityValue)
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

            val layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._8sdp)
            }
            binding.productsContainer.addView(itemBinding.root, layoutParams)
        }
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

    override fun onDestroyView() {
        quantityWatchers.clear()
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val MAX_VISIBLE_OPTIONS = 5
        private const val ENABLED_BUTTON_ALPHA = 1f
        private const val DISABLED_BUTTON_ALPHA = 0.45f
    }
}



