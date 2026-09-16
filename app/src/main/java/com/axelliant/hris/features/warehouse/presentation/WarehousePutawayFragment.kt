package com.axelliant.hris.features.warehouse.presentation

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
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
import com.axelliant.hris.core.ui.ShimmerAnimatorHelper
import com.axelliant.hris.core.ui.UiState
import com.axelliant.hris.databinding.FragmentWarehousePutawayBinding
import com.axelliant.hris.databinding.ItemAppliedProductFilterChipBinding
import com.axelliant.hris.databinding.LayoutWarehouseInventorySelectFieldBinding
import com.axelliant.hris.databinding.LayoutWarehousePutawayFilterSheetBinding
import com.axelliant.hris.features.internalapps.navigation.InternalAppsNavigator
import com.axelliant.hris.features.warehouse.domain.model.WarehouseLocationOptionModel
import com.axelliant.hris.features.warehouse.domain.model.WarehouseOptionModel
import com.axelliant.hris.ui.designsystem.components.createAppBottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetBehavior
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class WarehousePutawayFragment : Fragment() {

    private val viewModel: WarehousePutawayViewModel by viewModels()
    private var _binding: FragmentWarehousePutawayBinding? = null
    private val binding get() = _binding!!
    private val adapter = WarehousePutawayAdapter()
    private val shimmerHelper = ShimmerAnimatorHelper()
    private val appliedFiltersAdapter = AppliedPutawayFilterChipAdapter(::handleAppliedFilterAction)
    private var appliedFilters = WarehousePutawayFilters()
    private var draftFilters = WarehousePutawayFilters()
    private var activeFilterPopup: PopupWindow? = null
    private var activeSheetBinding: LayoutWarehousePutawayFilterSheetBinding? = null
    private var activeDropdown: PutawayDropdown? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWarehousePutawayBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.appTopBar.actionButton.contentDescription = getString(R.string.filters)
        setupAppliedFilterChips()
        setupList()
        setupInteractions()
        observePutaways()
        observeFilterOptions()
        renderAppliedFilterChips()
        viewModel.loadPutawaysIfNeeded()
    }

    private fun setupAppliedFilterChips() {
        binding.appliedFiltersRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL, false)
            adapter = appliedFiltersAdapter
            itemAnimator = null
        }
    }

    private fun setupList() {
        binding.putawayRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@WarehousePutawayFragment.adapter
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
        binding.appTopBar.setOnActionClickListener {
            showFilterSheet()
        }
    }

    private fun showFilterSheet() {
        draftFilters = appliedFilters
        val sheetBinding = LayoutWarehousePutawayFilterSheetBinding.inflate(layoutInflater)
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
            draftFilters = WarehousePutawayFilters()
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

    private fun bindFilterSheetFields(sheetBinding: LayoutWarehousePutawayFilterSheetBinding) {
        bindField(
            binding = sheetBinding.warehouseFieldInclude,
            text = draftFilters.warehouseLabel,
            placeholder = getString(R.string.putaway_filter_warehouse)
        ) { showWarehouseDropdown() }
        bindField(
            binding = sheetBinding.locationFieldInclude,
            text = draftFilters.locationLabel,
            placeholder = getString(R.string.putaway_filter_location)
        ) { showLocationDropdown() }
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
        activeDropdown = PutawayDropdown.Warehouse
        if (showWarehouseDropdownIfReady()) return
        showLoadingDropdown(sheetBinding.warehouseFieldInclude.selectField)
        viewModel.loadWarehouseOptionsIfNeeded()
    }

    private fun showLocationDropdown() {
        val sheetBinding = activeSheetBinding ?: return
        val warehouseId = draftFilters.warehouseId
        if (warehouseId.isNullOrBlank()) {
            showFilterError(getString(R.string.putaway_select_warehouse_first))
            return
        }
        activeDropdown = PutawayDropdown.Location
        if (showLocationDropdownIfReady()) return
        showLoadingDropdown(sheetBinding.locationFieldInclude.selectField)
        viewModel.loadLocationOptionsForWarehouse(warehouseId)
    }

    private fun showWarehouseDropdownIfReady(): Boolean {
        val sheetBinding = activeSheetBinding ?: return false
        val state = viewModel.warehouseOptionsState.value as? UiState.Success ?: return false
        if (activeDropdown != PutawayDropdown.Warehouse) return true
        showOptionDropdown(
            anchor = sheetBinding.warehouseFieldInclude.selectField,
            allLabel = getString(R.string.putaway_all_warehouses),
            options = state.data.map { option ->
                PutawayDropdownOption(option.id, option.selectorLabel())
            }
        ) { selected ->
            val warehouseChanged = selected?.id != draftFilters.warehouseId
            draftFilters = draftFilters.copy(
                warehouseId = selected?.id,
                warehouseLabel = selected?.label,
                locationId = if (warehouseChanged) null else draftFilters.locationId,
                locationLabel = if (warehouseChanged) null else draftFilters.locationLabel
            )
            bindFilterSheetFields(sheetBinding)
        }
        return true
    }

    private fun showLocationDropdownIfReady(): Boolean {
        val sheetBinding = activeSheetBinding ?: return false
        val state = viewModel.locationOptionsState.value as? UiState.Success ?: return false
        if (activeDropdown != PutawayDropdown.Location) return true
        showOptionDropdown(
            anchor = sheetBinding.locationFieldInclude.selectField,
            allLabel = getString(R.string.putaway_all_locations),
            options = state.data.map { option ->
                PutawayDropdownOption(option.id, option.selectorLabel())
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

    private fun showLoadingDropdown(anchor: View) {
        showOptionDropdown(
            anchor = anchor,
            allLabel = null,
            options = listOf(PutawayDropdownOption(LOADING_ID, getString(R.string.loading))),
            onSelected = {}
        )
    }

    private fun showOptionDropdown(
        anchor: View,
        allLabel: String?,
        options: List<PutawayDropdownOption>,
        onSelected: (PutawayDropdownOption?) -> Unit
    ) {
        activeFilterPopup?.dismiss()
        anchor.setBackgroundResource(R.drawable.bg_filter_field_focused)
        val rows = buildList {
            allLabel?.let { add(PutawayDropdownOption(ALL_FILTER_ID, it)) }
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
        option: PutawayDropdownOption,
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

    private fun observePutaways() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.putawayState.collect { state ->
                    when (state) {
                        UiState.Loading -> showLoading()
                        is UiState.Success -> showSuccess(state.data)
                        UiState.Empty -> showEmpty()
                        is UiState.Error -> showError(state.message)
                        UiState.Unauthorized -> showError(getString(R.string.putaway_error))
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
                            UiState.Unauthorized -> showFilterError(getString(R.string.putaway_filter_error))
                            else -> Unit
                        }
                    }
                }
                launch {
                    viewModel.locationOptionsState.collect { state ->
                        when (state) {
                            is UiState.Success -> showLocationDropdownIfReady()
                            is UiState.Error -> {
                                val text = if (state.message == WarehousePutawayViewModel.LOCATION_REQUIRES_WAREHOUSE) {
                                    getString(R.string.putaway_select_warehouse_first)
                                } else {
                                    state.message
                                }
                                showFilterError(text)
                            }
                            UiState.Unauthorized -> showFilterError(getString(R.string.putaway_filter_error))
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
            message.ifBlank { getString(R.string.putaway_filter_error) },
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun showLoading() {
        showPaginationShimmer(false)
        binding.putawayRecyclerView.isVisible = false
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

    private fun showSuccess(data: WarehousePutawayUiModel) {
        shimmerHelper.clear(binding.shimmerContainer)
        binding.shimmerContainer.isVisible = false
        binding.errorStateText.isVisible = false
        appliedFilters = data.filters
        renderAppliedFilterChips()
        binding.totalEntriesText.text = resources.getQuantityString(
            R.plurals.putaway_count,
            data.totalCount,
            data.totalCount
        )
        val isEmpty = data.putaways.isEmpty()
        binding.emptyStateText.isVisible = isEmpty
        binding.putawayRecyclerView.isVisible = !isEmpty
        adapter.submitList(data.putaways)
        showPaginationShimmer(data.isLoadingNextPage)
    }

    private fun showEmpty() {
        shimmerHelper.clear(binding.shimmerContainer)
        binding.shimmerContainer.isVisible = false
        showPaginationShimmer(false)
        binding.putawayRecyclerView.isVisible = false
        binding.errorStateText.isVisible = false
        binding.emptyStateText.isVisible = true
        binding.totalEntriesText.text = getString(R.string.putaway_empty)
        adapter.submitList(emptyList())
    }

    private fun showError(message: String) {
        shimmerHelper.clear(binding.shimmerContainer)
        binding.shimmerContainer.isVisible = false
        showPaginationShimmer(false)
        binding.putawayRecyclerView.isVisible = false
        binding.emptyStateText.isVisible = false
        binding.errorStateText.isVisible = true
        binding.errorStateText.text = message.ifBlank { getString(R.string.putaway_error) }
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

    private fun buildAppliedFilterChips(): List<AppliedPutawayFilterChipUi> {
        val chips = mutableListOf<AppliedPutawayFilterChipUi>()
        appliedFilters.warehouseLabel?.let {
            chips.add(AppliedPutawayFilterChipUi(APPLIED_WAREHOUSE_ID, it))
        }
        appliedFilters.locationLabel?.let {
            chips.add(AppliedPutawayFilterChipUi(APPLIED_LOCATION_ID, it))
        }
        if (chips.isNotEmpty()) {
            chips.add(
                AppliedPutawayFilterChipUi(
                    id = APPLIED_CLEAR_ALL_ID,
                    label = getString(R.string.clear_all),
                    isClearAll = true
                )
            )
        }
        return chips
    }

    private fun handleAppliedFilterAction(chip: AppliedPutawayFilterChipUi) {
        appliedFilters = when (chip.id) {
            APPLIED_CLEAR_ALL_ID -> WarehousePutawayFilters()
            APPLIED_WAREHOUSE_ID -> WarehousePutawayFilters()
            APPLIED_LOCATION_ID -> appliedFilters.copy(locationId = null, locationLabel = null)
            else -> appliedFilters
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
        return path.takeIf { it.isNotBlank() && it != "-" }
            ?: name.takeIf { it.isNotBlank() && it != "-" }
            ?: getString(R.string.warehouse_not_available)
    }

    override fun onDestroyView() {
        activeFilterPopup?.dismiss()
        shimmerHelper.release()
        super.onDestroyView()
        _binding = null
    }

    private enum class PutawayDropdown {
        Warehouse,
        Location
    }

    private companion object {
        const val ALL_FILTER_ID = "all"
        const val LOADING_ID = "loading"
        const val PAGINATION_THRESHOLD_ITEMS = 2
        const val SHIMMER_ITEM_COUNT = 5
        const val FILTER_SHEET_HEIGHT_RATIO = 0.48f
        const val MAX_VISIBLE_FILTER_OPTIONS = 6
        const val APPLIED_WAREHOUSE_ID = "warehouse"
        const val APPLIED_LOCATION_ID = "location"
        const val APPLIED_CLEAR_ALL_ID = "clear_all"
    }
}

private data class PutawayDropdownOption(
    val id: String,
    val label: String
)

private data class AppliedPutawayFilterChipUi(
    val id: String,
    val label: String,
    val isClearAll: Boolean = false
)

private class AppliedPutawayFilterChipAdapter(
    private val onChipClick: (AppliedPutawayFilterChipUi) -> Unit
) : RecyclerView.Adapter<AppliedPutawayFilterChipAdapter.FilterChipViewHolder>() {

    private val items = mutableListOf<AppliedPutawayFilterChipUi>()

    fun submitItems(newItems: List<AppliedPutawayFilterChipUi>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FilterChipViewHolder {
        val binding = ItemAppliedProductFilterChipBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return FilterChipViewHolder(binding, onChipClick)
    }

    override fun onBindViewHolder(holder: FilterChipViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class FilterChipViewHolder(
        private val binding: ItemAppliedProductFilterChipBinding,
        private val onChipClick: (AppliedPutawayFilterChipUi) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: AppliedPutawayFilterChipUi) = with(binding) {
            appliedFilterText.text = item.label
            removeFilterIcon.isVisible = !item.isClearAll
            root.setOnClickListener { onChipClick(item) }
            removeFilterIcon.setOnClickListener { onChipClick(item) }
        }
    }
}
