package com.axelliant.hris.features.inventory.products.presentation

import android.graphics.Color
import android.content.res.ColorStateList
import android.graphics.drawable.ColorDrawable
import android.text.Editable
import android.text.InputType
import android.text.TextUtils
import android.text.TextWatcher
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.res.use
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.axelliant.hris.R
import com.axelliant.hris.ui.designsystem.components.AppCheckboxView
import com.axelliant.hris.ui.designsystem.components.AppTextView
import com.axelliant.hris.ui.designsystem.components.createAppBottomSheetDialog
import com.axelliant.hris.core.extensions.enableClearTextButton
import com.axelliant.hris.core.ui.UiState
import com.axelliant.hris.databinding.LayoutProductFilterSheetBinding
import com.axelliant.hris.features.inventory.products.ai.ProductAiStatus
import com.axelliant.hris.features.inventory.products.presentation.dynamicfilters.SelectedCategorySpecs
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.axelliant.hris.ui.designsystem.components.AppButtonView
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class ProductFilterSheetController(
    private val fragment: Fragment,
    private val lifecycleOwner: LifecycleOwner,
    private val viewModel: ProductsViewModel,
    initialState: ProductFilterSheetState,
    private val showSearchAndStatus: Boolean = false,
    private val onCategorySpecsClick: ((ProductFilterSheetState) -> Unit)? = null,
    private val onCloseWithoutApply: (() -> Unit)? = null,
    private val onApply: (ProductFilterSheetState) -> Unit
) {
    private val context get() = fragment.requireContext()
    private val resources get() = fragment.resources
    private val filterState = initialState.copyForEditing()
    private var activeFilterPopup: PopupWindow? = null
    private var selectedStatus = filterState.status ?: ProductAiStatus.All

    fun show() {
        val sheetBinding = LayoutProductFilterSheetBinding.inflate(fragment.layoutInflater)
        val dialog = context.createAppBottomSheetDialog().apply {
            setContentView(sheetBinding.root)
            setCancelable(false)
            setCanceledOnTouchOutside(false)
        }
        val manufacturerField = sheetBinding.root.findViewById<LinearLayout>(R.id.manufacturerField)
        val vendorField = sheetBinding.root.findViewById<LinearLayout>(R.id.vendorField)
        val manufacturerFieldText = sheetBinding.root.findViewById<AppTextView>(R.id.manufacturerFieldText)
        val vendorFieldText = sheetBinding.root.findViewById<AppTextView>(R.id.vendorFieldText)
        val attributeTagFieldText = sheetBinding.root.findViewById<AppTextView>(R.id.attributeTagFieldText)

        sheetBinding.filterSearchEditText.enableClearTextButton()
        sheetBinding.minPriceEditText.enableClearTextButton()
        sheetBinding.maxPriceEditText.enableClearTextButton()
        bindSearchAndStatusSection(sheetBinding)
        bindCategorySpecsSection(sheetBinding)
        bindFilterSheetValues(sheetBinding, manufacturerFieldText, vendorFieldText, attributeTagFieldText)
        val lookupJobs = bindFilterLookupObservers(
            sheetBinding,
            manufacturerFieldText,
            vendorFieldText,
            attributeTagFieldText
        )
        viewModel.loadCommonProductFilters()

        sheetBinding.root.setOnClickListener { activeFilterPopup?.dismiss() }
        sheetBinding.closeFilterButton.setOnClickListener {
            activeFilterPopup?.dismiss()
            onCloseWithoutApply?.invoke()
            dialog.dismiss()
        }
        sheetBinding.resetFiltersButton.setOnClickListener {
            resetFilterSheet(sheetBinding, manufacturerFieldText, vendorFieldText, attributeTagFieldText)
        }
        sheetBinding.applyFiltersButton.setOnClickListener {
            if (capturePriceFields(sheetBinding)) {
                onApply(filterState.copyForEditing())
                dialog.dismiss()
            }
        }
        sheetBinding.categorySpecsCard.setOnClickListener {
            if (onCategorySpecsClick == null) return@setOnClickListener
            activeFilterPopup?.dismiss()
            if (capturePriceFields(sheetBinding)) {
                onCategorySpecsClick.invoke(filterState.copyForEditing())
                dialog.dismiss()
            }
        }
        manufacturerField.setOnClickListener {
            showFilterDropdownPopup(
                anchor = manufacturerField,
                lookupState = viewModel.manufacturerFiltersState.value,
                selectedOptions = filterState.manufacturers,
                fieldText = manufacturerFieldText,
                chipsContainer = sheetBinding.manufacturerChipsContainer,
                placeholder = fragment.getString(R.string.select_manufacturer),
                searchHint = fragment.getString(R.string.search_manufacturers),
                allowMultipleSelection = false,
                onLoad = viewModel::loadCommonProductFilters
            )
        }
        vendorField.setOnClickListener {
            showFilterDropdownPopup(
                anchor = vendorField,
                lookupState = viewModel.vendorFiltersState.value,
                selectedOptions = filterState.vendors,
                fieldText = vendorFieldText,
                chipsContainer = sheetBinding.vendorChipsContainer,
                placeholder = fragment.getString(R.string.select_vendor),
                searchHint = fragment.getString(R.string.search_vendors),
                allowMultipleSelection = false,
                onLoad = viewModel::loadCommonProductFilters
            )
        }
        sheetBinding.attributeTagField.setOnClickListener {
            showFilterDropdownPopup(
                anchor = sheetBinding.attributeTagField,
                lookupState = viewModel.attributeTagFiltersState.value,
                selectedOptions = filterState.attributeTags,
                fieldText = attributeTagFieldText,
                chipsContainer = sheetBinding.attributeTagChipsContainer,
                placeholder = fragment.getString(R.string.select_tags),
                searchHint = fragment.getString(R.string.search_tags),
                allowMultipleSelection = true,
                onLoad = viewModel::loadCommonProductFilters
            )
        }
        dialog.setOnDismissListener {
            activeFilterPopup?.dismiss()
            lookupJobs.forEach { it.cancel() }
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

    private fun bindSearchAndStatusSection(sheetBinding: LayoutProductFilterSheetBinding) {
        sheetBinding.askAiSearchStatusSection.isVisible = true
        sheetBinding.filterSearchEditText.setText(filterState.searchText)
        sheetBinding.filterStatusLabel.isVisible = showSearchAndStatus
        sheetBinding.filterStatusChoiceContainer.isVisible = showSearchAndStatus
        if (showSearchAndStatus) renderStatusChoices(sheetBinding)
    }

    private fun bindCategorySpecsSection(sheetBinding: LayoutProductFilterSheetBinding) {
        sheetBinding.categorySpecsSection.isVisible =
            onCategorySpecsClick != null || filterState.dynamicCategorySpecs.hasCategory
        sheetBinding.categorySpecsCard.isClickable = onCategorySpecsClick != null
        sheetBinding.categorySpecsCard.isFocusable = onCategorySpecsClick != null
        renderCategorySpecsSummary(sheetBinding)
    }

    private fun renderCategorySpecsSummary(sheetBinding: LayoutProductFilterSheetBinding) {
        val specs = filterState.dynamicCategorySpecs
        val categoryName = specs.categoryName.orEmpty()
        sheetBinding.categorySpecsTitleText.text = categoryName.ifBlank {
            fragment.getString(R.string.dynamic_category_select_category)
        }
        sheetBinding.categorySpecsTitleText.setTextColor(
            ContextCompat.getColor(
                context,
                if (categoryName.isBlank()) R.color.ds_text_primary else R.color.ds_primary
            )
        )
        sheetBinding.categorySpecsSubtitleText.text = when {
            categoryName.isBlank() -> fragment.getString(R.string.dynamic_category_none_selected)
            specs.hasSelectedOptions -> specs.selectedOptions.toCategorySpecsSummary()
            else -> fragment.getString(R.string.dynamic_category_no_specs_selected)
        }
        sheetBinding.categorySpecsCountText.isVisible = categoryName.isNotBlank() && !specs.hasSelectedOptions
        sheetBinding.categorySpecsCountText.text = fragment.getString(R.string.dynamic_category_no_specs_selected)
    }

    private fun renderStatusChoices(sheetBinding: LayoutProductFilterSheetBinding) {
        sheetBinding.filterStatusChoiceContainer.removeAllViews()
        listOf(ProductAiStatus.All, ProductAiStatus.Active, ProductAiStatus.Inactive).forEach { status ->
            sheetBinding.filterStatusChoiceContainer.addView(
                createStatusChoiceButton(status) {
                    selectedStatus = status
                    renderStatusChoices(sheetBinding)
                }
            )
        }
    }

    private fun createStatusChoiceButton(
        status: ProductAiStatus,
        onClick: () -> Unit
    ): AppButtonView {
        val selected = selectedStatus == status
        return AppButtonView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                0,
                resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._36sdp),
                1f
            ).apply {
                marginEnd = resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._6sdp)
            }
            text = status.displayName
            isAllCaps = false
            minWidth = 0
            insetTop = 0
            insetBottom = 0
            cornerRadius = resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._10sdp)
            strokeWidth = resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._1sdp)
            strokeColor = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.ds_primary))
            backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(context, if (selected) R.color.ds_primary else R.color.ds_surface)
            )
            setTextColor(ContextCompat.getColor(context, if (selected) R.color.ds_on_primary else R.color.ds_primary))
            setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(com.intuit.ssp.R.dimen._10ssp))
            setOnClickListener { onClick() }
        }
    }

    private fun bindFilterLookupObservers(
        sheetBinding: LayoutProductFilterSheetBinding,
        manufacturerFieldText: AppTextView?,
        vendorFieldText: AppTextView?,
        attributeTagFieldText: AppTextView?
    ): List<Job> {
        return listOf(
            lifecycleOwner.lifecycleScope.launch {
                viewModel.manufacturerFiltersState.collect { state ->
                    if (state is UiState.Success) filterState.manufacturerOptions = state.data
                    refreshFilterSelectionUi(
                        fieldText = manufacturerFieldText,
                        chipsContainer = sheetBinding.manufacturerChipsContainer,
                        selectedOptions = filterState.manufacturers,
                        placeholder = fragment.getString(R.string.select_manufacturer),
                        showSelectedChips = false
                    )
                }
            },
            lifecycleOwner.lifecycleScope.launch {
                viewModel.vendorFiltersState.collect { state ->
                    if (state is UiState.Success) filterState.vendorOptions = state.data
                    refreshFilterSelectionUi(
                        fieldText = vendorFieldText,
                        chipsContainer = sheetBinding.vendorChipsContainer,
                        selectedOptions = filterState.vendors,
                        placeholder = fragment.getString(R.string.select_vendor),
                        showSelectedChips = false
                    )
                }
            },
            lifecycleOwner.lifecycleScope.launch {
                viewModel.attributeTagFiltersState.collect { state ->
                    if (state is UiState.Success) filterState.attributeTagOptions = state.data
                    refreshFilterSelectionUi(
                        fieldText = attributeTagFieldText,
                        chipsContainer = sheetBinding.attributeTagChipsContainer,
                        selectedOptions = filterState.attributeTags,
                        placeholder = fragment.getString(R.string.select_tags)
                    )
                }
            }
        )
    }

    private fun showFilterDropdownPopup(
        anchor: View,
        lookupState: UiState<List<FilterOptionUi>>,
        selectedOptions: LinkedHashMap<String, String>,
        fieldText: AppTextView?,
        chipsContainer: RecyclerView,
        placeholder: String,
        searchHint: String,
        onLoad: () -> Unit,
        allowMultipleSelection: Boolean = true,
        selectedCountText: AppTextView? = null
    ) {
        activeFilterPopup?.dismiss()
        val stateForPopup = lookupState.takeUnless { it is UiState.Idle } ?: UiState.Loading
        if (lookupState !is UiState.Success) onLoad()

        val dropdownContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.bg_filter_dropdown_panel)
            setPadding(
                resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._8sdp),
                resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._8sdp),
                resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._8sdp),
                resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._8sdp)
            )
        }
        val searchInput = EditText(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._38sdp)
            )
            background = ContextCompat.getDrawable(context, R.drawable.bg_filter_field)
            hint = searchHint
            inputType = InputType.TYPE_CLASS_TEXT
            isSingleLine = true
            setPadding(
                resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._10sdp),
                0,
                resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._10sdp),
                0
            )
            setTextColor(ContextCompat.getColor(context, R.color.ds_text_primary))
            setHintTextColor(ContextCompat.getColor(context, R.color.ds_text_muted))
            setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(com.intuit.ssp.R.dimen._12ssp))
            enableClearTextButton()
        }
        val optionsContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
        }
        val optionsScrollView = ScrollView(context).apply {
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

        fun refreshPopupSelectionUi() {
            updateFilterSelectionUi(
                fieldText = fieldText,
                chipsContainer = chipsContainer,
                selectedOptions = selectedOptions,
                placeholder = placeholder,
                showSelectedChips = allowMultipleSelection
            ) { removedId ->
                selectedOptions.remove(removedId)
                selectedCountText?.text = selectedCountText(selectedOptions.size, stateForPopup)
                refreshPopupSelectionUi()
            }
        }

        fun renderOptions(query: String) {
            optionsContainer.removeAllViews()
            when (stateForPopup) {
                UiState.Loading,
                UiState.Idle -> {
                    updateFilterOptionsViewport(optionsScrollView, 1)
                    optionsContainer.addView(createFilterMessageRow(fragment.getString(R.string.loading)))
                }

                is UiState.Success -> {
                    val filteredOptions = stateForPopup.data.filter {
                        query.isBlank() || it.name.contains(query, ignoreCase = true)
                    }
                    updateFilterOptionsViewport(optionsScrollView, filteredOptions.size.coerceAtLeast(1))
                    selectedCountText?.text = selectedCountText(selectedOptions.size, stateForPopup)
                    if (filteredOptions.isEmpty()) {
                        optionsContainer.addView(createFilterMessageRow(fragment.getString(R.string.empty_state)))
                        return
                    }
                    filteredOptions.forEach { option ->
                        optionsContainer.addView(
                            createFilterOptionRow(
                                option = option,
                                selected = selectedOptions.containsKey(option.id),
                                showCheckbox = allowMultipleSelection
                            ) {
                                if (selectedOptions.containsKey(option.id)) {
                                    selectedOptions.remove(option.id)
                                } else {
                                    if (!allowMultipleSelection) selectedOptions.clear()
                                    selectedOptions[option.id] = option.name
                                }
                                refreshPopupSelectionUi()
                                renderOptions(searchInput.text?.toString().orEmpty())
                            }
                        )
                    }
                }

                UiState.Empty -> {
                    updateFilterOptionsViewport(optionsScrollView, 1)
                    optionsContainer.addView(createFilterMessageRow(fragment.getString(R.string.empty_state)))
                }

                is UiState.Error -> {
                    updateFilterOptionsViewport(optionsScrollView, 1)
                    optionsContainer.addView(createFilterMessageRow(stateForPopup.message))
                }

                UiState.Unauthorized -> {
                    updateFilterOptionsViewport(optionsScrollView, 1)
                    optionsContainer.addView(createFilterMessageRow(fragment.getString(R.string.ia_login_microsoft_unavailable)))
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

    private fun selectedCountText(
        selectedCount: Int,
        state: UiState<List<FilterOptionUi>>
    ): String {
        val total = (state as? UiState.Success)?.data?.size ?: 0
        return fragment.getString(R.string.filter_selected_count_dynamic, selectedCount, total)
    }

    private fun createFilterOptionRow(
        option: FilterOptionUi,
        selected: Boolean,
        showCheckbox: Boolean,
        onToggle: () -> Unit
    ): View {
        return LinearLayout(context).apply {
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
            if (showCheckbox) {
                addView(
                    AppCheckboxView(context).apply {
                        layoutParams = LinearLayout.LayoutParams(
                            resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._26sdp),
                            resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._26sdp)
                        )
                        isChecked = selected
                        buttonTintList = ContextCompat.getColorStateList(context, R.color.ds_outline_strong)
                        setOnClickListener { onToggle() }
                    }
                )
            }
            addView(
                TextView(context).apply {
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                        marginStart = if (showCheckbox) {
                            resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._8sdp)
                        } else {
                            0
                        }
                    }
                    text = option.name
                    setTextColor(ContextCompat.getColor(context, R.color.ds_text_primary))
                    setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(com.intuit.ssp.R.dimen._12ssp))
                    maxLines = 1
                    ellipsize = TextUtils.TruncateAt.END
                }
            )
            if (selected) {
                addView(
                    ImageView(context).apply {
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
        fieldText: AppTextView?,
        chipsContainer: RecyclerView,
        selectedOptions: LinkedHashMap<String, String>,
        placeholder: String,
        showSelectedChips: Boolean,
        onRemove: (String) -> Unit
    ) {
        val selectedNames = selectedOptions.values.toList()
        fieldText?.text = selectedNames.takeIf { it.isNotEmpty() }?.joinToString(", ") ?: placeholder
        fieldText?.setTextColor(
            ContextCompat.getColor(
                context,
                if (selectedNames.isEmpty()) R.color.ds_text_muted else R.color.ds_text_primary
            )
        )

        chipsContainer.isVisible = showSelectedChips && selectedOptions.isNotEmpty()
        if (!showSelectedChips) {
            chipsContainer.adapter = null
            return
        }
        if (chipsContainer.layoutManager == null) {
            chipsContainer.layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
        }
        chipsContainer.adapter = ProductFilterSelectedChipAdapter(
            selectedOptions.map { (id, name) -> ProductFilterSelectedChipUi(id, name) },
            onRemove = onRemove
        )
    }

    private fun refreshFilterSelectionUi(
        fieldText: AppTextView?,
        chipsContainer: RecyclerView,
        selectedOptions: LinkedHashMap<String, String>,
        placeholder: String,
        showSelectedChips: Boolean = true
    ) {
        updateFilterSelectionUi(
            fieldText = fieldText,
            chipsContainer = chipsContainer,
            selectedOptions = selectedOptions,
            placeholder = placeholder,
            showSelectedChips = showSelectedChips
        ) { removedId ->
            selectedOptions.remove(removedId)
            refreshFilterSelectionUi(fieldText, chipsContainer, selectedOptions, placeholder, showSelectedChips)
        }
    }

    private fun resetFilterSheet(
        sheetBinding: LayoutProductFilterSheetBinding,
        manufacturerFieldText: AppTextView?,
        vendorFieldText: AppTextView?,
        attributeTagFieldText: AppTextView?
    ) {
        filterState.categories.clear()
        filterState.manufacturers.clear()
        filterState.vendors.clear()
        filterState.attributeTags.clear()
        filterState.minListPrice = null
        filterState.maxListPrice = null
        filterState.availability = null
        filterState.dynamicCategorySpecs = SelectedCategorySpecs()
        filterState.searchText = ""
        filterState.status = ProductAiStatus.All
        selectedStatus = ProductAiStatus.All
        sheetBinding.filterSearchEditText.setText("")
        sheetBinding.minPriceEditText.setText("")
        sheetBinding.maxPriceEditText.setText("")
        if (showSearchAndStatus) {
            renderStatusChoices(sheetBinding)
        }
        bindFilterSheetValues(sheetBinding, manufacturerFieldText, vendorFieldText, attributeTagFieldText)
        renderCategorySpecsSummary(sheetBinding)
    }

    private fun bindFilterSheetValues(
        sheetBinding: LayoutProductFilterSheetBinding,
        manufacturerFieldText: AppTextView?,
        vendorFieldText: AppTextView?,
        attributeTagFieldText: AppTextView?
    ) {
        sheetBinding.filterSearchEditText.setText(filterState.searchText)
        sheetBinding.minPriceEditText.setText(filterState.minListPrice?.toString().orEmpty())
        sheetBinding.maxPriceEditText.setText(filterState.maxListPrice?.toString().orEmpty())
        refreshFilterSelectionUi(
            fieldText = manufacturerFieldText,
            chipsContainer = sheetBinding.manufacturerChipsContainer,
            selectedOptions = filterState.manufacturers,
            placeholder = fragment.getString(R.string.select_manufacturer),
            showSelectedChips = false
        )
        refreshFilterSelectionUi(
            fieldText = vendorFieldText,
            chipsContainer = sheetBinding.vendorChipsContainer,
            selectedOptions = filterState.vendors,
            placeholder = fragment.getString(R.string.select_vendor),
            showSelectedChips = false
        )
        refreshFilterSelectionUi(
            fieldText = attributeTagFieldText,
            chipsContainer = sheetBinding.attributeTagChipsContainer,
            selectedOptions = filterState.attributeTags,
            placeholder = fragment.getString(R.string.select_tags)
        )
        renderCategorySpecsSummary(sheetBinding)
    }

    private fun Map<String, List<String>>.toCategorySpecsSummary(): String {
        return entries.joinToString(separator = "; ") { (filterKey, values) ->
            "${filterKey.toDisplayLabel()}: ${values.joinToString()}"
        }
    }

    private fun String.toDisplayLabel(): String {
        return replace(Regex("([a-z])([A-Z])"), "$1 $2")
            .replace("_", " ")
            .replace("-", " ")
            .trim()
            .ifBlank { this }
    }

    private fun capturePriceFields(sheetBinding: LayoutProductFilterSheetBinding): Boolean {
        val minPrice = sheetBinding.minPriceEditText.text?.toString()?.trim()?.takeIf { it.isNotEmpty() }?.toDoubleOrNull()
        val maxPrice = sheetBinding.maxPriceEditText.text?.toString()?.trim()?.takeIf { it.isNotEmpty() }?.toDoubleOrNull()
        if (minPrice != null && maxPrice != null && maxPrice < minPrice) {
            Toast.makeText(context, R.string.invalid_price_range, Toast.LENGTH_SHORT).show()
            return false
        }

        filterState.minListPrice = minPrice
        filterState.maxListPrice = maxPrice
        filterState.searchText = sheetBinding.filterSearchEditText.text?.toString().orEmpty().trim()
        if (showSearchAndStatus) filterState.status = selectedStatus
        return true
    }

    private fun createFilterMessageRow(message: String): View {
        return TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._36sdp)
            )
            gravity = android.view.Gravity.CENTER_VERTICAL
            text = message
            setTextColor(ContextCompat.getColor(context, R.color.ds_text_muted))
            setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(com.intuit.ssp.R.dimen._11ssp))
        }
    }

    private fun updateFilterOptionsViewport(optionsScrollView: ScrollView, optionCount: Int) {
        val visibleRows = optionCount.coerceIn(1, MAX_VISIBLE_FILTER_OPTIONS)
        val rowHeight = resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._34sdp)
        optionsScrollView.layoutParams = optionsScrollView.layoutParams.apply {
            height = rowHeight * visibleRows
        }
        optionsScrollView.isVerticalScrollBarEnabled = optionCount > MAX_VISIBLE_FILTER_OPTIONS
        optionsScrollView.requestLayout()
    }

    private companion object {
        const val FILTER_SHEET_HEIGHT_RATIO = 0.78f
        const val MAX_VISIBLE_FILTER_OPTIONS = 4
    }
}

