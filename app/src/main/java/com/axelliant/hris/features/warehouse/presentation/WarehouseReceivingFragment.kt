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
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.core.content.res.use
import androidx.core.os.bundleOf
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
import com.axelliant.hris.databinding.BottomSheetCreateWarehouseReceiptBinding
import com.axelliant.hris.databinding.BottomSheetScannedWarehouseReceiptBinding
import com.axelliant.hris.databinding.DialogWarehouseDeleteConfirmationBinding
import com.axelliant.hris.databinding.FragmentWarehouseReceivingBinding
import com.axelliant.hris.databinding.ItemAppliedProductFilterChipBinding
import com.axelliant.hris.databinding.LayoutWarehouseInventorySelectFieldBinding
import com.axelliant.hris.databinding.LayoutWarehouseReceivingFilterSheetBinding
import com.axelliant.hris.features.internalapps.navigation.InternalAppsNavigator
import com.axelliant.hris.features.warehouse.domain.model.PurchaseOrderOptionModel
import com.axelliant.hris.features.warehouse.domain.model.WarehouseOptionModel
import com.axelliant.hris.features.warehouse.domain.model.WarehouseReceiptModel
import com.axelliant.hris.ui.designsystem.components.installAppViewTreeOwners
import com.axelliant.hris.ui.designsystem.components.createAppBottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class WarehouseReceivingFragment : Fragment() {

    private val viewModel: WarehouseReceivingViewModel by viewModels()
    private var _binding: FragmentWarehouseReceivingBinding? = null
    private val binding get() = _binding!!
    private val adapter = WarehouseReceivingAdapter(::showReceivingActions)
    private val shimmerHelper = ShimmerAnimatorHelper()
    private val appliedFiltersAdapter = AppliedReceivingFilterChipAdapter(::handleAppliedFilterAction)
    private var appliedFilters = WarehouseReceivingFilters()
    private var draftFilters = WarehouseReceivingFilters()
    private var activeFilterPopup: PopupWindow? = null
    private var activeSheetBinding: LayoutWarehouseReceivingFilterSheetBinding? = null
    private var activeCreateSheetBinding: BottomSheetCreateWarehouseReceiptBinding? = null
    private var activeDropdown: ReceivingDropdown? = null
    private var isSearchVisible = false
    private var isFabMenuOpen = false
    private lateinit var fabMenuBackCallback: OnBackPressedCallback
    private var createReceiptDraft = CreateReceiptDraft()

    private val receiptTypeOptions by lazy {
        listOf(
            ReceivingStaticFilterOption(1, getString(R.string.receiving_type_purchase_order)),
            ReceivingStaticFilterOption(2, getString(R.string.receiving_type_customer_provided))
        )
    }

    private val receiptStatusOptions by lazy {
        listOf(
            ReceivingStaticFilterOption(1, getString(R.string.receiving_status_expected)),
            ReceivingStaticFilterOption(2, getString(R.string.receiving_status_receiving)),
            ReceivingStaticFilterOption(3, getString(R.string.receiving_status_inspection)),
            ReceivingStaticFilterOption(4, getString(R.string.receiving_status_accepted)),
            ReceivingStaticFilterOption(5, getString(R.string.receiving_status_rejected)),
            ReceivingStaticFilterOption(6, getString(R.string.receiving_status_putaway_pending)),
            ReceivingStaticFilterOption(7, getString(R.string.receiving_status_completed))
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWarehouseReceivingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupSearchUi()
        setupFabMenu()
        setupAppliedFilterChips()
        setupList()
        setupInteractions()
        observeScannerResult()
        observeReceipts()
        observeFilterOptions()
        renderAppliedFilterChips()
        viewModel.loadReceiptsIfNeeded()
    }

    private fun setupSearchUi() {
        isSearchVisible = false
        binding.searchInputLayout.isVisible = false
        binding.searchInputLayout.alpha = 0f
        binding.appTopBar.searchButton.contentDescription = getString(R.string.receiving_search_open)
        binding.appTopBar.actionButton.contentDescription = getString(R.string.filters)
    }

    private fun setupAppliedFilterChips() {
        binding.appliedFiltersRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL, false)
            adapter = appliedFiltersAdapter
            itemAnimator = null
        }
    }

    private fun setupList() {
        binding.receivingRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@WarehouseReceivingFragment.adapter
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
            if (isFabMenuOpen) {
                closeFabMenu()
            } else {
                InternalAppsNavigator.returnToHomeShell(findNavController())
            }
        }
        binding.appTopBar.setOnSearchClickListener {
            if (isFabMenuOpen) {
                closeFabMenu()
                return@setOnSearchClickListener
            }
            toggleSearchField()
        }
        binding.appTopBar.setOnActionClickListener {
            showFilterSheet()
        }
        binding.createReceiptFab.setOnClickListener { toggleFabMenu() }
        binding.fabMenuScrim.setOnClickListener { closeFabMenu() }
        binding.createReceiptOptionFab.setOnClickListener {
            closeFabMenu()
            showCreateReceiptSheet()
        }
        binding.createReceiptLabel.setOnClickListener { binding.createReceiptOptionFab.performClick() }
        binding.scanReceiptFab.setOnClickListener {
            closeFabMenu()
            findNavController().navigate(R.id.iaWarehouseReceiptScannerFragment)
        }
        binding.scanReceiptLabel.setOnClickListener { binding.scanReceiptFab.performClick() }
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

    private fun setupFabMenu() {
        binding.fabMenuScrim.isVisible = false
        binding.fabMenuScrim.alpha = 0f
        binding.createReceiptActionRow.isVisible = false
        binding.scanReceiptActionRow.isVisible = false
        prepareClosedFabAction(binding.createReceiptActionRow)
        prepareClosedFabAction(binding.scanReceiptActionRow)
        fabMenuBackCallback = object : OnBackPressedCallback(false) {
            override fun handleOnBackPressed() {
                closeFabMenu()
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, fabMenuBackCallback)
    }

    private fun toggleFabMenu() {
        if (isFabMenuOpen) closeFabMenu() else openFabMenu()
    }

    private fun openFabMenu() {
        if (isFabMenuOpen) return
        isFabMenuOpen = true
        fabMenuBackCallback.isEnabled = true
        binding.fabMenuScrim.isVisible = true
        binding.fabMenuScrim.animate()
            .alpha(1f)
            .setDuration(FAB_MENU_ANIMATION_MS)
            .setInterpolator(DecelerateInterpolator())
            .start()
        binding.createReceiptFab.setImageResource(R.drawable.ic_close)
        binding.createReceiptFab.contentDescription = getString(R.string.receiving_fab_close)
        binding.createReceiptFab.animate()
            .rotation(90f)
            .setDuration(FAB_MENU_ANIMATION_MS)
            .start()
        showFabAction(binding.createReceiptActionRow, delayMs = 40L)
        showFabAction(binding.scanReceiptActionRow, delayMs = 90L)
    }

    private fun closeFabMenu() {
        if (!isFabMenuOpen) return
        isFabMenuOpen = false
        fabMenuBackCallback.isEnabled = false
        binding.createReceiptFab.setImageResource(R.drawable.ia_ic_add)
        binding.createReceiptFab.contentDescription = getString(R.string.receiving_fab_open)
        binding.createReceiptFab.animate()
            .rotation(0f)
            .setDuration(FAB_MENU_ANIMATION_MS)
            .start()
        hideFabAction(binding.scanReceiptActionRow, delayMs = 0L)
        hideFabAction(binding.createReceiptActionRow, delayMs = 40L)
        binding.fabMenuScrim.animate()
            .alpha(0f)
            .setDuration(FAB_MENU_ANIMATION_MS)
            .withEndAction {
                if (_binding == null) return@withEndAction
                binding.fabMenuScrim.isVisible = false
            }
            .start()
    }

    private fun prepareClosedFabAction(row: View) {
        row.alpha = 0f
        row.translationY = FAB_ACTION_TRANSLATION_Y
        row.scaleX = 0.85f
        row.scaleY = 0.85f
    }

    private fun showFabAction(row: View, delayMs: Long) {
        row.animate().cancel()
        prepareClosedFabAction(row)
        row.isVisible = true
        row.animate()
            .alpha(1f)
            .translationY(0f)
            .scaleX(1f)
            .scaleY(1f)
            .setStartDelay(delayMs)
            .setDuration(FAB_MENU_ANIMATION_MS)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    private fun hideFabAction(row: View, delayMs: Long) {
        row.animate().cancel()
        row.animate()
            .alpha(0f)
            .translationY(FAB_ACTION_TRANSLATION_Y)
            .scaleX(0.85f)
            .scaleY(0.85f)
            .setStartDelay(delayMs)
            .setDuration(FAB_MENU_ANIMATION_MS)
            .withEndAction {
                if (_binding == null) return@withEndAction
                row.isVisible = false
            }
            .start()
    }

    private fun showFilterSheet() {
        draftFilters = appliedFilters
        val sheetBinding = LayoutWarehouseReceivingFilterSheetBinding.inflate(layoutInflater)
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
            draftFilters = WarehouseReceivingFilters(search = appliedFilters.search)
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

    private fun showCreateReceiptSheet() {
        createReceiptDraft = CreateReceiptDraft()
        val sheetBinding = BottomSheetCreateWarehouseReceiptBinding.inflate(layoutInflater)
        val dialog = requireContext().createAppBottomSheetDialog().apply {
            setContentView(sheetBinding.root)
            setCancelable(false)
            setCanceledOnTouchOutside(false)
        }
        activeCreateSheetBinding = sheetBinding
        bindCreateReceiptSheet(sheetBinding)
        sheetBinding.root.setOnClickListener { activeFilterPopup?.dismiss() }
        sheetBinding.closeButton.setOnClickListener {
            activeFilterPopup?.dismiss()
            dialog.dismiss()
        }
        sheetBinding.cancelButton.setOnClickListener {
            activeFilterPopup?.dismiss()
            dialog.dismiss()
        }
        sheetBinding.createButton.setOnClickListener {
            Toast.makeText(
                requireContext(),
                getString(R.string.receiving_create_save_not_connected),
                Toast.LENGTH_SHORT
            ).show()
        }
        sheetBinding.addLineButton.setOnClickListener {
            sheetBinding.manualLineContainer.isVisible = true
            sheetBinding.itemsEmptyText.isVisible = false
        }
        sheetBinding.removeLineButton.setOnClickListener {
            sheetBinding.manualLineContainer.isVisible = false
            sheetBinding.itemsEmptyText.isVisible = true
        }
        dialog.setOnDismissListener {
            activeFilterPopup?.dismiss()
            activeFilterPopup = null
            activeCreateSheetBinding = null
            activeDropdown = null
        }
        dialog.setOnShowListener {
            val bottomSheet = dialog.findViewById<FrameLayout>(
                com.google.android.material.R.id.design_bottom_sheet
            )
            bottomSheet?.let { sheet ->
                sheet.background = ColorDrawable(Color.TRANSPARENT)
                sheet.layoutParams = sheet.layoutParams.apply {
                    height = (resources.displayMetrics.heightPixels * CREATE_SHEET_HEIGHT_RATIO).toInt()
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

    private fun observeScannerResult() {
        findNavController().currentBackStackEntry?.savedStateHandle
            ?.getLiveData<Bundle>(WarehouseReceiptScannerResult.REQUEST_KEY)
            ?.observe(viewLifecycleOwner) { bundle ->
                findNavController().currentBackStackEntry?.savedStateHandle
                    ?.remove<Bundle>(WarehouseReceiptScannerResult.REQUEST_KEY)
                val scannedValue = bundle.getString(WarehouseReceiptScannerResult.RESULT_VALUE).orEmpty()
                showScannedReceiptSheet(scannedValue)
            }
    }

    private fun showScannedReceiptSheet(scannedValue: String) {
        val receipt = ScannedReceiptUi.dummy(scannedValue)
        val sheetBinding = BottomSheetScannedWarehouseReceiptBinding.inflate(layoutInflater)
        val dialog = requireContext().createAppBottomSheetDialog().apply {
            setContentView(sheetBinding.root)
            setCancelable(false)
            setCanceledOnTouchOutside(false)
        }
        bindScannedReceiptSheet(sheetBinding, receipt)
        sheetBinding.closeButton.setOnClickListener { dialog.dismiss() }
        sheetBinding.cancelButton.setOnClickListener { dialog.dismiss() }
        sheetBinding.createButton.setOnClickListener {
            Toast.makeText(
                requireContext(),
                getString(R.string.receiving_create_save_not_connected),
                Toast.LENGTH_SHORT
            ).show()
        }
        dialog.setOnShowListener {
            val bottomSheet = dialog.findViewById<FrameLayout>(
                com.google.android.material.R.id.design_bottom_sheet
            )
            bottomSheet?.let { sheet ->
                sheet.background = ColorDrawable(Color.TRANSPARENT)
                sheet.layoutParams = sheet.layoutParams.apply {
                    height = (resources.displayMetrics.heightPixels * CREATE_SHEET_HEIGHT_RATIO).toInt()
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

    private fun bindScannedReceiptSheet(
        sheetBinding: BottomSheetScannedWarehouseReceiptBinding,
        receipt: ScannedReceiptUi
    ) = with(sheetBinding) {
        scannedPayloadText.text = getString(R.string.receiving_scanned_result_format, receipt.scannedValue)
        warehouseField.fieldLabel.text = getString(R.string.receiving_filter_warehouse)
        warehouseField.fieldValue.text = receipt.warehouse
        typeField.fieldLabel.text = getString(R.string.receiving_filter_type)
        typeField.fieldValue.text = receipt.receiptType
        purchaseOrderField.fieldLabel.text = getString(R.string.receiving_filter_purchase_order)
        purchaseOrderField.fieldValue.text = receipt.purchaseOrder
        expectedDateField.fieldLabel.text = getString(R.string.receiving_create_expected_date)
        expectedDateField.fieldValue.text = receipt.expectedDate
        notesField.fieldLabel.text = getString(R.string.receiving_create_notes)
        notesField.fieldValue.text = receipt.notes
        itemProductText.text = receipt.itemProduct
        itemQuantityText.text = receipt.itemQuantity
    }

    private fun bindCreateReceiptSheet(sheetBinding: BottomSheetCreateWarehouseReceiptBinding) {
        val hasWarehouse = !createReceiptDraft.warehouseId.isNullOrBlank()
        val isCustomerProvided = createReceiptDraft.receiptType == CUSTOMER_PROVIDED_TYPE
        bindField(
            binding = sheetBinding.warehouseFieldInclude,
            text = createReceiptDraft.warehouseLabel,
            placeholder = getString(R.string.receiving_filter_warehouse)
        ) { showCreateWarehouseDropdown() }
        bindField(
            binding = sheetBinding.typeFieldInclude,
            text = createReceiptDraft.receiptTypeLabel,
            placeholder = getString(R.string.receiving_filter_type)
        ) {
            if (hasWarehouse) {
                showCreateTypeDropdown()
            } else {
                showFilterError(getString(R.string.putaway_select_warehouse_first))
            }
        }
        setCreateFieldEnabled(sheetBinding.typeFieldInclude, hasWarehouse)
        sheetBinding.sourceLabel.text = if (isCustomerProvided) {
            getString(R.string.receiving_create_customer_id)
        } else {
            getString(R.string.receiving_create_purchase_order_required)
        }
        sheetBinding.purchaseOrderFieldInclude.root.isVisible = !isCustomerProvided
        sheetBinding.customerIdEditText.isVisible = isCustomerProvided
        bindField(
            binding = sheetBinding.purchaseOrderFieldInclude,
            text = createReceiptDraft.purchaseOrderLabel,
            placeholder = getString(R.string.receiving_filter_purchase_order)
        ) {
            if (hasWarehouse) {
                showCreatePurchaseOrderDropdown()
            } else {
                showFilterError(getString(R.string.putaway_select_warehouse_first))
            }
        }
        setCreateFieldEnabled(sheetBinding.purchaseOrderFieldInclude, hasWarehouse)
        listOf(
            sheetBinding.customerIdEditText,
            sheetBinding.expectedDateEditText,
            sheetBinding.notesEditText,
            sheetBinding.addLineButton,
            sheetBinding.createButton
        ).forEach { view ->
            view.isEnabled = hasWarehouse
            view.alpha = if (hasWarehouse) ENABLED_ALPHA else DISABLED_ALPHA
        }
        sheetBinding.itemsSubtitleText.text = if (isCustomerProvided) {
            getString(R.string.receiving_create_items_customer_subtitle)
        } else {
            getString(R.string.receiving_create_items_po_subtitle)
        }
        sheetBinding.itemsEmptyText.text = if (isCustomerProvided) {
            ""
        } else {
            getString(R.string.receiving_create_items_empty_po)
        }
        sheetBinding.addLineButton.isVisible = isCustomerProvided
        sheetBinding.manualLineContainer.isVisible = isCustomerProvided && hasWarehouse
        sheetBinding.itemsEmptyText.isVisible = !isCustomerProvided
        bindField(
            binding = sheetBinding.productFieldInclude,
            text = null,
            placeholder = getString(R.string.receiving_create_product_placeholder)
        ) {
            Toast.makeText(requireContext(), R.string.coming_soon, Toast.LENGTH_SHORT).show()
        }
    }

    private fun bindFilterSheetFields(sheetBinding: LayoutWarehouseReceivingFilterSheetBinding) {
        bindField(
            binding = sheetBinding.warehouseFieldInclude,
            text = draftFilters.warehouseLabel,
            placeholder = getString(R.string.receiving_filter_warehouse)
        ) { showWarehouseDropdown() }
        bindField(
            binding = sheetBinding.purchaseOrderFieldInclude,
            text = draftFilters.purchaseOrderLabel,
            placeholder = getString(R.string.receiving_filter_purchase_order)
        ) { showPurchaseOrderDropdown() }
        bindField(
            binding = sheetBinding.typeFieldInclude,
            text = draftFilters.receiptTypeLabel,
            placeholder = getString(R.string.receiving_filter_type)
        ) {
            showStaticDropdown(
                dropdown = ReceivingDropdown.Type,
                fieldBinding = sheetBinding.typeFieldInclude,
                allLabel = getString(R.string.receiving_all_types),
                options = receiptTypeOptions
            ) { option ->
                draftFilters = draftFilters.copy(
                    receiptType = option?.value,
                    receiptTypeLabel = option?.label
                )
                bindFilterSheetFields(sheetBinding)
            }
        }
        bindField(
            binding = sheetBinding.statusFieldInclude,
            text = draftFilters.receiptStatusLabel,
            placeholder = getString(R.string.receiving_filter_status)
        ) {
            showStaticDropdown(
                dropdown = ReceivingDropdown.Status,
                fieldBinding = sheetBinding.statusFieldInclude,
                allLabel = getString(R.string.receiving_all_statuses),
                options = receiptStatusOptions
            ) { option ->
                draftFilters = draftFilters.copy(
                    receiptStatus = option?.value,
                    receiptStatusLabel = option?.label
                )
                bindFilterSheetFields(sheetBinding)
            }
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

    private fun setCreateFieldEnabled(
        binding: LayoutWarehouseInventorySelectFieldBinding,
        enabled: Boolean
    ) {
        binding.selectField.isEnabled = enabled
        binding.selectField.alpha = if (enabled) ENABLED_ALPHA else DISABLED_ALPHA
    }

    private fun showWarehouseDropdown() {
        val sheetBinding = activeSheetBinding ?: return
        activeDropdown = ReceivingDropdown.Warehouse
        if (showWarehouseDropdownIfReady()) return
        showLoadingDropdown(sheetBinding.warehouseFieldInclude.selectField)
        viewModel.loadWarehouseOptionsIfNeeded()
    }

    private fun showPurchaseOrderDropdown() {
        val sheetBinding = activeSheetBinding ?: return
        activeDropdown = ReceivingDropdown.PurchaseOrder
        if (showPurchaseOrderDropdownIfReady()) return
        showLoadingDropdown(sheetBinding.purchaseOrderFieldInclude.selectField)
        viewModel.loadPurchaseOrderOptionsIfNeeded()
    }

    private fun showCreateWarehouseDropdown() {
        val sheetBinding = activeCreateSheetBinding ?: return
        activeDropdown = ReceivingDropdown.CreateWarehouse
        if (showCreateWarehouseDropdownIfReady()) return
        showLoadingDropdown(sheetBinding.warehouseFieldInclude.selectField)
        viewModel.loadWarehouseOptionsIfNeeded()
    }

    private fun showCreateTypeDropdown() {
        val sheetBinding = activeCreateSheetBinding ?: return
        activeDropdown = ReceivingDropdown.CreateType
        showOptionDropdown(
            anchor = sheetBinding.typeFieldInclude.selectField,
            allLabel = null,
            options = receiptTypeOptions.map {
                ReceivingDropdownOption(it.value.toString(), it.label)
            }
        ) { selected ->
            val selectedType = receiptTypeOptions.firstOrNull { it.value.toString() == selected?.id }
            createReceiptDraft = createReceiptDraft.copy(
                receiptType = selectedType?.value,
                receiptTypeLabel = selectedType?.label,
                purchaseOrderId = null,
                purchaseOrderLabel = null
            )
            bindCreateReceiptSheet(sheetBinding)
        }
    }

    private fun showCreatePurchaseOrderDropdown() {
        val sheetBinding = activeCreateSheetBinding ?: return
        activeDropdown = ReceivingDropdown.CreatePurchaseOrder
        if (showCreatePurchaseOrderDropdownIfReady()) return
        showLoadingDropdown(sheetBinding.purchaseOrderFieldInclude.selectField)
        viewModel.loadPurchaseOrderOptionsIfNeeded()
    }

    private fun showWarehouseDropdownIfReady(): Boolean {
        val sheetBinding = activeSheetBinding ?: return false
        val state = viewModel.warehouseOptionsState.value as? UiState.Success ?: return false
        if (activeDropdown != ReceivingDropdown.Warehouse) return true
        showOptionDropdown(
            anchor = sheetBinding.warehouseFieldInclude.selectField,
            allLabel = getString(R.string.receiving_all_warehouses),
            options = state.data.map { option ->
                ReceivingDropdownOption(option.id, option.selectorLabel())
            }
        ) { selected ->
            draftFilters = draftFilters.copy(
                warehouseId = selected?.id,
                warehouseLabel = selected?.label
            )
            bindFilterSheetFields(sheetBinding)
        }
        return true
    }

    private fun showCreateWarehouseDropdownIfReady(): Boolean {
        val sheetBinding = activeCreateSheetBinding ?: return false
        val state = viewModel.warehouseOptionsState.value as? UiState.Success ?: return false
        if (activeDropdown != ReceivingDropdown.CreateWarehouse) return true
        showOptionDropdown(
            anchor = sheetBinding.warehouseFieldInclude.selectField,
            allLabel = null,
            options = state.data.map { option ->
                ReceivingDropdownOption(option.id, option.selectorLabel())
            }
        ) { selected ->
            createReceiptDraft = createReceiptDraft.copy(
                warehouseId = selected?.id,
                warehouseLabel = selected?.label,
                receiptType = PURCHASE_ORDER_TYPE,
                receiptTypeLabel = getString(R.string.receiving_type_purchase_order),
                purchaseOrderId = null,
                purchaseOrderLabel = null
            )
            bindCreateReceiptSheet(sheetBinding)
        }
        return true
    }

    private fun showCreatePurchaseOrderDropdownIfReady(): Boolean {
        val sheetBinding = activeCreateSheetBinding ?: return false
        val state = viewModel.purchaseOrderOptionsState.value as? UiState.Success ?: return false
        if (activeDropdown != ReceivingDropdown.CreatePurchaseOrder) return true
        showOptionDropdown(
            anchor = sheetBinding.purchaseOrderFieldInclude.selectField,
            allLabel = null,
            options = state.data.map { option ->
                ReceivingDropdownOption(option.id, option.number)
            }
        ) { selected ->
            createReceiptDraft = createReceiptDraft.copy(
                purchaseOrderId = selected?.id,
                purchaseOrderLabel = selected?.label
            )
            bindCreateReceiptSheet(sheetBinding)
        }
        return true
    }

    private fun showPurchaseOrderDropdownIfReady(): Boolean {
        val sheetBinding = activeSheetBinding ?: return false
        val state = viewModel.purchaseOrderOptionsState.value as? UiState.Success ?: return false
        if (activeDropdown != ReceivingDropdown.PurchaseOrder) return true
        showOptionDropdown(
            anchor = sheetBinding.purchaseOrderFieldInclude.selectField,
            allLabel = getString(R.string.receiving_all_purchase_orders),
            options = state.data.map { option ->
                ReceivingDropdownOption(option.id, option.number)
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
        dropdown: ReceivingDropdown,
        fieldBinding: LayoutWarehouseInventorySelectFieldBinding,
        allLabel: String,
        options: List<ReceivingStaticFilterOption>,
        onSelected: (ReceivingStaticFilterOption?) -> Unit
    ) {
        activeDropdown = dropdown
        showOptionDropdown(
            anchor = fieldBinding.selectField,
            allLabel = allLabel,
            options = options.map { ReceivingDropdownOption(it.value.toString(), it.label) }
        ) { selected ->
            onSelected(options.firstOrNull { it.value.toString() == selected?.id })
        }
    }

    private fun showLoadingDropdown(anchor: View) {
        showOptionDropdown(
            anchor = anchor,
            allLabel = null,
            options = listOf(ReceivingDropdownOption(LOADING_ID, getString(R.string.loading))),
            onSelected = {}
        )
    }

    private fun showOptionDropdown(
        anchor: View,
        allLabel: String?,
        options: List<ReceivingDropdownOption>,
        onSelected: (ReceivingDropdownOption?) -> Unit
    ) {
        activeFilterPopup?.dismiss()
        anchor.setBackgroundResource(R.drawable.bg_filter_field_focused)
        val rows = buildList {
            allLabel?.let { add(ReceivingDropdownOption(ALL_FILTER_ID, it)) }
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
        option: ReceivingDropdownOption,
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

    private fun observeReceipts() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.receivingState.collect { state ->
                    when (state) {
                        UiState.Loading -> showLoading()
                        is UiState.Success -> showSuccess(state.data)
                        UiState.Empty -> showEmpty()
                        is UiState.Error -> showError(state.message)
                        UiState.Unauthorized -> showError(getString(R.string.receiving_error))
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
                                .also { showCreateWarehouseDropdownIfReady() }
                            is UiState.Error -> showFilterError(state.message)
                            UiState.Unauthorized -> showFilterError(getString(R.string.receiving_filter_error))
                            else -> Unit
                        }
                    }
                }
                launch {
                    viewModel.purchaseOrderOptionsState.collect { state ->
                        when (state) {
                            is UiState.Success -> showPurchaseOrderDropdownIfReady()
                                .also { showCreatePurchaseOrderDropdownIfReady() }
                            is UiState.Error -> showFilterError(state.message)
                            UiState.Unauthorized -> showFilterError(getString(R.string.receiving_filter_error))
                            else -> Unit
                        }
                    }
                }
            }
        }
    }

    private fun showFilterError(message: String) {
        activeFilterPopup?.dismiss()
        Toast.makeText(
            requireContext(),
            message.ifBlank { getString(R.string.receiving_filter_error) },
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun showReceivingActions(receipt: WarehouseReceiptModel, anchor: View) {
        val popupMenu = PopupMenu(anchor.context, anchor)
        popupMenu.menuInflater.inflate(R.menu.menu_warehouse_receiving_actions, popupMenu.menu)
        popupMenu.menu.findItem(R.id.action_receiving_delete)?.icon?.mutate()?.setTint(
            ContextCompat.getColor(anchor.context, R.color.ds_error)
        )
        popupMenu.setForceShowIcon(true)
        popupMenu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_receiving_view_manage -> {
                    findNavController().navigate(
                        R.id.iaWarehouseReceivingDetailFragment,
                        bundleOf(WarehouseReceivingDetailViewModel.ARG_RECEIPT_ID to receipt.id)
                    )
                    true
                }
                R.id.action_receiving_delete -> {
                    showDeleteConfirmation(receipt)
                    true
                }
                else -> false
            }
        }
        popupMenu.show()
    }

    private fun showDeleteConfirmation(receipt: WarehouseReceiptModel) {
        val dialogBinding = DialogWarehouseDeleteConfirmationBinding.inflate(layoutInflater)
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogBinding.root)
            .setCancelable(true)
            .create()
        dialogBinding.root.installAppViewTreeOwners(
            lifecycleOwner = viewLifecycleOwner,
            viewModelStoreOwner = this,
            savedStateRegistryOwner = this
        )
        dialog.installAppViewTreeOwners(
            lifecycleOwner = viewLifecycleOwner,
            viewModelStoreOwner = this,
            savedStateRegistryOwner = this
        )

        dialogBinding.deleteConfirmYesButton.setOnClickListener {
            dialog.dismiss()
            Toast.makeText(
                requireContext(),
                getString(R.string.receiving_delete_coming_soon, receipt.receiptNumber),
                Toast.LENGTH_SHORT
            ).show()
        }
        dialogBinding.deleteConfirmCancelButton.setOnClickListener {
            dialog.dismiss()
        }
        dialog.setOnShowListener {
            dialog.installAppViewTreeOwners(
                lifecycleOwner = viewLifecycleOwner,
                viewModelStoreOwner = this,
                savedStateRegistryOwner = this
            )
            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
        dialog.show()
    }

    private fun showLoading() {
        showPaginationShimmer(false)
        binding.receivingRecyclerView.isVisible = false
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

    private fun showSuccess(data: WarehouseReceivingUiModel) {
        shimmerHelper.clear(binding.shimmerContainer)
        binding.shimmerContainer.isVisible = false
        binding.errorStateText.isVisible = false
        appliedFilters = data.filters
        renderAppliedFilterChips()
        binding.totalEntriesText.text = resources.getQuantityString(
            R.plurals.receiving_count,
            data.totalCount,
            data.totalCount
        )
        val isEmpty = data.receipts.isEmpty()
        binding.emptyStateText.isVisible = isEmpty
        binding.receivingRecyclerView.isVisible = !isEmpty
        adapter.submitList(data.receipts)
        showPaginationShimmer(data.isLoadingNextPage)
    }

    private fun showEmpty() {
        shimmerHelper.clear(binding.shimmerContainer)
        binding.shimmerContainer.isVisible = false
        showPaginationShimmer(false)
        binding.receivingRecyclerView.isVisible = false
        binding.errorStateText.isVisible = false
        binding.emptyStateText.isVisible = true
        binding.totalEntriesText.text = getString(R.string.receiving_empty)
        adapter.submitList(emptyList())
    }

    private fun showError(message: String) {
        shimmerHelper.clear(binding.shimmerContainer)
        binding.shimmerContainer.isVisible = false
        showPaginationShimmer(false)
        binding.receivingRecyclerView.isVisible = false
        binding.emptyStateText.isVisible = false
        binding.errorStateText.isVisible = true
        binding.errorStateText.text = message.ifBlank { getString(R.string.receiving_error) }
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

    private fun buildAppliedFilterChips(): List<AppliedReceivingFilterChipUi> {
        val chips = mutableListOf<AppliedReceivingFilterChipUi>()
        if (appliedFilters.search.isNotBlank()) {
            chips.add(
                AppliedReceivingFilterChipUi(
                    id = APPLIED_SEARCH_ID,
                    label = "${getString(R.string.search_keyword)}: ${appliedFilters.search}"
                )
            )
        }
        appliedFilters.warehouseLabel?.let {
            chips.add(AppliedReceivingFilterChipUi(APPLIED_WAREHOUSE_ID, it))
        }
        appliedFilters.purchaseOrderLabel?.let {
            chips.add(AppliedReceivingFilterChipUi(APPLIED_PURCHASE_ORDER_ID, it))
        }
        appliedFilters.receiptTypeLabel?.let {
            chips.add(AppliedReceivingFilterChipUi(APPLIED_TYPE_ID, it))
        }
        appliedFilters.receiptStatusLabel?.let {
            chips.add(AppliedReceivingFilterChipUi(APPLIED_STATUS_ID, it))
        }
        if (chips.isNotEmpty()) {
            chips.add(
                AppliedReceivingFilterChipUi(
                    id = APPLIED_CLEAR_ALL_ID,
                    label = getString(R.string.clear_all),
                    isClearAll = true
                )
            )
        }
        return chips
    }

    private fun handleAppliedFilterAction(chip: AppliedReceivingFilterChipUi) {
        appliedFilters = when (chip.id) {
            APPLIED_CLEAR_ALL_ID -> WarehouseReceivingFilters()
            APPLIED_SEARCH_ID -> appliedFilters.copy(search = "")
            APPLIED_WAREHOUSE_ID -> appliedFilters.copy(warehouseId = null, warehouseLabel = null)
            APPLIED_PURCHASE_ORDER_ID -> appliedFilters.copy(
                purchaseOrderId = null,
                purchaseOrderLabel = null
            )
            APPLIED_TYPE_ID -> appliedFilters.copy(receiptType = null, receiptTypeLabel = null)
            APPLIED_STATUS_ID -> appliedFilters.copy(receiptStatus = null, receiptStatusLabel = null)
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

    override fun onDestroyView() {
        activeFilterPopup?.dismiss()
        shimmerHelper.release()
        super.onDestroyView()
        _binding = null
    }

    private enum class ReceivingDropdown {
        Warehouse,
        PurchaseOrder,
        Type,
        Status,
        CreateWarehouse,
        CreateType,
        CreatePurchaseOrder
    }

    private companion object {
        const val ALL_FILTER_ID = "all"
        const val LOADING_ID = "loading"
        const val PAGINATION_THRESHOLD_ITEMS = 2
        const val SHIMMER_ITEM_COUNT = 5
        const val FILTER_SHEET_HEIGHT_RATIO = 0.68f
        const val CREATE_SHEET_HEIGHT_RATIO = 0.92f
        const val MAX_VISIBLE_FILTER_OPTIONS = 6
        const val SEARCH_ANIMATION_DURATION_MS = 180L
        const val FAB_MENU_ANIMATION_MS = 180L
        const val FAB_ACTION_TRANSLATION_Y = 28f
        const val PURCHASE_ORDER_TYPE = 1
        const val CUSTOMER_PROVIDED_TYPE = 2
        const val ENABLED_ALPHA = 1f
        const val DISABLED_ALPHA = 0.45f
        const val APPLIED_SEARCH_ID = "search"
        const val APPLIED_WAREHOUSE_ID = "warehouse"
        const val APPLIED_PURCHASE_ORDER_ID = "purchase_order"
        const val APPLIED_TYPE_ID = "type"
        const val APPLIED_STATUS_ID = "status"
        const val APPLIED_CLEAR_ALL_ID = "clear_all"
    }
}

private data class ReceivingDropdownOption(
    val id: String,
    val label: String
)

private data class ReceivingStaticFilterOption(
    val value: Int,
    val label: String
)

private data class CreateReceiptDraft(
    val warehouseId: String? = null,
    val warehouseLabel: String? = null,
    val receiptType: Int? = null,
    val receiptTypeLabel: String? = null,
    val purchaseOrderId: String? = null,
    val purchaseOrderLabel: String? = null
)

private data class ScannedReceiptUi(
    val scannedValue: String,
    val warehouse: String,
    val receiptType: String,
    val purchaseOrder: String,
    val expectedDate: String,
    val notes: String,
    val itemProduct: String,
    val itemQuantity: String
) {
    companion object {
        fun dummy(scannedValue: String): ScannedReceiptUi {
            return ScannedReceiptUi(
                scannedValue = scannedValue.ifBlank { "WR-QR-DUMMY" },
                warehouse = "LA",
                receiptType = "Purchase Order",
                purchaseOrder = "PO-00000028",
                expectedDate = "09/10/2026",
                notes = "Auto-filled from scanned receipt QR.",
                itemProduct = "#6 WALL BRACKET WHITE",
                itemQuantity = "1"
            )
        }
    }
}

private data class AppliedReceivingFilterChipUi(
    val id: String,
    val label: String,
    val isClearAll: Boolean = false
)

private class AppliedReceivingFilterChipAdapter(
    private val onChipAction: (AppliedReceivingFilterChipUi) -> Unit
) : RecyclerView.Adapter<AppliedReceivingFilterChipAdapter.ChipViewHolder>() {

    private val items = mutableListOf<AppliedReceivingFilterChipUi>()

    fun submitItems(newItems: List<AppliedReceivingFilterChipUi>) {
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
            item: AppliedReceivingFilterChipUi,
            onChipAction: (AppliedReceivingFilterChipUi) -> Unit
        ) {
            binding.appliedFilterText.text = item.label
            binding.removeFilterIcon.isVisible = !item.isClearAll
            binding.root.setOnClickListener { onChipAction(item) }
            binding.removeFilterIcon.setOnClickListener { onChipAction(item) }
        }
    }
}
