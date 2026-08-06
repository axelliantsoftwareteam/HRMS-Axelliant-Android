package com.axelliant.hris.features.inventory.products.presentation

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.TypedValue
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.res.use
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.axelliant.hris.R
import com.axelliant.hris.core.extensions.enableClearTextButton
import com.axelliant.hris.core.extensions.hideKeyboard
import com.axelliant.hris.core.extensions.showKeyboard
import com.axelliant.hris.core.contracts.session.WorkspaceSessionProvider
import com.axelliant.hris.core.ui.BottomNavigationHost
import com.axelliant.hris.core.ui.UiState
import com.axelliant.hris.databinding.FragmentProductsBinding
import com.axelliant.hris.databinding.ItemAppliedProductFilterChipBinding
import com.axelliant.hris.databinding.ItemProductListingBinding
import com.axelliant.hris.databinding.LayoutProductFilterSheetBinding
import com.axelliant.hris.features.inventory.products.ai.ProductAiStatus
import com.axelliant.hris.features.inventory.products.data.remote.dto.ProductSearchFilters
import com.axelliant.hris.features.inventory.products.presentation.ProductPickerResultBundles.toProductListItem
import com.axelliant.hris.features.inventory.products.presentation.dynamicfilters.DynamicCategorySpecsResult
import com.axelliant.hris.features.inventory.products.presentation.dynamicfilters.SelectedCategorySpecs
import com.axelliant.hris.features.quotes.presentation.QuoteProductSelectionBundles
import com.axelliant.hris.features.quotes.presentation.QuoteProductSelectionBundles.toPreselectedProductItems
import com.axelliant.hris.features.quotes.presentation.SmartQuoteProductFlow
import com.axelliant.hris.features.quotes.presentation.SmartQuoteViewModel
import com.axelliant.hris.features.quotes.presentation.toQuoteCreationProduct
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale
import javax.inject.Inject
import java.text.NumberFormat
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ProductsFragment : Fragment() {
    @Inject
    lateinit var workspaceSessionProvider: WorkspaceSessionProvider

    private val viewModel: ProductsViewModel by viewModels()
    private var _binding: FragmentProductsBinding? = null
    private val binding get() = _binding!!

    private var selectedStatus = ProductStatusFilter.All
    private var draftSearchQuery = ""
    private var products = emptyList<ProductListItemUi>()
    private var totalProducts = 0
    private var tabCounts = ProductTabCountsUiModel.empty()
    private var renderedProductIds = emptyList<String>()
    private var appliedProductFilters = ProductSearchFilters()
    private var appliedFilterSheetState = ProductFilterSheetState()
    private var pendingFilterSheetState: ProductFilterSheetState? = null
    private var activeFilterPopup: PopupWindow? = null
    private val shimmerAnimators = mutableListOf<ObjectAnimator>()
    private val appliedFiltersAdapter = AppliedProductFilterChipAdapter(::handleAppliedFilterAction)
    private val productsAdapter = ProductsAdapter(::bindProduct)
    private val selectedProducts = linkedMapOf<String, ProductListItemUi>()

    private val selectionMode: Boolean
        get() = arguments?.getBoolean(SmartQuoteProductFlow.ARG_SELECTION_MODE, false) == true

    private val pickerFlow: String
        get() = arguments?.getString(SmartQuoteProductFlow.ARG_PRODUCT_PICKER_FLOW)
            ?: SmartQuoteProductFlow.FLOW_DEFAULT

    private val isProductComparisonPicker: Boolean
        get() = pickerFlow == ProductComparisonFlow.FLOW_PRODUCT_COMPARISON

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProductsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupAppliedFilterChips()
        setupProductsList()
        setupInteractions()
        setupPagination()
        if (selectionMode) {
            setupSelectionMode()
            restorePreselectedProducts()
            arguments?.getBundle(SmartQuoteProductFlow.ARG_ASK_AI_RESULT)?.let(::applyAskAiSearchResult)
            arguments?.getString(SmartQuoteProductFlow.ARG_INITIAL_SEARCH_QUERY)?.trim()?.takeIf { it.isNotEmpty() }?.let { query ->
                draftSearchQuery = query
                binding.searchEditText.setText(query)
            }
        } else {
            observeAskAiSearchResult()
        }
        observeDynamicCategorySpecsResult()
        observeProducts()
        viewModel.loadProductsIfNeeded()
        renderAppliedFilterChips()
        renderProducts()
        if (selectionMode) {
            renderSelectionFooter()
        }
    }

    override fun onResume() {
        super.onResume()
        if (selectionMode) {
            setBottomNavigationVisible(false)
        }
    }

    private fun setupSelectionMode() {
        binding.selectionBottomPanel.isVisible = true
        binding.askAiButton.isVisible = false
        binding.compareButton.isVisible = false
        binding.searchInputLayout.isVisible = false
        binding.productsRecyclerView.setPadding(
            binding.productsRecyclerView.paddingLeft,
            binding.productsRecyclerView.paddingTop,
            binding.productsRecyclerView.paddingRight,
            resources.getDimensionPixelSize(R.dimen.ds_space_72)
        )
        binding.confirmSelectionButton.setOnClickListener { confirmSelection() }
        setBottomNavigationVisible(false)
        renderSelectionFooter()
    }

    private fun setBottomNavigationVisible(visible: Boolean) {
        (activity as? BottomNavigationHost)?.setBottomNavigationVisible(visible)
    }

    private fun restorePreselectedProducts() {
        selectedProducts.clear()
        if (isProductComparisonPicker) {
            arguments?.getBundle(ProductComparisonFlow.ARG_PRESELECTED_PRODUCT)
                ?.toProductListItem()
                ?.let { selectedProducts[it.id] = it }
            return
        }
        arguments?.getBundle(SmartQuoteProductFlow.ARG_PRESELECTED_PRODUCTS)
            ?.toPreselectedProductItems()
            ?.let { restored -> selectedProducts.putAll(restored) }
    }

    private fun confirmSelection() {
        if (isProductComparisonPicker) {
            confirmProductComparisonSelection()
        } else {
            confirmSmartQuoteSelection()
        }
    }

    private fun confirmSmartQuoteSelection() {
        val productsBundle = QuoteProductSelectionBundles.fromProducts(
            selectedProducts.values.map { it.toQuoteCreationProduct() }
        )
        val navController = findNavController()
        val smartQuoteEntry = runCatching {
            navController.getBackStackEntry(R.id.iaSmartQuoteFragment)
        }.getOrNull() ?: run {
            navController.navigateUp()
            return
        }
        smartQuoteEntry.savedStateHandle.apply {
            set(SmartQuoteViewModel.RESULT_PRODUCTS, productsBundle)
            set(SmartQuoteViewModel.RESULT_SEARCH_QUERY, draftSearchQuery)
        }
        navController.popBackStack(R.id.iaSmartQuoteFragment, false)
    }

    private fun confirmProductComparisonSelection() {
        val product = selectedProducts.values.firstOrNull() ?: return
        val navController = findNavController()
        val comparisonEntry = runCatching {
            navController.getBackStackEntry(R.id.iaProductComparisonFragment)
        }.getOrNull() ?: run {
            navController.navigateUp()
            return
        }
        comparisonEntry.savedStateHandle.apply {
            set(ProductComparisonFlow.RESULT_PRODUCT, ProductPickerResultBundles.fromProduct(product))
            set(
                ProductComparisonFlow.RESULT_SLOT,
                arguments?.getInt(
                    ProductComparisonFlow.ARG_COMPARE_SLOT,
                    ProductComparisonFlow.SLOT_ONE
                ) ?: ProductComparisonFlow.SLOT_ONE
            )
        }
        navController.popBackStack(R.id.iaProductComparisonFragment, false)
    }

    private fun toggleSelectedProduct(product: ProductListItemUi) {
        if (isProductComparisonPicker) {
            if (selectedProducts.containsKey(product.id)) {
                selectedProducts.clear()
            } else {
                selectedProducts.clear()
                selectedProducts[product.id] = product
            }
            renderProducts()
            renderSelectionFooter()
            return
        }
        if (selectedProducts.containsKey(product.id)) {
            selectedProducts.remove(product.id)
        } else {
            selectedProducts[product.id] = product
        }
        renderProducts()
        renderSelectionFooter()
    }

    private fun renderSelectionFooter() {
        if (!selectionMode) return
        val total = selectedProducts.values.sumOf { it.displayPrice.toCurrencyDouble() }
        if (isProductComparisonPicker) {
            binding.selectionCountText.text = getString(
                R.string.product_compare_picker_selected_format,
                selectedProducts.size
            )
            binding.selectionTotalText.text = selectedProducts.values.firstOrNull()?.name
                ?: getString(R.string.product_compare_picker_hint)
            binding.confirmSelectionButton.text = getString(R.string.product_compare_use_product)
        } else {
            binding.selectionCountText.text = getString(
                R.string.add_quote_products_selected_format,
                selectedProducts.size
            )
            binding.selectionTotalText.text = getString(
                R.string.add_quote_estimated_total_format,
                NumberFormat.getCurrencyInstance(Locale.US).format(total)
            )
            binding.confirmSelectionButton.text = getString(R.string.add_quote_confirm_selection)
        }
        binding.confirmSelectionButton.isEnabled = selectedProducts.isNotEmpty()
    }

    private fun String.toCurrencyDouble(): Double {
        return replace("$", "")
            .replace(",", "")
            .trim()
            .toDoubleOrNull() ?: 0.0
    }

    private fun setupInteractions() {
        binding.allTab.setOnClickListener { updateStatusFilter(ProductStatusFilter.All) }
        binding.activeTab.setOnClickListener { updateStatusFilter(ProductStatusFilter.Active) }
        binding.inactiveTab.setOnClickListener { updateStatusFilter(ProductStatusFilter.Inactive) }

        binding.filterIconButton.setOnClickListener { showFilterMenu() }
        binding.searchIconButton.setOnClickListener { toggleSearchField() }
        binding.compareButton.setOnClickListener { findNavController().navigate(R.id.iaProductComparisonFragment) }
        binding.askAiButton.setOnClickListener { findNavController().navigate(R.id.iaAskAiProductSearchFragment) }
        binding.signInAgainButton.setOnClickListener { logoutAndOpenLogin() }
        binding.retryText.setOnClickListener {
            clearRenderedProducts()
            viewModel.loadProducts(draftSearchQuery, selectedStatus, appliedProductFilters)
        }
        binding.searchInputLayout.isEndIconVisible = binding.searchEditText.text?.isNotEmpty() == true
        binding.searchInputLayout.setEndIconOnClickListener {
            binding.searchEditText.setText("")
        }
        binding.searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(text: CharSequence?, start: Int, count: Int, after: Int) = Unit

            override fun onTextChanged(text: CharSequence?, start: Int, before: Int, count: Int) {
                draftSearchQuery = text?.toString().orEmpty().trim()
            }

            override fun afterTextChanged(editable: Editable?) {
                binding.searchInputLayout.isEndIconVisible = editable?.isNotEmpty() == true
            }
        })

        binding.searchEditText.setOnEditorActionListener { _, actionId, event ->
            val isSubmitAction = actionId == EditorInfo.IME_ACTION_SEARCH ||
                actionId == EditorInfo.IME_ACTION_DONE
            val isEnterKey = event?.keyCode == KeyEvent.KEYCODE_ENTER &&
                event.action == KeyEvent.ACTION_UP

            if (isSubmitAction || isEnterKey) {
                submitSearch()
                true
            } else {
                false
            }
        }
    }

    private fun setupAppliedFilterChips() {
        binding.appliedFiltersRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL, false)
            adapter = appliedFiltersAdapter
            itemAnimator = null
        }
    }

    private fun setupProductsList() {
        binding.productsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = productsAdapter
            itemAnimator = null
        }
    }

    private fun updateStatusFilter(status: ProductStatusFilter) {
        if (selectedStatus == status) return

        selectedStatus = status
        clearRenderedProducts()
        binding.productsRecyclerView.scrollToPosition(0)
        renderStatusTabs()
        viewModel.loadProducts(draftSearchQuery, selectedStatus, appliedProductFilters)
    }

    private fun toggleSearchField() {
        if (binding.searchInputLayout.isVisible) {
            binding.searchEditText.setText("")
            binding.searchInputLayout.isVisible = false
            binding.searchEditText.clearFocus()
            binding.searchEditText.hideKeyboard()
            submitSearch()
            return
        }

        binding.searchInputLayout.isVisible = true
        binding.searchEditText.showKeyboard()
    }

    private fun logoutAndOpenLogin() {
        workspaceSessionProvider.clearAllSessions()
        findNavController().navigate(
            R.id.commonLoginFragment,
            null,
            NavOptions.Builder()
                .setPopUpTo(R.id.iaInternalAppsNavGraph, true)
                .build()
        )
    }

    private fun showFilterMenu(
        initialState: ProductFilterSheetState = (pendingFilterSheetState ?: appliedFilterSheetState).copyForEditing().apply {
            searchText = draftSearchQuery
        }
    ) {
        ProductFilterSheetController(
            fragment = this,
            lifecycleOwner = viewLifecycleOwner,
            viewModel = viewModel,
            initialState = initialState,
            onCategorySpecsClick = { currentSheetState ->
                pendingFilterSheetState = currentSheetState.copyForEditing()
                findNavController().navigate(
                    R.id.iaDynamicCategorySelectionFragment,
                    bundleOf(
                        DynamicCategorySpecsResult.ARG_SELECTED_SPECS_JSON to
                            DynamicCategorySpecsResult.toJson(currentSheetState.dynamicCategorySpecs)
                    )
                )
            },
            onCloseWithoutApply = {
                pendingFilterSheetState = null
            },
            onApply = { appliedState ->
                pendingFilterSheetState = null
                applyAppliedFilterState(appliedState)
            }
        ).show()
    }

    private fun bindFilterLookupObservers(
        sheetBinding: LayoutProductFilterSheetBinding,
        filterState: ProductFilterSheetState,
        manufacturerFieldText: TextView?,
        vendorFieldText: TextView?
    ): List<Job> {
        return listOf(
            viewLifecycleOwner.lifecycleScope.launch {
                viewModel.categoryFiltersState.collect { state ->
                    if (state is UiState.Success) filterState.categoryOptions = state.data
                    renderFilterOptions(
                        container = sheetBinding.categoryOptionsContainer,
                        optionsScrollView = sheetBinding.categoryOptionsScrollView,
                        state = state,
                        selectedOptions = filterState.categories,
                        fieldText = sheetBinding.categoryFieldText,
                        chipsContainer = sheetBinding.categoryChipsContainer,
                        placeholder = getString(R.string.select_category),
                        selectedCountText = sheetBinding.categorySelectedCountText
                    )
                }
            },
            viewLifecycleOwner.lifecycleScope.launch {
                viewModel.manufacturerFiltersState.collect { state ->
                    if (state is UiState.Success) filterState.manufacturerOptions = state.data
                    renderFilterOptions(
                        container = sheetBinding.manufacturerOptionsContainer,
                        optionsScrollView = sheetBinding.manufacturerOptionsScrollView,
                        state = state,
                        selectedOptions = filterState.manufacturers,
                        fieldText = manufacturerFieldText,
                        chipsContainer = sheetBinding.manufacturerChipsContainer,
                        placeholder = getString(R.string.select_manufacturer)
                    )
                }
            },
            viewLifecycleOwner.lifecycleScope.launch {
                viewModel.vendorFiltersState.collect { state ->
                    if (state is UiState.Success) filterState.vendorOptions = state.data
                    renderFilterOptions(
                        container = sheetBinding.vendorOptionsContainer,
                        optionsScrollView = sheetBinding.vendorOptionsScrollView,
                        state = state,
                        selectedOptions = filterState.vendors,
                        fieldText = vendorFieldText,
                        chipsContainer = sheetBinding.vendorChipsContainer,
                        placeholder = getString(R.string.select_vendor)
                    )
                }
            }
        )
    }

    private fun toggleFilterDropdown(
        field: View,
        panel: View,
        onOpen: () -> Unit
    ) {
        val showDropdown = !panel.isVisible
        panel.isVisible = showDropdown
        field.setBackgroundResource(
            if (showDropdown) R.drawable.bg_filter_field_focused else R.drawable.bg_filter_field
        )
        if (showDropdown) onOpen()
    }

    private fun showFilterDropdownPopup(
        anchor: View,
        lookupState: UiState<List<FilterOptionUi>>,
        selectedOptions: LinkedHashMap<String, String>,
        fieldText: TextView?,
        chipsContainer: RecyclerView,
        placeholder: String,
        searchHint: String,
        onLoad: () -> Unit,
        selectedCountText: TextView? = null
    ) {
        activeFilterPopup?.dismiss()
        val stateForPopup = lookupState.takeUnless { it is UiState.Idle } ?: UiState.Loading
        if (lookupState !is UiState.Success) onLoad()

        val dropdownContainer = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.bg_filter_dropdown_panel)
            setPadding(
                resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._8sdp),
                resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._8sdp),
                resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._8sdp),
                resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._8sdp)
            )
        }
        val searchInput = EditText(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._38sdp)
            )
            background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_filter_field)
            hint = searchHint
            inputType = InputType.TYPE_CLASS_TEXT
            isSingleLine = true
            setPadding(
                resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._10sdp),
                0,
                resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._10sdp),
                0
            )
            setTextColor(ContextCompat.getColor(requireContext(), R.color.ds_text_primary))
            setHintTextColor(ContextCompat.getColor(requireContext(), R.color.ds_text_muted))
            setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(com.intuit.ssp.R.dimen._12ssp))
            enableClearTextButton()
        }
        val optionsContainer = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
        }
        val optionsScrollView = ScrollView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._36sdp) * MAX_VISIBLE_FILTER_OPTIONS
            ).apply {
                topMargin = resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._6sdp)
            }
            isVerticalScrollBarEnabled = true
            overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS
            addView(optionsContainer)
        }
        dropdownContainer.addView(searchInput)
        dropdownContainer.addView(optionsScrollView)

        val popup = PopupWindow(
            dropdownContainer,
            anchor.width,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            true
        ).apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            elevation = resources.getDimension(com.intuit.sdp.R.dimen._8sdp)
            isOutsideTouchable = true
            setOnDismissListener {
                if (activeFilterPopup === this) activeFilterPopup = null
                anchor.setBackgroundResource(R.drawable.bg_filter_field)
            }
        }

        fun renderOptions(query: String) {
            optionsContainer.removeAllViews()
            when (stateForPopup) {
                UiState.Loading,
                UiState.Idle -> {
                    updatePopupOptionsViewport(optionsScrollView, 1)
                    optionsContainer.addView(createFilterMessageRow(getString(R.string.loading)))
                }

                is UiState.Success -> {
                    val filteredOptions = stateForPopup.data.filter {
                        query.isBlank() || it.name.contains(query, ignoreCase = true)
                    }
                    fun refreshPopupSelectionUi() {
                        updateFilterSelectionUi(
                            fieldText = fieldText,
                            chipsContainer = chipsContainer,
                            selectedOptions = selectedOptions,
                            placeholder = placeholder
                        ) { removedId ->
                            selectedOptions.remove(removedId)
                            selectedCountText?.text = getString(
                                R.string.filter_selected_count_dynamic,
                                selectedOptions.size,
                                stateForPopup.data.size
                            )
                            refreshPopupSelectionUi()
                            renderOptions(searchInput.text?.toString().orEmpty())
                        }
                    }
                    updatePopupOptionsViewport(optionsScrollView, filteredOptions.size.coerceAtLeast(1))
                    if (filteredOptions.isEmpty()) {
                        optionsContainer.addView(createFilterMessageRow(getString(R.string.empty_state)))
                        return
                    }
                    filteredOptions.forEach { option ->
                        optionsContainer.addView(
                            createFilterOptionRow(
                                option = option,
                                selected = selectedOptions.containsKey(option.id)
                            ) {
                                if (selectedOptions.containsKey(option.id)) {
                                    selectedOptions.remove(option.id)
                                } else {
                                    selectedOptions[option.id] = option.name
                                }
                                refreshPopupSelectionUi()
                                selectedCountText?.text = getString(
                                    R.string.filter_selected_count_dynamic,
                                    selectedOptions.size,
                                    stateForPopup.data.size
                                )
                                renderOptions(searchInput.text?.toString().orEmpty())
                            }
                        )
                    }
                }

                UiState.Empty -> {
                    updatePopupOptionsViewport(optionsScrollView, 1)
                    optionsContainer.addView(createFilterMessageRow(getString(R.string.empty_state)))
                }

                is UiState.Error -> {
                    updatePopupOptionsViewport(optionsScrollView, 1)
                    optionsContainer.addView(createFilterMessageRow(stateForPopup.message))
                }

                UiState.Unauthorized -> {
                    updatePopupOptionsViewport(optionsScrollView, 1)
                    optionsContainer.addView(createFilterMessageRow(getString(R.string.ia_login_microsoft_unavailable)))
                }
            }
        }

        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(text: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(text: CharSequence?, start: Int, before: Int, count: Int) {
                renderOptions(text?.toString().orEmpty())
            }

            override fun afterTextChanged(editable: Editable?) = Unit
        })

        anchor.setBackgroundResource(R.drawable.bg_filter_field_focused)
        renderOptions("")
        activeFilterPopup = popup
        popup.showAsDropDown(anchor, 0, resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._4sdp))
    }

    private fun renderFilterOptions(
        container: LinearLayout,
        optionsScrollView: View? = null,
        state: UiState<List<FilterOptionUi>>,
        selectedOptions: LinkedHashMap<String, String>,
        fieldText: TextView?,
        chipsContainer: RecyclerView,
        placeholder: String,
        selectedCountText: TextView? = null
    ) {
        container.removeAllViews()
        val visibleOptionCount = when (state) {
            is UiState.Success -> state.data.size.coerceAtLeast(1)
            else -> 1
        }
        updateFilterOptionsViewport(optionsScrollView, visibleOptionCount)
        when (state) {
            UiState.Loading -> container.addView(createFilterMessageRow(getString(R.string.loading)))
            is UiState.Success -> state.data.forEach { option ->
                container.addView(
                    createFilterOptionRow(
                        option = option,
                        selected = selectedOptions.containsKey(option.id)
                    ) {
                        if (selectedOptions.containsKey(option.id)) {
                            selectedOptions.remove(option.id)
                        } else {
                            selectedOptions[option.id] = option.name
                        }
                        updateFilterSelectionUi(
                            fieldText = fieldText,
                            chipsContainer = chipsContainer,
                            selectedOptions = selectedOptions,
                            placeholder = placeholder
                        ) { removedId ->
                            selectedOptions.remove(removedId)
                            renderFilterOptions(
                                container = container,
                                state = state,
                                selectedOptions = selectedOptions,
                                fieldText = fieldText,
                                chipsContainer = chipsContainer,
                                placeholder = placeholder,
                                selectedCountText = selectedCountText
                            )
                        }
                        selectedCountText?.text = getString(
                            R.string.filter_selected_count_dynamic,
                            selectedOptions.size,
                            state.data.size
                        )
                        renderFilterOptions(
                            container = container,
                            state = state,
                            selectedOptions = selectedOptions,
                            fieldText = fieldText,
                            chipsContainer = chipsContainer,
                            placeholder = placeholder,
                            selectedCountText = selectedCountText
                        )
                    }
                )
            }

            UiState.Empty -> container.addView(createFilterMessageRow(getString(R.string.empty_state)))
            is UiState.Error -> container.addView(createFilterMessageRow(state.message))
            UiState.Unauthorized -> container.addView(createFilterMessageRow(getString(R.string.ia_login_microsoft_unavailable)))
            else -> Unit
        }
        if (state is UiState.Success) {
            selectedCountText?.text = getString(
                R.string.filter_selected_count_dynamic,
                selectedOptions.size,
                state.data.size
            )
        }
        updateFilterSelectionUi(
            fieldText = fieldText,
            chipsContainer = chipsContainer,
            selectedOptions = selectedOptions,
            placeholder = placeholder
        ) { removedId ->
            selectedOptions.remove(removedId)
            renderFilterOptions(
                container = container,
                state = state,
                selectedOptions = selectedOptions,
                fieldText = fieldText,
                chipsContainer = chipsContainer,
                placeholder = placeholder,
                selectedCountText = selectedCountText
            )
        }
    }

    private fun createFilterOptionRow(
        option: FilterOptionUi,
        selected: Boolean,
        onToggle: () -> Unit
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
            foreground = context.obtainStyledAttributes(intArrayOf(android.R.attr.selectableItemBackground))
                .use { it.getDrawable(0) }
            setOnClickListener { onToggle() }
            gravity = android.view.Gravity.CENTER_VERTICAL
            orientation = LinearLayout.HORIZONTAL
            setPadding(
                resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._6sdp),
                0,
                resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._8sdp),
                0
            )
            addView(
                CheckBox(requireContext()).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._26sdp),
                        resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._26sdp)
                    )
                    isChecked = selected
                    buttonTintList = ContextCompat.getColorStateList(requireContext(), R.color.ds_outline_strong)
                    setOnClickListener { onToggle() }
                }
            )
            addView(
                TextView(requireContext()).apply {
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                        marginStart = resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._8sdp)
                    }
                    text = option.name
                    setTextColor(ContextCompat.getColor(requireContext(), R.color.ds_text_primary))
                    setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(com.intuit.ssp.R.dimen._12ssp))
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

    private fun updateFilterSelectionUi(
        fieldText: TextView?,
        chipsContainer: RecyclerView,
        selectedOptions: LinkedHashMap<String, String>,
        placeholder: String,
        onRemove: (String) -> Unit
    ) {
        val selectedNames = selectedOptions.values.toList()
        fieldText?.text = selectedNames.takeIf { it.isNotEmpty() }?.joinToString(", ") ?: placeholder
        fieldText?.setTextColor(
            ContextCompat.getColor(
                requireContext(),
                if (selectedNames.isEmpty()) R.color.ds_text_muted else R.color.ds_text_primary
            )
        )

        chipsContainer.isVisible = selectedOptions.isNotEmpty()
        if (chipsContainer.layoutManager == null) {
            chipsContainer.layoutManager = LinearLayoutManager(
                requireContext(),
                RecyclerView.HORIZONTAL,
                false
            )
        }
        chipsContainer.adapter = SelectedFilterChipAdapter(
            selectedOptions.map { (id, name) -> SelectedFilterChipUi(id, name) },
            onRemove = onRemove
        )
    }

    private fun refreshFilterSelectionUi(
        fieldText: TextView?,
        chipsContainer: RecyclerView,
        selectedOptions: LinkedHashMap<String, String>,
        placeholder: String
    ) {
        updateFilterSelectionUi(
            fieldText = fieldText,
            chipsContainer = chipsContainer,
            selectedOptions = selectedOptions,
            placeholder = placeholder
        ) { removedId ->
            selectedOptions.remove(removedId)
            refreshFilterSelectionUi(
                fieldText = fieldText,
                chipsContainer = chipsContainer,
                selectedOptions = selectedOptions,
                placeholder = placeholder
            )
        }
    }

    private fun resetFilterSheet(
        sheetBinding: LayoutProductFilterSheetBinding,
        filterState: ProductFilterSheetState,
        manufacturerFieldText: TextView?,
        vendorFieldText: TextView?
    ) {
        filterState.categories.clear()
        filterState.manufacturers.clear()
        filterState.vendors.clear()
        sheetBinding.minPriceEditText.setText("")
        sheetBinding.maxPriceEditText.setText("")
        filterState.minListPrice = null
        filterState.maxListPrice = null
        renderFilterOptions(
            container = sheetBinding.categoryOptionsContainer,
            optionsScrollView = sheetBinding.categoryOptionsScrollView,
            state = UiState.Success(filterState.categoryOptions),
            selectedOptions = filterState.categories,
            fieldText = sheetBinding.categoryFieldText,
            chipsContainer = sheetBinding.categoryChipsContainer,
            placeholder = getString(R.string.select_category),
            selectedCountText = sheetBinding.categorySelectedCountText
        )
        renderFilterOptions(
            container = sheetBinding.manufacturerOptionsContainer,
            optionsScrollView = sheetBinding.manufacturerOptionsScrollView,
            state = UiState.Success(filterState.manufacturerOptions),
            selectedOptions = filterState.manufacturers,
            fieldText = manufacturerFieldText,
            chipsContainer = sheetBinding.manufacturerChipsContainer,
            placeholder = getString(R.string.select_manufacturer)
        )
        renderFilterOptions(
            container = sheetBinding.vendorOptionsContainer,
            optionsScrollView = sheetBinding.vendorOptionsScrollView,
            state = UiState.Success(filterState.vendorOptions),
            selectedOptions = filterState.vendors,
            fieldText = vendorFieldText,
            chipsContainer = sheetBinding.vendorChipsContainer,
            placeholder = getString(R.string.select_vendor)
        )
    }

    private fun bindFilterSheetValues(
        sheetBinding: LayoutProductFilterSheetBinding,
        filterState: ProductFilterSheetState,
        manufacturerFieldText: TextView?,
        vendorFieldText: TextView?
    ) {
        sheetBinding.minPriceEditText.setText(filterState.minListPrice?.toString().orEmpty())
        sheetBinding.maxPriceEditText.setText(filterState.maxListPrice?.toString().orEmpty())
        refreshFilterSelectionUi(
            fieldText = sheetBinding.categoryFieldText,
            chipsContainer = sheetBinding.categoryChipsContainer,
            selectedOptions = filterState.categories,
            placeholder = getString(R.string.select_category)
        )
        refreshFilterSelectionUi(
            fieldText = manufacturerFieldText,
            chipsContainer = sheetBinding.manufacturerChipsContainer,
            selectedOptions = filterState.manufacturers,
            placeholder = getString(R.string.select_manufacturer)
        )
        refreshFilterSelectionUi(
            fieldText = vendorFieldText,
            chipsContainer = sheetBinding.vendorChipsContainer,
            selectedOptions = filterState.vendors,
            placeholder = getString(R.string.select_vendor)
        )
    }

    private fun applyProductFilters(
        sheetBinding: LayoutProductFilterSheetBinding,
        filterState: ProductFilterSheetState
    ): Boolean {
        val minPrice = sheetBinding.minPriceEditText.text?.toString()?.trim()?.takeIf { it.isNotEmpty() }?.toDoubleOrNull()
        val maxPrice = sheetBinding.maxPriceEditText.text?.toString()?.trim()?.takeIf { it.isNotEmpty() }?.toDoubleOrNull()
        if (minPrice != null && maxPrice != null && maxPrice < minPrice) {
            Toast.makeText(requireContext(), R.string.invalid_price_range, Toast.LENGTH_SHORT).show()
            return false
        }

        filterState.minListPrice = minPrice
        filterState.maxListPrice = maxPrice
        applyAppliedFilterState(filterState)
        return true
    }

    private fun applyAppliedFilterState(filterState: ProductFilterSheetState) {
        appliedFilterSheetState = filterState.copyForEditing()
        draftSearchQuery = filterState.searchText.trim()
        if (binding.searchEditText.text?.toString() != draftSearchQuery) {
            binding.searchEditText.setText(draftSearchQuery)
            binding.searchEditText.setSelection(binding.searchEditText.text?.length ?: 0)
        }
        if (!selectionMode) {
            binding.searchInputLayout.isVisible =
                draftSearchQuery.isNotBlank() || binding.searchInputLayout.isVisible
        }
        appliedProductFilters = appliedFilterSheetState.toProductSearchFilters()
        clearRenderedProducts()
        binding.productsRecyclerView.scrollToPosition(0)
        renderAppliedFilterChips()
        viewModel.loadProducts(draftSearchQuery, selectedStatus, appliedProductFilters)
    }

    private fun ProductFilterSheetState.toProductSearchFilters(): ProductSearchFilters {
        return ProductSearchFilters(
            vendorNames = vendors.values.toList(),
            manufacturerNames = manufacturers.values.toList(),
            categoryNames = (categories.values + listOfNotNull(dynamicCategorySpecs.categoryName))
                .filter { it.isNotBlank() }
                .distinct(),
            attributeTags = attributeTags.values.toList(),
            specificationOptions = dynamicCategorySpecs.selectedOptions
                .mapValues { (_, values) -> values.filter { it.isNotBlank() }.distinct() }
                .filterValues { it.isNotEmpty() },
            minListPrice = minListPrice,
            maxListPrice = maxListPrice,
            availabilityCode = availability?.stockCode,
            excludedAvailabilityCode = availability?.excludedStockCode
        )
    }

    private fun renderAppliedFilterChips() {
        val appliedChips = buildAppliedFilterChips()
        binding.appliedFiltersRecyclerView.isVisible = appliedChips.isNotEmpty()
        appliedFiltersAdapter.submitItems(appliedChips)
    }

    private fun buildAppliedFilterChips(): List<AppliedProductFilterChipUi> {
        val filterState = appliedFilterSheetState
        val chips = mutableListOf<AppliedProductFilterChipUi>()

        if (filterState.dynamicCategorySpecs.hasCategory) {
            chips.add(
                AppliedProductFilterChipUi(
                    id = APPLIED_DYNAMIC_CATEGORY_SPECS_ID,
                    label = filterState.dynamicCategorySpecs.toAppliedChipLabel()
                )
            )
        }
        if (filterState.searchText.isNotBlank()) {
            chips.add(
                AppliedProductFilterChipUi(
                    id = APPLIED_SEARCH_TEXT_ID,
                    label = getString(R.string.search_keyword) + ": ${filterState.searchText}"
                )
            )
        }
        filterState.manufacturers.forEach { (id, name) ->
            chips.add(
                AppliedProductFilterChipUi(
                    id = "$APPLIED_MANUFACTURER_PREFIX$id",
                    label = name
                )
            )
        }
        filterState.vendors.forEach { (id, name) ->
            chips.add(
                AppliedProductFilterChipUi(
                    id = "$APPLIED_VENDOR_PREFIX$id",
                    label = name
                )
            )
        }
        filterState.attributeTags.forEach { (id, name) ->
            chips.add(
                AppliedProductFilterChipUi(
                    id = "$APPLIED_ATTRIBUTE_TAG_PREFIX$id",
                    label = name
                )
            )
        }
        filterState.minListPrice?.let { minPrice ->
            chips.add(
                AppliedProductFilterChipUi(
                    id = APPLIED_MIN_PRICE_ID,
                    label = getString(R.string.applied_filter_min_price, minPrice.formatFilterPrice())
                )
            )
        }
        filterState.maxListPrice?.let { maxPrice ->
            chips.add(
                AppliedProductFilterChipUi(
                    id = APPLIED_MAX_PRICE_ID,
                    label = getString(R.string.applied_filter_max_price, maxPrice.formatFilterPrice())
                )
            )
        }
        filterState.availability?.let { availability ->
            chips.add(
                AppliedProductFilterChipUi(
                    id = APPLIED_AVAILABILITY_ID,
                    label = availability.displayName
                )
            )
        }

        if (chips.isNotEmpty()) {
            chips.add(
                AppliedProductFilterChipUi(
                    id = APPLIED_CLEAR_ALL_ID,
                    label = getString(R.string.clear_all),
                    isClearAll = true
                )
            )
        }

        return chips
    }

    private fun handleAppliedFilterAction(chip: AppliedProductFilterChipUi) {
        val filterState = appliedFilterSheetState.copyForEditing()
        when {
            chip.id == APPLIED_CLEAR_ALL_ID -> {
                applyAppliedFilterState(ProductFilterSheetState())
                return
            }

            chip.id == APPLIED_DYNAMIC_CATEGORY_SPECS_ID -> {
                filterState.dynamicCategorySpecs = SelectedCategorySpecs()
            }

            chip.id == APPLIED_SEARCH_TEXT_ID -> {
                filterState.searchText = ""
            }

            chip.id.startsWith(APPLIED_MANUFACTURER_PREFIX) -> {
                filterState.manufacturers.remove(chip.id.removePrefix(APPLIED_MANUFACTURER_PREFIX))
            }

            chip.id.startsWith(APPLIED_VENDOR_PREFIX) -> {
                filterState.vendors.remove(chip.id.removePrefix(APPLIED_VENDOR_PREFIX))
            }

            chip.id.startsWith(APPLIED_ATTRIBUTE_TAG_PREFIX) -> {
                filterState.attributeTags.remove(chip.id.removePrefix(APPLIED_ATTRIBUTE_TAG_PREFIX))
            }

            chip.id == APPLIED_MIN_PRICE_ID -> {
                filterState.minListPrice = null
            }

            chip.id == APPLIED_MAX_PRICE_ID -> {
                filterState.maxListPrice = null
            }

            chip.id == APPLIED_AVAILABILITY_ID -> {
                filterState.availability = null
            }
        }

        applyAppliedFilterState(filterState)
    }

    private fun Double.formatFilterPrice(): String {
        val format = if (this % 1.0 == 0.0) "$%,.0f" else "$%,.2f"
        return String.format(Locale.US, format, this)
    }

    private fun SelectedCategorySpecs.toAppliedChipLabel(): String {
        val category = categoryName.orEmpty()
        if (!hasSelectedOptions) return getString(R.string.applied_filter_dynamic_category, category)
        val values = selectedOptions.values.flatten().take(DYNAMIC_APPLIED_VALUES_LIMIT)
        val suffix = if (selectedOptionCount > DYNAMIC_APPLIED_VALUES_LIMIT) {
            getString(R.string.applied_filter_dynamic_more_count, selectedOptionCount - DYNAMIC_APPLIED_VALUES_LIMIT)
        } else {
            ""
        }
        return getString(
            R.string.applied_filter_dynamic_category_specs,
            category,
            values.joinToString() + suffix
        )
    }

    private fun observeDynamicCategorySpecsResult() {
        val savedStateHandle = findNavController().currentBackStackEntry?.savedStateHandle ?: return
        savedStateHandle
            .getLiveData<String>(DynamicCategorySpecsResult.REQUEST_KEY)
            .observe(viewLifecycleOwner) { result ->
                savedStateHandle.remove<String>(DynamicCategorySpecsResult.REQUEST_KEY)
                val specs = DynamicCategorySpecsResult.fromJson(result)
                val nextState = (pendingFilterSheetState ?: appliedFilterSheetState.copyForEditing()).apply {
                    dynamicCategorySpecs = specs
                }
                pendingFilterSheetState = nextState.copyForEditing()
                showFilterMenu(nextState)
            }
    }

    private fun observeAskAiSearchResult() {
        val savedStateHandle = findNavController().currentBackStackEntry?.savedStateHandle ?: return
        savedStateHandle
            .getLiveData<Bundle>(AskAiProductSearchResult.REQUEST_KEY)
            .observe(viewLifecycleOwner) { result ->
                savedStateHandle.remove<Bundle>(AskAiProductSearchResult.REQUEST_KEY)
                applyAskAiSearchResult(result)
            }
    }

    private fun applyAskAiSearchResult(result: Bundle) {
        draftSearchQuery = AskAiProductSearchResult.searchText(result).trim()
        binding.searchEditText.setText(draftSearchQuery)
        if (!selectionMode) {
            binding.searchInputLayout.isVisible = draftSearchQuery.isNotBlank()
        }

        AskAiProductSearchResult.status(result)?.let { aiStatus ->
            selectedStatus = when (aiStatus) {
                ProductAiStatus.Active -> ProductStatusFilter.Active
                ProductAiStatus.Inactive -> ProductStatusFilter.Inactive
                ProductAiStatus.All -> ProductStatusFilter.All
            }
        }

        val filterState = appliedFilterSheetState.copyForEditing().apply {
            manufacturers.clear()
            AskAiProductSearchResult.manufacturers(result).forEach { manufacturers[it.id] = it.name }
            vendors.clear()
            AskAiProductSearchResult.vendors(result).forEach { vendors[it.id] = it.name }
            minListPrice = AskAiProductSearchResult.minPrice(result)
            maxListPrice = AskAiProductSearchResult.maxPrice(result)
            availability = AskAiProductSearchResult.availability(result)
            dynamicCategorySpecs = AskAiProductSearchResult.dynamicCategorySpecs(result)
        }
        renderStatusTabs()
        applyAppliedFilterState(filterState)
    }

    private fun createFilterMessageRow(message: String): View {
        return TextView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._36sdp)
            )
            gravity = android.view.Gravity.CENTER_VERTICAL
            text = message
            setTextColor(ContextCompat.getColor(requireContext(), R.color.ds_text_muted))
            setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(com.intuit.ssp.R.dimen._11ssp))
        }
    }

    private fun updateFilterOptionsViewport(optionsScrollView: View?, optionCount: Int) {
        val visibleRows = optionCount.coerceIn(1, MAX_VISIBLE_FILTER_OPTIONS)
        val rowHeight = resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._34sdp)
        optionsScrollView?.let { view ->
            view.layoutParams = view.layoutParams.apply {
                height = rowHeight * visibleRows
            }
            view.isVerticalScrollBarEnabled = optionCount > MAX_VISIBLE_FILTER_OPTIONS
            view.requestLayout()
        }
    }

    private fun updatePopupOptionsViewport(optionsScrollView: ScrollView, optionCount: Int) {
        updateFilterOptionsViewport(optionsScrollView, optionCount)
    }

    private fun observeProducts() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.productsState.collect { state ->
                        binding.progress.isVisible = false

                        when (state) {
                            UiState.Loading -> {
                                hideStateMessage()
                                clearRenderedProducts()
                                showInitialShimmer()
                            }

                            is UiState.Success -> {
                                hideStateMessage()
                                hideInitialShimmer()
                                val previousProducts = products
                                products = state.data.products
                                totalProducts = state.data.total
                                if (!state.data.isLoadingNextPage) {
                                    renderProducts(allowAppend = state.data.isAppendUpdate && previousProducts.isNotEmpty())
                                } else {
                                    renderStatusTabs()
                                }
                                showPaginationShimmer(state.data.isLoadingNextPage)
                            }

                            UiState.Empty -> {
                                hideInitialShimmer()
                                showPaginationShimmer(false)
                                products = emptyList()
                                totalProducts = 0
                                renderedProductIds = emptyList()
                                productsAdapter.submitItems(emptyList())
                                showEmptyState()
                            }

                            is UiState.Error -> {
                                hideInitialShimmer()
                                showPaginationShimmer(false)
                                products = emptyList()
                                totalProducts = 0
                                renderedProductIds = emptyList()
                                productsAdapter.submitItems(emptyList())
                                when {
                                    state.message.isSessionExpiredMessage() -> showSessionExpiredState()
                                    state.message.isNoRecordFoundMessage() -> showEmptyState()
                                    else -> showErrorState(state.message)
                                }
                            }

                            UiState.Unauthorized -> {
                                hideInitialShimmer()
                                showPaginationShimmer(false)
                                products = emptyList()
                                totalProducts = 0
                                renderedProductIds = emptyList()
                                productsAdapter.submitItems(emptyList())
                                showSessionExpiredState()
                            }

                            else -> Unit
                        }
                    }
                }

                launch {
                    viewModel.tabCountsState.collect { counts ->
                        tabCounts = counts
                        renderStatusTabs()
                    }
                }
            }
        }
    }

    private fun setupPagination() {
        binding.productsRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (dy <= 0) return

                val layoutManager = recyclerView.layoutManager as? LinearLayoutManager ?: return
                val lastVisiblePosition = layoutManager.findLastVisibleItemPosition()
                val itemCount = layoutManager.itemCount
                if (itemCount > 0 && itemCount - lastVisiblePosition <= PAGINATION_VISIBLE_THRESHOLD) {
                    viewModel.loadNextPage()
                }
            }
        })
    }

    private fun submitSearch() {
        binding.searchEditText.clearFocus()
        binding.searchEditText.hideKeyboard()
        appliedFilterSheetState = appliedFilterSheetState.copyForEditing().apply {
            searchText = draftSearchQuery
        }
        clearRenderedProducts()
        binding.productsRecyclerView.scrollToPosition(0)
        viewModel.loadProducts(draftSearchQuery, selectedStatus, appliedProductFilters)
    }

    private fun renderProducts(allowAppend: Boolean = false) {
        renderStatusTabs()

        val visibleProducts = filteredProducts()
        val visibleProductIds = visibleProducts.map { it.id }
        if (visibleProducts.isEmpty() && !binding.shimmerContainer.isVisible) {
            showEmptyState()
        } else {
            hideStateMessage()
        }

        if (allowAppend && visibleProductIds.size >= renderedProductIds.size &&
            visibleProductIds.take(renderedProductIds.size) == renderedProductIds
        ) {
            productsAdapter.submitItems(visibleProducts)
            renderedProductIds = visibleProductIds
            return
        }

        productsAdapter.submitItems(visibleProducts)
        renderedProductIds = visibleProductIds
    }

    private fun clearRenderedProducts() {
        products = emptyList()
        renderedProductIds = emptyList()
        productsAdapter.submitItems(emptyList())
        hideStateMessage()
        showPaginationShimmer(false)
    }

    private fun filteredProducts(): List<ProductListItemUi> {
        return products
    }

    private fun bindProduct(binding: ItemProductListingBinding, product: ProductListItemUi) {
        binding.productName.text = product.name
        binding.productSku.text = product.sku
        binding.productPrice.text = product.displayPrice
        binding.thumbnailLabel.text = product.thumbnailLabel

        val thumbnailBackground = if (product.brandThumbnail) {
            R.drawable.bg_product_thumb_cisco
        } else {
            R.drawable.bg_product_thumb_placeholder
        }
        binding.thumbnailFrame.setBackgroundResource(thumbnailBackground)
        binding.thumbnailLabel.setTextColor(
            ContextCompat.getColor(
                requireContext(),
                if (product.brandThumbnail) R.color.ds_on_primary else R.color.ds_text_muted
            )
        )

        val statusColor = if (product.availability == ProductAvailability.InStock) {
            R.color.ds_success
        } else {
            R.color.ds_error
        }
        val statusBackground = if (product.availability == ProductAvailability.InStock) {
            R.drawable.bg_status_success
        } else {
            R.drawable.bg_status_error
        }
        binding.statusChip.text = product.availability.label
        binding.statusChip.setTextColor(ContextCompat.getColor(requireContext(), statusColor))
        binding.statusChip.setBackgroundResource(statusBackground)

        if (selectionMode) {
            val selected = selectedProducts.containsKey(product.id)
            binding.root.setBackgroundResource(
/*
                if (selected) R.drawable.bg_smart_quote_option_selected else R.drawable.bg_smart_quote_option
*/
                if (selected) R.drawable.bg_product_tab_selected else R.drawable.bg_smart_quote_option
            )
            binding.detailButton.setIconResource(
                if (selected) R.drawable.ia_ic_filter_check else R.drawable.ia_ic_add
            )
            binding.root.setOnClickListener { toggleSelectedProduct(product) }
            binding.detailButton.setOnClickListener { toggleSelectedProduct(product) }
            return
        }

        binding.root.setOnClickListener {
            findNavController().navigate(
                R.id.iaProductDetailFragment,
                product.toDetailBundle()
            )
        }
        binding.detailButton.setOnClickListener {
            findNavController().navigate(
                R.id.iaProductDetailFragment,
                product.toDetailBundle()
            )
        }
    }

    private fun ProductListItemUi.toDetailBundle() = bundleOf(
        "productId" to id,
        "productName" to name,
        "productSku" to sku,
        "productManufacturer" to manufacturer,
        "productManufacturerPartNumber" to manufacturerPartNumber,
        "productVendor" to vendor,
        "productCategory" to category,
        "productDescription" to description,
        "productCountryOfOrigin" to countryOfOrigin,
        "productScreenSize" to screenSize,
        "productDimensions" to dimensions,
        "productUnitOfMeasure" to unitOfMeasure,
        "productWeight" to weight,
        "productWarrantyPeriod" to warrantyPeriod,
        "productValidityPeriod" to validityPeriod,
        "productVendorNames" to ArrayList(vendors.map { it.name }),
        "productVendorSkus" to ArrayList(vendors.map { it.sku }),
        "productVendorAvailabilities" to ArrayList(vendors.map { it.availability }),
        "productVendorUnits" to ArrayList(vendors.map { it.unit }),
        "productVendorCostPrices" to ArrayList(vendors.map { it.costPrice }),
        "productVendorListPrices" to ArrayList(vendors.map { it.listPrice }),
        "productVendorStatuses" to ArrayList(vendors.map { it.status }),
        "productPrice" to displayPrice,
        "productAvailability" to availability.label,
        "productStatus" to status.name,
        "productThumbnailLabel" to thumbnailLabel,
        "productBrandThumbnail" to brandThumbnail
    )

    private fun renderStatusTabs() {
        binding.allTabCount.text = tabCounts.all
        binding.activeTabCount.text = tabCounts.active
        binding.inactiveTabCount.text = tabCounts.inactive

        updateTab(
            tab = binding.allTab,
            label = binding.allTabLabel,
            count = binding.allTabCount,
            selectedBackground = R.drawable.bg_product_segment_selected_left,
            selected = selectedStatus == ProductStatusFilter.All
        )
        updateTab(
            tab = binding.activeTab,
            label = binding.activeTabLabel,
            count = binding.activeTabCount,
            selectedBackground = R.drawable.bg_product_segment_selected_middle,
            selected = selectedStatus == ProductStatusFilter.Active
        )
        updateTab(
            tab = binding.inactiveTab,
            label = binding.inactiveTabLabel,
            count = binding.inactiveTabCount,
            selectedBackground = R.drawable.bg_product_segment_selected_right,
            selected = selectedStatus == ProductStatusFilter.Inactive
        )
    }

    private fun updateTab(
        tab: View,
        label: TextView,
        count: TextView,
        selectedBackground: Int,
        selected: Boolean
    ) {
        tab.setBackgroundResource(
            if (selected) selectedBackground else R.drawable.bg_product_segment_unselected
        )
        label.setTextColor(
            ContextCompat.getColor(
                requireContext(),
                if (selected) R.color.ds_primary else R.color.ds_text_secondary
            )
        )
        count.setTextColor(
            ContextCompat.getColor(
                requireContext(),
                if (selected) R.color.ds_primary else R.color.ds_text_primary
            )
        )
    }

    private fun showInitialShimmer() {
        binding.shimmerContainer.isVisible = true
        binding.productsRecyclerView.isVisible = false
        hideStateMessage()
        populateShimmer(binding.shimmerContainer, INITIAL_SHIMMER_COUNT)
    }

    private fun hideInitialShimmer() {
        binding.shimmerContainer.isVisible = false
        binding.productsRecyclerView.isVisible = true
        stopShimmerAnimations()
    }

    private fun showSessionExpiredState() {
        binding.stateContainer.isVisible = true
        binding.stateTitleText.text = getString(R.string.session_expired)
        binding.stateMessageText.text = getString(R.string.products_session_expired_message)
        binding.signInAgainButton.isVisible = true
        binding.retryText.isVisible = true
    }

    private fun showEmptyState() {
        binding.stateContainer.isVisible = true
        binding.stateTitleText.text = getString(R.string.empty_state)
        binding.stateMessageText.text = getString(R.string.products_empty_message)
        binding.signInAgainButton.isVisible = false
        binding.retryText.isVisible = true
    }

    private fun showErrorState(message: String) {
        binding.stateContainer.isVisible = true
        binding.stateTitleText.text = message.ifBlank { getString(R.string.empty_state) }
        binding.stateMessageText.text = getString(R.string.products_empty_message)
        binding.signInAgainButton.isVisible = false
        binding.retryText.isVisible = true
    }

    private fun hideStateMessage() {
        binding.stateContainer.isVisible = false
    }

    private fun String.isSessionExpiredMessage(): Boolean {
        return contains("401", ignoreCase = true) ||
            contains("unauthorized", ignoreCase = true) ||
            contains("session expired", ignoreCase = true)
    }

    private fun String.isNoRecordFoundMessage(): Boolean {
        return contains("no record", ignoreCase = true) ||
            contains("no records", ignoreCase = true)
    }

    private fun showPaginationShimmer(show: Boolean) {
        binding.paginationShimmerContainer.isVisible = show
        if (show) {
            populateShimmer(binding.paginationShimmerContainer, PAGINATION_SHIMMER_COUNT)
        } else {
            binding.paginationShimmerContainer.removeAllViews()
        }
    }

    private fun populateShimmer(container: ViewGroup, count: Int) {
        container.removeAllViews()
        repeat(count) {
            val view = layoutInflater.inflate(R.layout.item_product_shimmer, container, false)
            container.addView(view)
            startShimmerAnimation(view)
        }
    }

    private fun startShimmerAnimation(view: View) {
        val animator = ObjectAnimator.ofFloat(view, View.ALPHA, SHIMMER_MIN_ALPHA, 1f).apply {
            duration = SHIMMER_DURATION_MS
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            startDelay = (shimmerAnimators.size % INITIAL_SHIMMER_COUNT) * SHIMMER_STAGGER_MS
            start()
        }
        shimmerAnimators.add(animator)
    }

    private fun stopShimmerAnimations() {
        shimmerAnimators.forEach { it.cancel() }
        shimmerAnimators.clear()
    }

    override fun onDestroyView() {
        if (selectionMode) {
            setBottomNavigationVisible(true)
        }
        stopShimmerAnimations()
        super.onDestroyView()
        _binding = null
    }

    private companion object {
        const val PAGINATION_VISIBLE_THRESHOLD = 4
        const val INITIAL_SHIMMER_COUNT = 6
        const val PAGINATION_SHIMMER_COUNT = 2
        const val SHIMMER_DURATION_MS = 700L
        const val SHIMMER_STAGGER_MS = 80L
        const val SHIMMER_MIN_ALPHA = 0.45f
        const val FILTER_SHEET_HEIGHT_RATIO = 0.78f
        const val MAX_VISIBLE_FILTER_OPTIONS = 4
        const val APPLIED_SEARCH_TEXT_ID = "search_text"
        const val APPLIED_MANUFACTURER_PREFIX = "manufacturer:"
        const val APPLIED_VENDOR_PREFIX = "vendor:"
        const val APPLIED_ATTRIBUTE_TAG_PREFIX = "attribute_tag:"
        const val APPLIED_MIN_PRICE_ID = "min_price"
        const val APPLIED_MAX_PRICE_ID = "max_price"
        const val APPLIED_AVAILABILITY_ID = "availability"
        const val APPLIED_CLEAR_ALL_ID = "clear_all"
        const val APPLIED_DYNAMIC_CATEGORY_SPECS_ID = "dynamic_category_specs"
        const val DYNAMIC_APPLIED_VALUES_LIMIT = 2
    }
}

