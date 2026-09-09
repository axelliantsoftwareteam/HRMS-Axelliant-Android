package com.axelliant.hris.features.warehouse.presentation

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.TypedValue
import android.view.Gravity
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.core.content.res.use
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.axelliant.hris.R
import com.axelliant.hris.core.extensions.hideKeyboard
import com.axelliant.hris.core.extensions.showKeyboard
import com.axelliant.hris.core.ui.ShimmerAnimatorHelper
import com.axelliant.hris.core.ui.UiState
import com.axelliant.hris.databinding.FragmentWarehouseInventoryBinding
import com.axelliant.hris.databinding.ItemAppliedProductFilterChipBinding
import com.axelliant.hris.databinding.LayoutWarehouseInventoryFilterSheetBinding
import com.axelliant.hris.databinding.LayoutWarehouseInventorySelectFieldBinding
import com.axelliant.hris.features.internalapps.navigation.InternalAppsNavigator
import com.axelliant.hris.features.warehouse.domain.model.InventoryModel
import com.axelliant.hris.features.warehouse.domain.model.WarehouseLocationOptionModel
import com.axelliant.hris.features.warehouse.domain.model.WarehouseOptionModel
import com.axelliant.hris.ui.designsystem.components.createAppBottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetBehavior
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class WarehouseInventoryFragment : Fragment() {

    private val viewModel: WarehouseInventoryViewModel by viewModels()
    private var _binding: FragmentWarehouseInventoryBinding? = null
    private val binding get() = _binding!!
    private val adapter = WarehouseInventoryAdapter(::showInventoryActions)
    private val transactionsBottomSheet by lazy { InventoryTransactionsBottomSheet(this) }
    private val adjustInventoryBottomSheet by lazy { AdjustInventoryBottomSheet(this) }
    private val shimmerHelper = ShimmerAnimatorHelper()
    private val appliedFiltersAdapter = AppliedInventoryFilterChipAdapter(::handleAppliedFilterAction)
    private var appliedFilters = WarehouseInventoryFilters()
    private var draftFilters = WarehouseInventoryFilters()
    private var activeFilterPopup: PopupWindow? = null
    private var activeSheetBinding: LayoutWarehouseInventoryFilterSheetBinding? = null
    private var activeDropdown: InventoryDropdown? = null
    private var isSearchVisible = false

    private val ownershipOptions by lazy {
        listOf(
            InventoryStaticFilterOption(1, getString(R.string.inventory_ownership_company)),
            InventoryStaticFilterOption(2, getString(R.string.inventory_ownership_customer)),
            InventoryStaticFilterOption(3, getString(R.string.inventory_ownership_vendor))
        )
    }
    private val purposeOptions by lazy {
        listOf(
            InventoryStaticFilterOption(1, getString(R.string.inventory_purpose_stock)),
            InventoryStaticFilterOption(2, getString(R.string.inventory_purpose_customer_provided)),
            InventoryStaticFilterOption(3, getString(R.string.inventory_purpose_customer_reserved)),
            InventoryStaticFilterOption(4, getString(R.string.inventory_purpose_configuration)),
            InventoryStaticFilterOption(5, getString(R.string.inventory_purpose_return)),
            InventoryStaticFilterOption(6, getString(R.string.inventory_purpose_repair)),
            InventoryStaticFilterOption(7, getString(R.string.inventory_purpose_service))
        )
    }
    private val statusOptions by lazy {
        listOf(
            InventoryStaticFilterOption(1, getString(R.string.inventory_status_available)),
            InventoryStaticFilterOption(2, getString(R.string.inventory_status_reserved)),
            InventoryStaticFilterOption(3, getString(R.string.inventory_status_allocated)),
            InventoryStaticFilterOption(4, getString(R.string.inventory_status_picked)),
            InventoryStaticFilterOption(5, getString(R.string.inventory_status_in_transit)),
            InventoryStaticFilterOption(6, getString(R.string.inventory_status_configuration)),
            InventoryStaticFilterOption(7, getString(R.string.inventory_status_quarantine)),
            InventoryStaticFilterOption(8, getString(R.string.inventory_status_damaged)),
            InventoryStaticFilterOption(9, getString(R.string.inventory_status_shipped)),
            InventoryStaticFilterOption(10, getString(R.string.inventory_status_disposed)),
            InventoryStaticFilterOption(11, getString(R.string.inventory_status_pending_inspection))
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWarehouseInventoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupSearchUi()
        setupAppliedFilterChips()
        setupList()
        setupInteractions()
        observeInventory()
        observeFilterOptions()
        observeTransactions()
        renderAppliedFilterChips()
        viewModel.loadInventoryIfNeeded()
    }

    private fun setupSearchUi() {
        isSearchVisible = false
        binding.searchInputLayout.isVisible = false
        binding.searchInputLayout.alpha = 0f
        binding.appTopBar.searchButton.contentDescription =
            getString(R.string.inventory_search_open)
        binding.appTopBar.actionButton.contentDescription =
            getString(R.string.filters)
    }

    private fun setupAppliedFilterChips() {
        binding.appliedFiltersRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL, false)
            adapter = appliedFiltersAdapter
            itemAnimator = null
        }
    }

    private fun setupList() {
        binding.inventoryRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@WarehouseInventoryFragment.adapter
            setHasFixedSize(false)
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    if (dy <= 0) return
                    val layoutManager = recyclerView.layoutManager as? LinearLayoutManager ?: return
                    if (layoutManager.findLastVisibleItemPosition() >=
                        layoutManager.itemCount - PAGINATION_THRESHOLD_ITEMS
                    ) {
                        viewModel.loadNextPage()
                    }
                }
            })
        }
    }

    private fun setupInteractions() {
        binding.appTopBar.setOnBackClickListener {
            InternalAppsNavigator.returnToHomeShell(findNavController())
        }
        binding.appTopBar.setOnSearchClickListener {
            toggleSearchField()
        }
        binding.appTopBar.setOnActionClickListener {
            showFilterSheet()
        }
        binding.searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(editable: Editable?) {
                val query = editable?.toString().orEmpty()
                appliedFilters = appliedFilters.copy(search = query.trim())
                viewModel.onSearchChanged(query)
                renderAppliedFilterChips()
            }
        })
        binding.searchEditText.setOnEditorActionListener { _, actionId, event ->
            val isSubmitAction = actionId == EditorInfo.IME_ACTION_SEARCH ||
                actionId == EditorInfo.IME_ACTION_DONE
            val isEnterKey = event?.keyCode == KeyEvent.KEYCODE_ENTER &&
                event.action == KeyEvent.ACTION_UP
            if (isSubmitAction || isEnterKey) {
                binding.searchEditText.hideKeyboard()
                true
            } else {
                false
            }
        }
    }

    private fun showFilterSheet() {
        draftFilters = appliedFilters
        val sheetBinding = LayoutWarehouseInventoryFilterSheetBinding.inflate(layoutInflater)
        val dialog = requireContext().createAppBottomSheetDialog().apply {
            setContentView(sheetBinding.root)
            setCancelable(false)
            setCanceledOnTouchOutside(false)
        }
        activeSheetBinding = sheetBinding
        bindFilterSheetFields(sheetBinding)
        sheetBinding.root.setOnClickListener { activeFilterPopup?.dismiss() }
        sheetBinding.closeFilterButton.setOnClickListener {
            activeFilterPopup?.dismiss()
            dialog.dismiss()
        }
        sheetBinding.resetFiltersButton.setOnClickListener {
            activeFilterPopup?.dismiss()
            draftFilters = WarehouseInventoryFilters(search = appliedFilters.search)
            bindFilterSheetFields(sheetBinding)
        }
        sheetBinding.applyFiltersButton.setOnClickListener {
            activeFilterPopup?.dismiss()
            applyDraftFilters()
            dialog.dismiss()
        }
        dialog.setOnDismissListener {
            activeFilterPopup?.dismiss()
            activeFilterPopup = null
            activeSheetBinding = null
            activeDropdown = null
        }
        dialog.setOnShowListener {
            val bottomSheet = dialog.findViewById<FrameLayout>(
                com.google.android.material.R.id.design_bottom_sheet
            )
            bottomSheet?.let { sheet ->
                sheet.background = ColorDrawable(Color.TRANSPARENT)
                sheet.layoutParams = sheet.layoutParams.apply {
                    height = (resources.displayMetrics.heightPixels * FILTER_SHEET_HEIGHT_RATIO).toInt()
                }
                BottomSheetBehavior.from(sheet).apply {
                    isDraggable = false
                    skipCollapsed = true
                    state = BottomSheetBehavior.STATE_EXPANDED
                }
            }
        }
        dialog.show()
    }

    private fun bindFilterSheetFields(sheetBinding: LayoutWarehouseInventoryFilterSheetBinding) {
        bindField(
            binding = sheetBinding.warehouseFieldInclude,
            text = draftFilters.warehouseLabel,
            placeholder = getString(R.string.inventory_filter_warehouse)
        ) { showWarehouseDropdown() }
        bindField(
            binding = sheetBinding.locationFieldInclude,
            text = draftFilters.locationLabel,
            placeholder = getString(R.string.inventory_filter_location)
        ) { showLocationDropdown() }
        bindField(
            binding = sheetBinding.purchaseOrderFieldInclude,
            text = draftFilters.purchaseOrderLabel,
            placeholder = getString(R.string.inventory_filter_purchase_order)
        ) { showPurchaseOrderDropdown() }
        bindField(
            binding = sheetBinding.ownershipFieldInclude,
            text = draftFilters.ownershipLabel,
            placeholder = getString(R.string.inventory_filter_ownership)
        ) {
            showStaticDropdown(
                dropdown = InventoryDropdown.Ownership,
                fieldBinding = sheetBinding.ownershipFieldInclude,
                allLabel = getString(R.string.inventory_all_ownership),
                options = ownershipOptions
            ) { option ->
                draftFilters = draftFilters.copy(
                    ownershipType = option?.value,
                    ownershipLabel = option?.label
                )
                bindFilterSheetFields(sheetBinding)
            }
        }
        bindField(
            binding = sheetBinding.purposeFieldInclude,
            text = draftFilters.purposeLabel,
            placeholder = getString(R.string.inventory_filter_purpose)
        ) {
            showStaticDropdown(
                dropdown = InventoryDropdown.Purpose,
                fieldBinding = sheetBinding.purposeFieldInclude,
                allLabel = getString(R.string.inventory_all_purposes),
                options = purposeOptions
            ) { option ->
                draftFilters = draftFilters.copy(
                    purpose = option?.value,
                    purposeLabel = option?.label
                )
                bindFilterSheetFields(sheetBinding)
            }
        }
        bindField(
            binding = sheetBinding.statusFieldInclude,
            text = draftFilters.statusLabel,
            placeholder = getString(R.string.inventory_filter_status)
        ) {
            showStaticDropdown(
                dropdown = InventoryDropdown.Status,
                fieldBinding = sheetBinding.statusFieldInclude,
                allLabel = getString(R.string.inventory_all_statuses),
                options = statusOptions
            ) { option ->
                draftFilters = draftFilters.copy(
                    inventoryStatus = option?.value,
                    statusLabel = option?.label
                )
                bindFilterSheetFields(sheetBinding)
            }
        }
        sheetBinding.hideDepletedCheckbox.setOnCheckedChangeListener(null)
        sheetBinding.hideDepletedCheckbox.isChecked = draftFilters.hideDepleted
        sheetBinding.hideDepletedCheckbox.setOnCheckedChangeListener { _, checked ->
            draftFilters = draftFilters.copy(
                hideDepleted = checked,
                includeDepleted = !checked
            )
        }
    }

    private fun bindField(
        binding: LayoutWarehouseInventorySelectFieldBinding,
        text: String?,
        placeholder: String,
        onClick: () -> Unit
    ) {
        binding.selectFieldText.text = text ?: placeholder
        binding.selectFieldText.setTextColor(
            ContextCompat.getColor(
                requireContext(),
                if (text.isNullOrBlank()) R.color.ds_text_muted else R.color.ds_text_primary
            )
        )
        binding.selectField.setOnClickListener { onClick() }
    }

    private fun showWarehouseDropdown() {
        val sheetBinding = activeSheetBinding ?: return
        activeDropdown = InventoryDropdown.Warehouse
        if (showWarehouseDropdownIfReady()) return
        showLoadingDropdown(sheetBinding.warehouseFieldInclude.selectField)
        viewModel.loadWarehouseOptionsIfNeeded()
    }

    private fun showLocationDropdown() {
        val sheetBinding = activeSheetBinding ?: return
        activeDropdown = InventoryDropdown.Location
        if (draftFilters.warehouseId.isNullOrBlank()) {
            Toast.makeText(
                requireContext(),
                R.string.inventory_select_warehouse_first,
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        val warehouseId = draftFilters.warehouseId.orEmpty()
        if (viewModel.areLocationOptionsLoadedForWarehouse(warehouseId) && showLocationDropdownIfReady()) {
            return
        }
        showLoadingDropdown(sheetBinding.locationFieldInclude.selectField)
        viewModel.loadLocationOptionsForWarehouse(warehouseId)
    }

    private fun showPurchaseOrderDropdown() {
        val sheetBinding = activeSheetBinding ?: return
        activeDropdown = InventoryDropdown.PurchaseOrder
        if (showPurchaseOrderDropdownIfReady()) return
        showLoadingDropdown(sheetBinding.purchaseOrderFieldInclude.selectField)
        viewModel.loadPurchaseOrderOptionsIfNeeded()
    }

    private fun showWarehouseDropdownIfReady(): Boolean {
        val sheetBinding = activeSheetBinding ?: return false
        val state = viewModel.warehouseOptionsState.value as? UiState.Success ?: return false
        if (activeDropdown != InventoryDropdown.Warehouse) return true
        showOptionDropdown(
            anchor = sheetBinding.warehouseFieldInclude.selectField,
            allLabel = getString(R.string.inventory_all_warehouses),
            options = state.data.map { option ->
                InventoryDropdownOption(option.id, option.selectorLabel())
            }
        ) { selected ->
            draftFilters = draftFilters.copy(
                warehouseId = selected?.id,
                warehouseLabel = selected?.label,
                locationId = null,
                locationLabel = null
            )
            bindFilterSheetFields(sheetBinding)
        }
        return true
    }

    private fun showLocationDropdownIfReady(): Boolean {
        val sheetBinding = activeSheetBinding ?: return false
        val state = viewModel.locationOptionsState.value as? UiState.Success ?: return false
        if (activeDropdown != InventoryDropdown.Location) return true
        showOptionDropdown(
            anchor = sheetBinding.locationFieldInclude.selectField,
            allLabel = getString(R.string.inventory_all_locations),
            options = state.data.map { option ->
                InventoryDropdownOption(option.id, option.selectorLabel())
            }
        ) { selected ->
            draftFilters = draftFilters.copy(
                locationId = selected?.id,
                locationLabel = selected?.label
            )
            bindFilterSheetFields(sheetBinding)
        }
        return true
    }

    private fun showPurchaseOrderDropdownIfReady(): Boolean {
        val sheetBinding = activeSheetBinding ?: return false
        val state = viewModel.purchaseOrderOptionsState.value as? UiState.Success ?: return false
        if (activeDropdown != InventoryDropdown.PurchaseOrder) return true
        showOptionDropdown(
            anchor = sheetBinding.purchaseOrderFieldInclude.selectField,
            allLabel = getString(R.string.inventory_all_purchase_orders),
            options = state.data.map { option ->
                InventoryDropdownOption(option.id, option.number)
            }
        ) { selected ->
            draftFilters = draftFilters.copy(
                purchaseOrderId = selected?.id,
                purchaseOrderLabel = selected?.label
            )
            bindFilterSheetFields(sheetBinding)
        }
        return true
    }

    private fun showStaticDropdown(
        dropdown: InventoryDropdown,
        fieldBinding: LayoutWarehouseInventorySelectFieldBinding,
        allLabel: String,
        options: List<InventoryStaticFilterOption>,
        onSelected: (InventoryStaticFilterOption?) -> Unit
    ) {
        activeDropdown = dropdown
        showOptionDropdown(
            anchor = fieldBinding.selectField,
            allLabel = allLabel,
            options = options.map { InventoryDropdownOption(it.value.toString(), it.label) }
        ) { selected ->
            onSelected(options.firstOrNull { it.value.toString() == selected?.id })
        }
    }

    private fun showLoadingDropdown(anchor: View) {
        showOptionDropdown(
            anchor = anchor,
            allLabel = null,
            options = listOf(InventoryDropdownOption(LOADING_ID, getString(R.string.loading))),
            onSelected = {}
        )
    }

    private fun showOptionDropdown(
        anchor: View,
        allLabel: String?,
        options: List<InventoryDropdownOption>,
        onSelected: (InventoryDropdownOption?) -> Unit
    ) {
        activeFilterPopup?.dismiss()
        anchor.setBackgroundResource(R.drawable.bg_filter_field_focused)
        val rows = buildList {
            allLabel?.let { add(InventoryDropdownOption(ALL_FILTER_ID, it)) }
            addAll(options)
        }
        val optionsContainer = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.bg_filter_dropdown_panel)
            setPadding(
                resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._8sdp),
                resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._6sdp),
                resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._8sdp),
                resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._6sdp)
            )
        }
        rows.forEach { option ->
            optionsContainer.addView(createDropdownRow(option) {
                activeFilterPopup?.dismiss()
                if (option.id != LOADING_ID) {
                    onSelected(option.takeUnless { it.id == ALL_FILTER_ID })
                }
            })
        }
        val popupHeight = resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._36sdp) *
            rows.size.coerceAtMost(MAX_VISIBLE_FILTER_OPTIONS)
        val popup = PopupWindow(
            optionsContainer,
            anchor.width,
            popupHeight.coerceAtLeast(resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._44sdp)),
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
        activeFilterPopup = popup
        popup.showAsDropDown(anchor, 0, resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._4sdp))
    }

    private fun createDropdownRow(
        option: InventoryDropdownOption,
        onClick: () -> Unit
    ): View {
        return TextView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._36sdp)
            )
            gravity = Gravity.CENTER_VERTICAL
            text = option.label
            setTextColor(ContextCompat.getColor(requireContext(), R.color.ds_text_primary))
            setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(com.intuit.ssp.R.dimen._12ssp))
            setPadding(
                resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._10sdp),
                0,
                resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._10sdp),
                0
            )
            foreground = context.obtainStyledAttributes(intArrayOf(android.R.attr.selectableItemBackground))
                .use { it.getDrawable(0) }
            setOnClickListener { onClick() }
        }
    }

    private fun observeInventory() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.inventoryState.collect { state ->
                    when (state) {
                        UiState.Loading -> showLoading()
                        is UiState.Success -> showSuccess(state.data)
                        UiState.Empty -> showEmpty()
                        is UiState.Error -> showError(state.message)
                        UiState.Unauthorized -> showError(getString(R.string.inventory_error))
                        else -> Unit
                    }
                }
            }
        }
    }

    private fun observeFilterOptions() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.warehouseOptionsState.collect { state ->
                        when (state) {
                            is UiState.Success -> showWarehouseDropdownIfReady()
                            is UiState.Error -> showFilterError(state.message)
                            UiState.Unauthorized -> showFilterError(getString(R.string.inventory_filter_error))
                            else -> Unit
                        }
                    }
                }
                launch {
                    viewModel.locationOptionsState.collect { state ->
                        when (state) {
                            is UiState.Success -> showLocationDropdownIfReady()
                            is UiState.Error -> showFilterError(state.message)
                            UiState.Unauthorized -> showFilterError(getString(R.string.inventory_filter_error))
                            else -> Unit
                        }
                    }
                }
                launch {
                    viewModel.purchaseOrderOptionsState.collect { state ->
                        when (state) {
                            is UiState.Success -> showPurchaseOrderDropdownIfReady()
                            is UiState.Error -> showFilterError(state.message)
                            UiState.Unauthorized -> showFilterError(getString(R.string.inventory_filter_error))
                            else -> Unit
                        }
                    }
                }
            }
        }
    }

    private fun showFilterError(message: String) {
        activeFilterPopup?.dismiss()
        val text = if (message == WarehouseInventoryViewModel.LOCATION_REQUIRES_WAREHOUSE) {
            getString(R.string.inventory_select_warehouse_first)
        } else {
            message.ifBlank { getString(R.string.inventory_filter_error) }
        }
        Toast.makeText(requireContext(), text, Toast.LENGTH_SHORT).show()
    }

    private fun showInventoryActions(inventory: InventoryModel, anchor: View) {
        val popupMenu = PopupMenu(anchor.context, anchor)
        popupMenu.menuInflater.inflate(R.menu.menu_inventory_actions, popupMenu.menu)
        popupMenu.setForceShowIcon(true)
        popupMenu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_inventory_adjust_quantity -> {
                    adjustInventoryBottomSheet.show(inventory)
                    true
                }
                R.id.action_inventory_view_transactions -> {
                    transactionsBottomSheet.show(inventory)
                    viewModel.loadTransactions(inventory)
                    true
                }
                else -> false
            }
        }
        popupMenu.show()
    }

    private fun observeTransactions() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.transactionsState.collect { state ->
                    transactionsBottomSheet.render(
                        when (state) {
                            UiState.Unauthorized -> UiState.Error(
                                getString(R.string.inventory_transactions_error)
                            )
                            else -> state
                        }
                    )
                }
            }
        }
    }

    private fun showLoading() {
        showPaginationShimmer(false)
        binding.inventoryRecyclerView.isVisible = false
        binding.emptyStateText.isVisible = false
        binding.errorStateText.isVisible = false
        binding.shimmerContainer.isVisible = true
        shimmerHelper.populate(
            container = binding.shimmerContainer,
            inflater = layoutInflater,
            itemLayoutRes = R.layout.item_sale_order_shimmer,
            count = SHIMMER_ITEM_COUNT
        )
    }

    private fun showSuccess(data: WarehouseInventoryUiModel) {
        shimmerHelper.clear(binding.shimmerContainer)
        binding.shimmerContainer.isVisible = false
        binding.errorStateText.isVisible = false
        appliedFilters = data.filters
        renderAppliedFilterChips()
        binding.totalEntriesText.text =
            resources.getQuantityString(
                R.plurals.inventory_count,
                data.totalCount,
                data.totalCount
            )
        val isEmpty = data.items.isEmpty()
        binding.emptyStateText.isVisible = isEmpty
        binding.inventoryRecyclerView.isVisible = !isEmpty
        adapter.submitList(data.items)
        showPaginationShimmer(data.isLoadingNextPage)
    }

    private fun showEmpty() {
        shimmerHelper.clear(binding.shimmerContainer)
        binding.shimmerContainer.isVisible = false
        showPaginationShimmer(false)
        binding.inventoryRecyclerView.isVisible = false
        binding.errorStateText.isVisible = false
        binding.emptyStateText.isVisible = true
        binding.totalEntriesText.text = getString(R.string.inventory_empty)
        adapter.submitList(emptyList())
    }

    private fun showError(message: String) {
        shimmerHelper.clear(binding.shimmerContainer)
        binding.shimmerContainer.isVisible = false
        showPaginationShimmer(false)
        binding.inventoryRecyclerView.isVisible = false
        binding.emptyStateText.isVisible = false
        binding.errorStateText.isVisible = true
        binding.errorStateText.text = message.ifBlank { getString(R.string.inventory_error) }
        adapter.submitList(emptyList())
    }

    private fun showPaginationShimmer(show: Boolean) {
        binding.paginationShimmerContainer.isVisible = show
        if (show) {
            shimmerHelper.populate(
                container = binding.paginationShimmerContainer,
                inflater = layoutInflater,
                itemLayoutRes = R.layout.item_sale_order_shimmer,
                count = 1
            )
        } else {
            shimmerHelper.clear(binding.paginationShimmerContainer)
        }
    }

    private fun toggleSearchField() {
        if (isSearchVisible) {
            closeSearchField(clearText = true)
        } else {
            openSearchField()
        }
    }

    private fun openSearchField() {
        isSearchVisible = true
        binding.searchInputLayout.isVisible = true
        binding.searchInputLayout.alpha = 0f
        binding.searchInputLayout.animate()
            .alpha(1f)
            .setDuration(SEARCH_ANIMATION_DURATION_MS)
            .start()
        binding.searchEditText.post {
            if (_binding == null || !isSearchVisible) return@post
            binding.searchEditText.requestFocus()
            binding.searchEditText.showKeyboard()
        }
    }

    private fun closeSearchField(clearText: Boolean) {
        isSearchVisible = false
        binding.searchEditText.clearFocus()
        binding.searchEditText.hideKeyboard()
        binding.searchInputLayout.animate()
            .alpha(0f)
            .setDuration(SEARCH_ANIMATION_DURATION_MS)
            .withEndAction {
                if (_binding == null) return@withEndAction
                binding.searchInputLayout.isVisible = false
            }
            .start()
        if (clearText) {
            binding.searchEditText.setText("")
            appliedFilters = appliedFilters.copy(search = "")
            viewModel.applyFilters(appliedFilters)
            renderAppliedFilterChips()
        }
    }

    private fun applyDraftFilters() {
        appliedFilters = draftFilters
        viewModel.applyFilters(appliedFilters)
        renderAppliedFilterChips()
    }

    private fun renderAppliedFilterChips() {
        val chips = buildAppliedFilterChips()
        binding.appliedFiltersRecyclerView.isVisible = chips.isNotEmpty()
        appliedFiltersAdapter.submitItems(chips)
    }

    private fun buildAppliedFilterChips(): List<AppliedInventoryFilterChipUi> {
        val chips = mutableListOf<AppliedInventoryFilterChipUi>()
        if (appliedFilters.search.isNotBlank()) {
            chips.add(
                AppliedInventoryFilterChipUi(
                    id = APPLIED_SEARCH_ID,
                    label = "${getString(R.string.search_keyword)}: ${appliedFilters.search}"
                )
            )
        }
        appliedFilters.warehouseLabel?.let {
            chips.add(AppliedInventoryFilterChipUi(APPLIED_WAREHOUSE_ID, it))
        }
        appliedFilters.locationLabel?.let {
            chips.add(AppliedInventoryFilterChipUi(APPLIED_LOCATION_ID, it))
        }
        appliedFilters.purchaseOrderLabel?.let {
            chips.add(AppliedInventoryFilterChipUi(APPLIED_PURCHASE_ORDER_ID, it))
        }
        appliedFilters.ownershipLabel?.let {
            chips.add(AppliedInventoryFilterChipUi(APPLIED_OWNERSHIP_ID, it))
        }
        appliedFilters.purposeLabel?.let {
            chips.add(AppliedInventoryFilterChipUi(APPLIED_PURPOSE_ID, it))
        }
        appliedFilters.statusLabel?.let {
            chips.add(AppliedInventoryFilterChipUi(APPLIED_STATUS_ID, it))
        }
        if (appliedFilters.hideDepleted) {
            chips.add(AppliedInventoryFilterChipUi(APPLIED_HIDE_DEPLETED_ID, getString(R.string.inventory_hide_depleted)))
        }
        if (chips.isNotEmpty()) {
            chips.add(
                AppliedInventoryFilterChipUi(
                    id = APPLIED_CLEAR_ALL_ID,
                    label = getString(R.string.clear_all),
                    isClearAll = true
                )
            )
        }
        return chips
    }

    private fun handleAppliedFilterAction(chip: AppliedInventoryFilterChipUi) {
        appliedFilters = when (chip.id) {
            APPLIED_CLEAR_ALL_ID -> WarehouseInventoryFilters()
            APPLIED_SEARCH_ID -> appliedFilters.copy(search = "")
            APPLIED_WAREHOUSE_ID -> appliedFilters.copy(
                warehouseId = null,
                warehouseLabel = null,
                locationId = null,
                locationLabel = null
            )
            APPLIED_LOCATION_ID -> appliedFilters.copy(locationId = null, locationLabel = null)
            APPLIED_PURCHASE_ORDER_ID -> appliedFilters.copy(
                purchaseOrderId = null,
                purchaseOrderLabel = null
            )
            APPLIED_OWNERSHIP_ID -> appliedFilters.copy(ownershipType = null, ownershipLabel = null)
            APPLIED_PURPOSE_ID -> appliedFilters.copy(purpose = null, purposeLabel = null)
            APPLIED_STATUS_ID -> appliedFilters.copy(inventoryStatus = null, statusLabel = null)
            APPLIED_HIDE_DEPLETED_ID -> appliedFilters.copy(
                hideDepleted = false,
                includeDepleted = true
            )
            else -> appliedFilters
        }
        if (binding.searchEditText.text?.toString().orEmpty() != appliedFilters.search) {
            binding.searchEditText.setText(appliedFilters.search)
            binding.searchEditText.setSelection(binding.searchEditText.text?.length ?: 0)
        }
        viewModel.applyFilters(appliedFilters)
        renderAppliedFilterChips()
    }

    private fun WarehouseOptionModel.selectorLabel(): String {
        val nameValue = name.takeIf { it.isNotBlank() && it != "-" }
        val codeValue = code.takeIf { it.isNotBlank() && it != "-" }
        return listOfNotNull(nameValue, codeValue?.let { "($it)" }).joinToString(" ")
            .ifBlank { getString(R.string.warehouse_not_available) }
    }

    private fun WarehouseLocationOptionModel.selectorLabel(): String {
        val nameValue = name.takeIf { it.isNotBlank() && it != "-" }
        val pathValue = path.takeIf { it.isNotBlank() && it != "-" && it != nameValue }
        return listOfNotNull(nameValue, pathValue).joinToString(" - ")
            .ifBlank { getString(R.string.warehouse_not_available) }
    }

    override fun onDestroyView() {
        activeFilterPopup?.dismiss()
        adjustInventoryBottomSheet.dismiss()
        transactionsBottomSheet.dismiss()
        shimmerHelper.release()
        super.onDestroyView()
        _binding = null
    }

    private enum class InventoryDropdown {
        Warehouse,
        Location,
        PurchaseOrder,
        Ownership,
        Purpose,
        Status
    }

    private companion object {
        const val ALL_FILTER_ID = "all"
        const val LOADING_ID = "loading"
        const val PAGINATION_THRESHOLD_ITEMS = 2
        const val SHIMMER_ITEM_COUNT = 5
        const val FILTER_SHEET_HEIGHT_RATIO = 0.78f
        const val MAX_VISIBLE_FILTER_OPTIONS = 6
        const val SEARCH_ANIMATION_DURATION_MS = 180L
        const val APPLIED_SEARCH_ID = "search"
        const val APPLIED_WAREHOUSE_ID = "warehouse"
        const val APPLIED_LOCATION_ID = "location"
        const val APPLIED_PURCHASE_ORDER_ID = "purchase_order"
        const val APPLIED_OWNERSHIP_ID = "ownership"
        const val APPLIED_PURPOSE_ID = "purpose"
        const val APPLIED_STATUS_ID = "status"
        const val APPLIED_HIDE_DEPLETED_ID = "hide_depleted"
        const val APPLIED_CLEAR_ALL_ID = "clear_all"
    }
}

private data class InventoryDropdownOption(
    val id: String,
    val label: String
)

private data class AppliedInventoryFilterChipUi(
    val id: String,
    val label: String,
    val isClearAll: Boolean = false
)

private class AppliedInventoryFilterChipAdapter(
    private val onChipAction: (AppliedInventoryFilterChipUi) -> Unit
) : RecyclerView.Adapter<AppliedInventoryFilterChipAdapter.ChipViewHolder>() {

    private val items = mutableListOf<AppliedInventoryFilterChipUi>()

    fun submitItems(newItems: List<AppliedInventoryFilterChipUi>) {
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
            item: AppliedInventoryFilterChipUi,
            onChipAction: (AppliedInventoryFilterChipUi) -> Unit
        ) {
            binding.appliedFilterText.text = item.label
            binding.removeFilterIcon.isVisible = !item.isClearAll
            binding.root.setOnClickListener { onChipAction(item) }
            binding.removeFilterIcon.setOnClickListener { onChipAction(item) }
        }
    }
}
