package com.axelliant.hris.features.inventory.products.presentation.dynamicfilters

import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextUtils
import android.text.TextWatcher
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.axelliant.hris.R
import com.axelliant.hris.core.extensions.enableClearTextButton
import com.axelliant.hris.ui.designsystem.components.AppButtonView
import com.axelliant.hris.ui.designsystem.components.AppTextFieldView
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint
class DynamicCategorySpecsFragment : Fragment() {
    @Inject
    lateinit var categoryFilterRepository: StaticCategoryFilterRepository

    private lateinit var titleText: TextView
    private lateinit var filterTabsRecyclerView: RecyclerView
    private lateinit var optionSearchEditText: AppTextFieldView
    private lateinit var filterHeadingText: TextView
    private lateinit var optionsRecyclerView: RecyclerView
    private lateinit var emptyText: TextView
    private lateinit var selectedChipsRecyclerView: RecyclerView
    private lateinit var tabsAdapter: DynamicSpecTabAdapter
    private lateinit var optionsAdapter: DynamicSpecOptionAdapter
    private lateinit var selectedChipsAdapter: SelectedSpecChipAdapter

    private var categoryName = ""
    private var category: DynamicCategoryFilter? = null
    private var selectedOptionsByFilter = linkedMapOf<String, MutableList<String>>()
    private var selectedFilterKey: String? = null
    private var optionSearchQuery = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        categoryName = arguments?.getString(DynamicCategorySpecsResult.ARG_CATEGORY_NAME).orEmpty()
        selectedOptionsByFilter = DynamicCategorySpecsResult
            .fromJson(arguments?.getString(DynamicCategorySpecsResult.ARG_SELECTED_SPECS_JSON))
            .selectedOptions
            .mapValues { (_, values) -> values.toMutableList() }
            .toMap(LinkedHashMap())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val root = LinearLayout(requireContext()).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.ds_surface))
            orientation = LinearLayout.VERTICAL
        }
        root.addView(createToolbar())
        root.addView(createFilterTabs())
        root.addView(createOptionSearchField())
        root.addView(createFilterHeading())

        val optionsFrame = LinearLayout(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
            orientation = LinearLayout.VERTICAL
            setPadding(
                dimen(com.intuit.sdp.R.dimen._14sdp),
                dimen(com.intuit.sdp.R.dimen._4sdp),
                dimen(com.intuit.sdp.R.dimen._14sdp),
                dimen(com.intuit.sdp.R.dimen._8sdp)
            )
        }
        optionsRecyclerView = RecyclerView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
            layoutManager = GridLayoutManager(requireContext(), OPTION_GRID_SPAN_COUNT)
            itemAnimator = null
            overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS
        }
        emptyText = createEmptyText()
        optionsAdapter = DynamicSpecOptionAdapter(onOptionClick = ::toggleOption)
        optionsRecyclerView.adapter = optionsAdapter
        optionsFrame.addView(optionsRecyclerView)
        optionsFrame.addView(emptyText)
        root.addView(optionsFrame)
        root.addView(createBottomActions())

        bindStaticContent()
        renderFilterTabs()
        renderOptions()
        renderSelectedSummary()
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launch {
            category = categoryFilterRepository.getCategories().firstOrNull { it.name == categoryName }
            selectedFilterKey = category?.filters?.firstOrNull()?.selectionKey
            bindStaticContent()
            renderFilterTabs()
            renderOptions()
            renderSelectedSummary()
        }
    }

    private fun createToolbar(): View {
        return LinearLayout(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dimen(com.intuit.sdp.R.dimen._58sdp)
            )
            gravity = Gravity.CENTER_VERTICAL
            orientation = LinearLayout.HORIZONTAL
            setPadding(dimen(com.intuit.sdp.R.dimen._8sdp), dimen(com.intuit.sdp.R.dimen._8sdp), dimen(com.intuit.sdp.R.dimen._14sdp), 0)
            addView(
                ImageButton(requireContext()).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        dimen(com.intuit.sdp.R.dimen._42sdp),
                        dimen(com.intuit.sdp.R.dimen._42sdp)
                    )
                    background = null
                    setImageResource(R.drawable.ic_arrow_back)
                    contentDescription = getString(R.string.back)
                    setOnClickListener { findNavController().navigateUp() }
                }
            )
            titleText = TextView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                setTextColor(ContextCompat.getColor(requireContext(), R.color.ds_text_primary))
                setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(com.intuit.ssp.R.dimen._15ssp))
                typeface = ResourcesCompat.getFont(requireContext(), R.font.poppins_semibold)
                maxLines = 1
                ellipsize = TextUtils.TruncateAt.END
            }
            addView(titleText)
        }
    }

    private fun createFilterTabs(): View {
        filterTabsRecyclerView = RecyclerView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dimen(com.intuit.sdp.R.dimen._46sdp)
            ).apply {
                leftMargin = dimen(com.intuit.sdp.R.dimen._14sdp)
                rightMargin = dimen(com.intuit.sdp.R.dimen._14sdp)
                topMargin = dimen(com.intuit.sdp.R.dimen._2sdp)
            }
            layoutManager = LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL, false)
            itemAnimator = null
            overScrollMode = View.OVER_SCROLL_NEVER
        }
        tabsAdapter = DynamicSpecTabAdapter(onFilterClick = { filter ->
            selectedFilterKey = filter.selectionKey
            optionSearchQuery = ""
            optionSearchEditText.setText("")
            renderFilterTabs()
            renderOptions()
        })
        filterTabsRecyclerView.adapter = tabsAdapter
        return filterTabsRecyclerView
    }

    private fun createOptionSearchField(): View {
        optionSearchEditText = AppTextFieldView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dimen(com.intuit.sdp.R.dimen._44sdp)
            ).apply {
                marginStart = dimen(com.intuit.sdp.R.dimen._14sdp)
                marginEnd = dimen(com.intuit.sdp.R.dimen._14sdp)
                topMargin = dimen(com.intuit.sdp.R.dimen._4sdp)
            }
            background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_filter_field)
            hint = getString(R.string.dynamic_specs_search_hint)
            inputType = InputType.TYPE_CLASS_TEXT
            isSingleLine = true
            setPadding(dimen(com.intuit.sdp.R.dimen._12sdp), 0, dimen(com.intuit.sdp.R.dimen._12sdp), 0)
            setTextColor(ContextCompat.getColor(requireContext(), R.color.ds_text_primary))
            setHintTextColor(ContextCompat.getColor(requireContext(), R.color.ds_text_muted))
            setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(com.intuit.ssp.R.dimen._12ssp))
            enableClearTextButton()
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(text: CharSequence?, start: Int, count: Int, after: Int) = Unit
                override fun afterTextChanged(editable: Editable?) = Unit
                override fun onTextChanged(text: CharSequence?, start: Int, before: Int, count: Int) {
                    optionSearchQuery = text?.toString().orEmpty()
                    renderOptions()
                }
            })
        }
        return optionSearchEditText
    }

    private fun createFilterHeading(): View {
        return LinearLayout(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginStart = dimen(com.intuit.sdp.R.dimen._14sdp)
                marginEnd = dimen(com.intuit.sdp.R.dimen._14sdp)
                topMargin = dimen(com.intuit.sdp.R.dimen._8sdp)
            }
            gravity = Gravity.CENTER_VERTICAL
            orientation = LinearLayout.HORIZONTAL

            filterHeadingText = TextView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                maxWidth = dimen(com.intuit.sdp.R.dimen._132sdp)
                maxLines = 1
                ellipsize = TextUtils.TruncateAt.END
                setTextColor(ContextCompat.getColor(requireContext(), R.color.ds_text_primary))
                setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(com.intuit.ssp.R.dimen._12ssp))
                typeface = ResourcesCompat.getFont(requireContext(), R.font.poppins_semibold)
            }
            selectedChipsRecyclerView = RecyclerView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    dimen(com.intuit.sdp.R.dimen._32sdp),
                    1f
                ).apply {
                    marginStart = dimen(com.intuit.sdp.R.dimen._8sdp)
                }
                layoutManager = LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL, false)
                itemAnimator = null
                overScrollMode = View.OVER_SCROLL_NEVER
            }
            selectedChipsAdapter = SelectedSpecChipAdapter(::removeSelectedOption)
            selectedChipsRecyclerView.adapter = selectedChipsAdapter
            addView(filterHeadingText)
            addView(selectedChipsRecyclerView)
        }
    }

    private fun createBottomActions(): View {
        return LinearLayout(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dimen(com.intuit.sdp.R.dimen._72sdp)
            )
            gravity = Gravity.CENTER_VERTICAL
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.ds_surface))
            setPadding(dimen(com.intuit.sdp.R.dimen._14sdp), 0, dimen(com.intuit.sdp.R.dimen._14sdp), 0)
            addView(
                AppButtonView(requireContext()).apply {
                    layoutParams = LinearLayout.LayoutParams(0, dimen(com.intuit.sdp.R.dimen._46sdp), 1f)
                    text = getString(R.string.clear)
                    isAllCaps = false
                    minWidth = 0
                    insetTop = 0
                    insetBottom = 0
                    backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.ds_surface))
                    setTextColor(ContextCompat.getColor(requireContext(), R.color.ds_primary))
                    setOnClickListener {
                        selectedOptionsByFilter.clear()
                        renderOptions()
                        renderSelectedSummary()
                    }
                }
            )
            addView(View(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(dimen(com.intuit.sdp.R.dimen._16sdp), 1)
            })
            addView(
                AppButtonView(requireContext()).apply {
                    layoutParams = LinearLayout.LayoutParams(0, dimen(com.intuit.sdp.R.dimen._46sdp), 1f)
                    text = getString(R.string.dynamic_category_apply)
                    isAllCaps = false
                    minWidth = 0
                    insetTop = 0
                    insetBottom = 0
                    cornerRadius = dimen(com.intuit.sdp.R.dimen._10sdp)
                    setTextColor(ContextCompat.getColor(requireContext(), R.color.ds_on_primary))
                    backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.ds_primary))
                    setOnClickListener { applySelections() }
                }
            )
        }
    }

    private fun bindStaticContent() {
        val selectedCategory = category
        titleText.text = getString(
            R.string.dynamic_specs_title_format,
            selectedCategory?.name ?: getString(R.string.dynamic_category_specs_title)
        )
        if (selectedCategory == null || selectedCategory.filters.isEmpty()) {
            filterTabsRecyclerView.isVisible = false
            optionSearchEditText.isVisible = false
            filterHeadingText.text = getString(R.string.dynamic_specs_no_specs)
            optionsRecyclerView.isVisible = false
            emptyText.isVisible = true
            emptyText.text = getString(R.string.dynamic_specs_no_specs)
        }
    }

    private fun renderFilterTabs() {
        val filters = category?.filters.orEmpty()
        filterTabsRecyclerView.isVisible = filters.size > 1
        tabsAdapter.update(filters, selectedFilterKey)
    }

    private fun renderOptions() {
        val filter = currentFilter()
        val options = filter?.options.orEmpty()
        val filteredOptions = options.filter {
            optionSearchQuery.isBlank() || it.name.contains(optionSearchQuery, ignoreCase = true)
        }
        filterHeadingText.text = filter?.displayLabel ?: getString(R.string.dynamic_specs_no_specs)
        optionSearchEditText.isVisible = options.size > OPTION_SEARCH_THRESHOLD
        optionsRecyclerView.isVisible = filteredOptions.isNotEmpty()
        emptyText.isVisible = filteredOptions.isEmpty()
        emptyText.text = getString(
            when {
                filter == null -> R.string.dynamic_specs_no_specs
                options.isEmpty() -> R.string.dynamic_specs_no_options
                else -> R.string.dynamic_specs_no_options_found
            }
        )
        optionsAdapter.update(
            options = filteredOptions,
            selectedOptionNames = selectedOptionsByFilter[filter?.selectionKey].orEmpty().toSet()
        )
    }

    private fun toggleOption(option: DynamicSpecOption) {
        val filter = currentFilter() ?: return
        val key = filter.selectionKey
        val currentSelections = selectedOptionsByFilter.getOrPut(key) { mutableListOf() }
        val alreadySelected = option.name in currentSelections

        if (filter.allowsMultipleSelections) {
            if (alreadySelected) {
                currentSelections.remove(option.name)
            } else {
                currentSelections.add(option.name)
            }
        } else if (!alreadySelected) {
            currentSelections.clear()
            currentSelections.add(option.name)
        }

        if (currentSelections.isEmpty()) {
            selectedOptionsByFilter.remove(key)
        }
        renderOptions()
        renderSelectedSummary()
    }

    private fun removeSelectedOption(chip: SelectedSpecChipUi) {
        selectedOptionsByFilter[chip.filterKey]?.remove(chip.optionName)
        if (selectedOptionsByFilter[chip.filterKey].isNullOrEmpty()) {
            selectedOptionsByFilter.remove(chip.filterKey)
        }
        renderOptions()
        renderSelectedSummary()
    }

    private fun renderSelectedSummary() {
        val chips = selectedOptionsByFilter.flatMap { (filterKey, options) ->
            options.map { option -> SelectedSpecChipUi(filterKey = filterKey, optionName = option) }
        }
        selectedChipsRecyclerView.isVisible = chips.isNotEmpty()
        selectedChipsAdapter.update(chips)
    }

    private fun applySelections() {
        val selectedCategory = category ?: return
        val result = SelectedCategorySpecs(
            categoryName = selectedCategory.name,
            selectedOptions = selectedOptionsByFilter
                .mapValues { (_, values) -> values.toList() }
                .filterValues { it.isNotEmpty() }
        )
        findNavController().getBackStackEntry(R.id.iaProductsFragment)
            .savedStateHandle[DynamicCategorySpecsResult.REQUEST_KEY] = DynamicCategorySpecsResult.toJson(result)
        findNavController().popBackStack(R.id.iaProductsFragment, false)
    }

    private fun currentFilter(): DynamicSpecFilter? {
        val selectedKey = selectedFilterKey
        return category?.filters.orEmpty().firstOrNull { it.selectionKey == selectedKey }
            ?: category?.filters?.firstOrNull()
    }

    private fun createEmptyText(): TextView {
        return TextView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
            gravity = Gravity.CENTER
            setTextColor(ContextCompat.getColor(requireContext(), R.color.ds_text_muted))
            setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(com.intuit.ssp.R.dimen._12ssp))
            isVisible = false
        }
    }

    private fun dimen(resId: Int): Int = resources.getDimensionPixelSize(resId)

    private companion object {
        const val OPTION_GRID_SPAN_COUNT = 2
        const val OPTION_SEARCH_THRESHOLD = 12
    }
}