private data class AppliedProductFilterChipUi(
    val id: String,
    val label: String,
    val isClearAll: Boolean = false
)

private class AppliedProductFilterChipAdapter(
    private val onChipAction: (AppliedProductFilterChipUi) -> Unit
) : RecyclerView.Adapter<AppliedProductFilterChipAdapter.ChipViewHolder>() {
    private val items = mutableListOf<AppliedProductFilterChipUi>()

    fun submitItems(newItems: List<AppliedProductFilterChipUi>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChipViewHolder {
        val binding = ItemAppliedProductFilterChipBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ChipViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChipViewHolder, position: Int) {
        holder.bind(items[position], onChipAction)
    }

    override fun getItemCount(): Int = items.size

    class ChipViewHolder(
        private val binding: ItemAppliedProductFilterChipBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(
            item: AppliedProductFilterChipUi,
            onChipAction: (AppliedProductFilterChipUi) -> Unit
        ) {
            binding.appliedFilterText.text = item.label
            binding.removeFilterIcon.isVisible = !item.isClearAll
            binding.root.setOnClickListener { onChipAction(item) }
            binding.removeFilterIcon.setOnClickListener { onChipAction(item) }
        }
    }
}

private class ProductsAdapter(
    private val onBindProduct: (ItemProductListingBinding, ProductListItemUi) -> Unit
) : RecyclerView.Adapter<ProductsAdapter.ProductViewHolder>() {
    private val items = mutableListOf<ProductListItemUi>()

    fun submitItems(newItems: List<ProductListItemUi>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val binding = ItemProductListingBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ProductViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        onBindProduct(holder.binding, items[position])
    }

    override fun getItemCount(): Int = items.size

    class ProductViewHolder(
        val binding: ItemProductListingBinding
    ) : RecyclerView.ViewHolder(binding.root)
}

private data class SelectedFilterChipUi(
    val id: String,
    val label: String
)

private class SelectedFilterChipAdapter(
    private val items: List<SelectedFilterChipUi>,
    private val onRemove: (String) -> Unit
) : RecyclerView.Adapter<SelectedFilterChipAdapter.ChipViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChipViewHolder {
        val resources = parent.resources
        val container = LinearLayout(parent.context).apply {
            layoutParams = RecyclerView.LayoutParams(
                RecyclerView.LayoutParams.WRAP_CONTENT,
                resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._32sdp)
            ).apply {
                marginEnd = resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._8sdp)
            }
            setBackgroundResource(R.drawable.bg_filter_chip)
            gravity = android.view.Gravity.CENTER_VERTICAL
            orientation = LinearLayout.HORIZONTAL
            setPadding(
                resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._10sdp),
                0,
                resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._8sdp),
                0
            )
        }
        val label = TextView(parent.context).apply {
            setTextColor(ContextCompat.getColor(parent.context, R.color.ds_primary))
            setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(com.intuit.ssp.R.dimen._11ssp))
        }
        val removeIcon = ImageView(parent.context).apply {
            layoutParams = LinearLayout.LayoutParams(
                resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._18sdp),
                resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._18sdp)
            ).apply {
                marginStart = resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._8sdp)
            }
            setImageResource(R.drawable.ia_ic_filter_chip_close)
        }
        container.addView(label)
        container.addView(removeIcon)
        return ChipViewHolder(container, label, removeIcon)
    }

    override fun onBindViewHolder(holder: ChipViewHolder, position: Int) {
        val item = items[position]
        holder.label.text = item.label
        holder.removeIcon.setOnClickListener { onRemove(item.id) }
    }

    override fun getItemCount(): Int = items.size

    class ChipViewHolder(
        itemView: View,
        val label: TextView,
        val removeIcon: ImageView
    ) : RecyclerView.ViewHolder(itemView)
}