private data class ProductFilterSelectedChipUi(
    val id: String,
    val label: String
)

private class ProductFilterSelectedChipAdapter(
    private val items: List<ProductFilterSelectedChipUi>,
    private val onRemove: (String) -> Unit
) : RecyclerView.Adapter<ProductFilterSelectedChipAdapter.ChipViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChipViewHolder {
        val resources = parent.resources
        val container = LinearLayout(parent.context).apply {
            layoutParams = RecyclerView.LayoutParams(
                RecyclerView.LayoutParams.WRAP_CONTENT,
                resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._30sdp)
            ).apply {
                marginEnd = resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._6sdp)
            }
            setBackgroundResource(R.drawable.bg_filter_chip)
            gravity = android.view.Gravity.CENTER_VERTICAL
            orientation = LinearLayout.HORIZONTAL
            setPadding(
                resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._8sdp),
                0,
                resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._7sdp),
                0
            )
        }
        val label = TextView(parent.context).apply {
            layoutParams = LinearLayout.LayoutParams(
                resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._86sdp),
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            ellipsize = TextUtils.TruncateAt.END
            maxLines = 1
            setTextColor(ContextCompat.getColor(parent.context, R.color.ds_primary))
            setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(com.intuit.ssp.R.dimen._10ssp))
        }
        val removeIcon = ImageView(parent.context).apply {
            val iconSize = resources.getDimensionPixelSize(R.dimen.ds_filter_chip_close_icon_size)
            layoutParams = LinearLayout.LayoutParams(
                iconSize,
                iconSize
            ).apply {
                marginStart = resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._5sdp)
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