private class DynamicSpecTabAdapter(
    private val onFilterClick: (DynamicSpecFilter) -> Unit
) : RecyclerView.Adapter<DynamicSpecTabAdapter.TabViewHolder>() {
    private val filters = mutableListOf<DynamicSpecFilter>()
    private var selectedFilterKey: String? = null

    fun update(filters: List<DynamicSpecFilter>, selectedFilterKey: String?) {
        this.filters.clear()
        this.filters.addAll(filters)
        this.selectedFilterKey = selectedFilterKey
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TabViewHolder {
        val label = TextView(parent.context).apply {
            layoutParams = RecyclerView.LayoutParams(
                RecyclerView.LayoutParams.WRAP_CONTENT,
                parent.resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._38sdp)
            ).apply {
                rightMargin = parent.resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._8sdp)
            }
            gravity = Gravity.CENTER
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
            setPadding(
                parent.resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._14sdp),
                0,
                parent.resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._14sdp),
                0
            )
            setTextSize(TypedValue.COMPLEX_UNIT_PX, parent.resources.getDimension(com.intuit.ssp.R.dimen._10ssp))
        }
        return TabViewHolder(label)
    }

    override fun onBindViewHolder(holder: TabViewHolder, position: Int) {
        val filter = filters[position]
        val selected = filter.selectionKey == selectedFilterKey
        val textView = holder.itemView as TextView
        textView.text = filter.displayLabel
        textView.setTextColor(ContextCompat.getColor(textView.context, if (selected) R.color.ds_primary else R.color.ds_text_primary))
        textView.background = optionBackground(textView, selected, radiusRes = com.intuit.sdp.R.dimen._8sdp)
        textView.setOnClickListener { onFilterClick(filter) }
    }

    override fun getItemCount(): Int = filters.size

    class TabViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
}

