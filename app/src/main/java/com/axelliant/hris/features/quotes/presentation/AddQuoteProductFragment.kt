package com.axelliant.hris.features.quotes.presentation

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.axelliant.hris.R
import com.axelliant.hris.core.extensions.hideKeyboard
import com.axelliant.hris.core.ui.UiState
import com.axelliant.hris.databinding.FragmentAddQuoteProductBinding
import com.axelliant.hris.databinding.ItemQuoteProductSelectionBinding
import com.axelliant.hris.features.inventory.products.presentation.ProductListItemUi
import com.axelliant.hris.features.inventory.products.presentation.ProductStatusFilter
import com.axelliant.hris.features.inventory.products.presentation.ProductsViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.text.NumberFormat
import java.util.Locale
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AddQuoteProductFragment : Fragment() {

    private val viewModel: ProductsViewModel by viewModels()
    private var _binding: FragmentAddQuoteProductBinding? = null
    private val binding get() = _binding!!
    private val selectedProducts = linkedMapOf<String, ProductListItemUi>()
    private var currentProducts = emptyList<ProductListItemUi>()
    private var searchQuery = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddQuoteProductBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupInteractions()
        observeProducts()
        renderFooter()
        viewModel.loadProducts(statusFilter = ProductStatusFilter.Active)
    }

    private fun setupInteractions() {
        binding.backButton.setOnClickListener { findNavController().navigateUp() }
        binding.filterButton.setOnClickListener {
            Toast.makeText(requireContext(), R.string.filters, Toast.LENGTH_SHORT).show()
        }
        binding.confirmButton.setOnClickListener { confirmSelection() }
        binding.searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchQuery = s?.toString().orEmpty().trim()
            }
            override fun afterTextChanged(s: Editable?) = Unit
        })
        binding.searchEditText.setOnEditorActionListener { _, actionId, event ->
            val isSearch = actionId == EditorInfo.IME_ACTION_SEARCH ||
                actionId == EditorInfo.IME_ACTION_DONE
            val isEnter = event?.keyCode == KeyEvent.KEYCODE_ENTER &&
                event.action == KeyEvent.ACTION_UP
            if (isSearch || isEnter) {
                binding.searchEditText.hideKeyboard()
                viewModel.loadProducts(searchQuery, ProductStatusFilter.Active)
                true
            } else {
                false
            }
        }
    }

    private fun observeProducts() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.productsState.collect { state ->
                    binding.progress.isVisible = state is UiState.Loading
                    binding.errorText.isVisible = state is UiState.Error || state is UiState.Unauthorized
                    binding.emptyStateText.isVisible = false
                    when (state) {
                        is UiState.Success -> {
                            currentProducts = state.data.products
                            renderProducts()
                        }
                        UiState.Empty -> {
                            currentProducts = emptyList()
                            renderProducts()
                            binding.emptyStateText.isVisible = true
                        }
                        is UiState.Error -> binding.errorText.text = state.message
                        UiState.Unauthorized -> binding.errorText.text = getString(R.string.quotes_error)
                        else -> Unit
                    }
                }
            }
        }
    }

    private fun renderProducts() {
        binding.productsContainer.removeAllViews()
        currentProducts.forEach { product ->
            val itemBinding = ItemQuoteProductSelectionBinding.inflate(
                layoutInflater,
                binding.productsContainer,
                false
            )
            bindProduct(itemBinding, product)
            binding.productsContainer.addView(itemBinding.root)
        }
    }

    private fun bindProduct(itemBinding: ItemQuoteProductSelectionBinding, product: ProductListItemUi) {
        itemBinding.productCategoryText.text = product.category.ifBlank {
            getString(R.string.add_quote_product_category_fallback)
        }.uppercase(Locale.US)
        itemBinding.productNameText.text = product.name
        itemBinding.productSkuText.text = getString(R.string.quote_preview_sku_format, product.sku)
        itemBinding.productPriceText.text = product.displayPrice
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
        updateSelectionButton(itemBinding, product.id)
        itemBinding.root.setOnClickListener { toggleProduct(product) }
        itemBinding.selectButton.setOnClickListener { toggleProduct(product) }
    }

    private fun updateSelectionButton(itemBinding: ItemQuoteProductSelectionBinding, productId: String) {
        val selected = selectedProducts.containsKey(productId)
        itemBinding.selectButton.setIconResource(
            if (selected) R.drawable.ia_ic_filter_check else R.drawable.ia_ic_add
        )
        itemBinding.selectButton.alpha = if (selected) 0.86f else 1f
    }

    private fun toggleProduct(product: ProductListItemUi) {
        if (selectedProducts.containsKey(product.id)) {
            selectedProducts.remove(product.id)
        } else {
            selectedProducts[product.id] = product
        }
        renderProducts()
        renderFooter()
    }

    private fun renderFooter() {
        val total = selectedProducts.values.sumOf { it.displayPrice.toCurrencyDouble() }
        binding.selectedCountText.text = getString(
            R.string.add_quote_products_selected_format,
            selectedProducts.size
        )
        binding.estimatedTotalText.text = getString(
            R.string.add_quote_estimated_total_format,
            NumberFormat.getCurrencyInstance(Locale.US).format(total)
        )
        binding.confirmButton.isEnabled = selectedProducts.isNotEmpty()
    }

    private fun confirmSelection() {
        val products = selectedProducts.values.toList()
        findNavController().previousBackStackEntry?.savedStateHandle?.set(
            AddQuoteViewModel.RESULT_PRODUCTS,
            bundleOf(
                KEY_PRODUCT_IDS to ArrayList(products.map { it.id }),
                KEY_PRODUCT_NAMES to ArrayList(products.map { it.name }),
                KEY_PRODUCT_SKUS to ArrayList(products.map { it.sku }),
                KEY_PRODUCT_CATEGORIES to ArrayList(products.map { it.category }),
                KEY_PRODUCT_THUMBNAILS to ArrayList(products.map { it.thumbnailLabel }),
                KEY_PRODUCT_BRAND_THUMBNAILS to BooleanArray(products.size) { index ->
                    products[index].brandThumbnail
                },
                KEY_PRODUCT_PRICES to DoubleArray(products.size) { index ->
                    products[index].displayPrice.toCurrencyDouble()
                }
            )
        )
        findNavController().navigateUp()
    }

    private fun String.toCurrencyDouble(): Double {
        return replace("$", "")
            .replace(",", "")
            .trim()
            .toDoubleOrNull() ?: 0.0
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val KEY_PRODUCT_IDS = "productIds"
        const val KEY_PRODUCT_NAMES = "productNames"
        const val KEY_PRODUCT_SKUS = "productSkus"
        const val KEY_PRODUCT_CATEGORIES = "productCategories"
        const val KEY_PRODUCT_THUMBNAILS = "productThumbnails"
        const val KEY_PRODUCT_BRAND_THUMBNAILS = "productBrandThumbnails"
        const val KEY_PRODUCT_PRICES = "productPrices"
    }
}