private class DynamicSpecOptionAdapter(
    private val onOptionClick: (DynamicSpecOption) -> Unit
) : RecyclerView.Adapter<DynamicSpecOptionAdapter.OptionViewHolder>() {
    private val options = mutableListOf<DynamicSpecOption>()
    private var selectedOptionNames = emptySet<String>()

    fun update(options: List<DynamicSpecOption>, selectedOptionNames: Set<String>) {
        this.options.clear()
        this.options.addAll(options.filter { it.name.isNotBlank() })
        this.selectedOptionNames = selectedOptionNames
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OptionViewHolder {
        val resources = parent.resources
        val label = TextView(parent.context).apply {
            layoutParams = RecyclerView.LayoutParams(
                RecyclerView.LayoutParams.MATCH_PARENT,
                resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._58sdp)
            ).apply {
                setMargins(
                    resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._4sdp),
                    resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._4sdp),
                    resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._4sdp),
                    resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._4sdp)
                )
            }
            gravity = Gravity.CENTER_VERTICAL
            maxLines = 2
            ellipsize = TextUtils.TruncateAt.END
            setPadding(
                resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._12sdp),
                0,
                resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._12sdp),
                0
            )
            setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(com.intuit.ssp.R.dimen._10ssp))
        }
        return OptionViewHolder(label)
    }

    override fun onBindViewHolder(holder: OptionViewHolder, position: Int) {
        val option = options[position]
        val selected = option.name in selectedOptionNames
        val textView = holder.itemView as TextView
        textView.text = option.name
        textView.setTextColor(ContextCompat.getColor(textView.context, if (selected) R.color.ds_primary else R.color.ds_text_primary))
        textView.background = optionBackground(textView, selected, radiusRes = com.intuit.sdp.R.dimen._8sdp)
        textView.setOnClickListener { onOptionClick(option) }
    }

    override fun getItemCount(): Int = options.size

    class OptionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
}

private data class SelectedSpecChipUi(
    val filterKey: String,
    val optionName: String
)

private class SelectedSpecChipAdapter(
    private val onRemove: (SelectedSpecChipUi) -> Unit
) : RecyclerView.Adapter<SelectedSpecChipAdapter.ChipViewHolder>() {
    private val chips = mutableListOf<SelectedSpecChipUi>()

    fun update(items: List<SelectedSpecChipUi>) {
        chips.clear()
        chips.addAll(items)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChipViewHolder {
        val resources = parent.resources
        val container = LinearLayout(parent.context).apply {
            layoutParams = RecyclerView.LayoutParams(
                RecyclerView.LayoutParams.WRAP_CONTENT,
                resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._30sdp)
            ).apply {
                rightMargin = resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._6sdp)
            }
            gravity = Gravity.CENTER_VERTICAL
            orientation = LinearLayout.HORIZONTAL
            setBackgroundResource(R.drawable.bg_filter_chip)
            setPadding(
                resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._10sdp),
                0,
                resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._8sdp),
                0
            )
        }
        val label = TextView(parent.context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            maxWidth = resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._92sdp)
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
            setTextColor(ContextCompat.getColor(parent.context, R.color.ds_primary))
            setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(com.intuit.ssp.R.dimen._10ssp))
        }
        val remove = TextView(parent.context).apply {
            text = "x"
            setTextColor(ContextCompat.getColor(parent.context, R.color.ds_primary))
            setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(com.intuit.ssp.R.dimen._11ssp))
            setPadding(resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._6sdp), 0, 0, 0)
        }
        container.addView(label)
        container.addView(remove)
        return ChipViewHolder(container, label, remove)
    }

    override fun onBindViewHolder(holder: ChipViewHolder, position: Int) {
        val chip = chips[position]
        holder.label.text = chip.optionName
        holder.remove.setOnClickListener { onRemove(chip) }
    }

    override fun getItemCount(): Int = chips.size

    class ChipViewHolder(
        itemView: View,
        val label: TextView,
        val remove: TextView
    ) : RecyclerView.ViewHolder(itemView)
}

private fun optionBackground(view: View, selected: Boolean, radiusRes: Int): GradientDrawable {
    val resources = view.resources
    return GradientDrawable().apply {
        cornerRadius = resources.getDimension(radiusRes)
        setColor(ContextCompat.getColor(view.context, if (selected) R.color.ds_primary_container else R.color.ds_surface))
        setStroke(
            resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._1sdp),
            ContextCompat.getColor(view.context, if (selected) R.color.ds_primary else R.color.ds_outline)
        )
    }
}

