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
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.axelliant.hris.R
import com.axelliant.hris.core.extensions.enableClearTextButton
import com.axelliant.hris.ui.designsystem.components.AppButtonView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.axelliant.hris.ui.designsystem.components.AppTextFieldView
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint
class DynamicCategorySelectionFragment : Fragment() {
    @Inject
    lateinit var categoryFilterRepository: StaticCategoryFilterRepository

    private lateinit var categoryAdapter: DynamicCategoryGridAdapter
    private lateinit var nextButton: AppButtonView
    private lateinit var emptyText: TextView

    private var categories = emptyList<DynamicCategoryFilter>()
    private var selectedSpecs = SelectedCategorySpecs()
    private var categorySearchQuery = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        selectedSpecs = DynamicCategorySpecsResult.fromJson(
            arguments?.getString(DynamicCategorySpecsResult.ARG_SELECTED_SPECS_JSON)
        )
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
        root.addView(createSearchField())
        val listFrame = LinearLayout(requireContext()).apply {
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
                dimen(com.intuit.sdp.R.dimen._10sdp)
            )
        }
        val recyclerView = RecyclerView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
            layoutManager = GridLayoutManager(requireContext(), CATEGORY_GRID_SPAN_COUNT)
            itemAnimator = null
            overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS
        }
        emptyText = createEmptyText(R.string.dynamic_category_no_categories_found)
        categoryAdapter = DynamicCategoryGridAdapter(
            selectedCategoryName = selectedSpecs.categoryName,
            onCategoryClick = ::handleCategoryClick
        )
        recyclerView.adapter = categoryAdapter
        listFrame.addView(recyclerView)
        listFrame.addView(emptyText)
        root.addView(listFrame)
        root.addView(createBottomActions())
        renderCategories()
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launch {
            categories = categoryFilterRepository.getCategories()
            if (selectedSpecs.categoryName.isNullOrBlank() || categories.none { it.name == selectedSpecs.categoryName }) {
                selectedSpecs = SelectedCategorySpecs(categoryName = categories.firstOrNull()?.name)
            }
            renderCategories()
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
            setPadding(
                dimen(com.intuit.sdp.R.dimen._8sdp),
                dimen(com.intuit.sdp.R.dimen._8sdp),
                dimen(com.intuit.sdp.R.dimen._14sdp),
                0
            )
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
            addView(
                TextView(requireContext()).apply {
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    text = getString(R.string.dynamic_category_specs_title)
                    setTextColor(ContextCompat.getColor(requireContext(), R.color.ds_text_primary))
                    setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(com.intuit.ssp.R.dimen._16ssp))
                    typeface = ResourcesCompat.getFont(requireContext(), R.font.poppins_semibold)
                    gravity = Gravity.CENTER_VERTICAL
                }
            )
        }
    }

    private fun createSearchField(): View {
        return AppTextFieldView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dimen(com.intuit.sdp.R.dimen._44sdp)
            ).apply {
                marginStart = dimen(com.intuit.sdp.R.dimen._14sdp)
                marginEnd = dimen(com.intuit.sdp.R.dimen._14sdp)
                topMargin = dimen(com.intuit.sdp.R.dimen._8sdp)
                bottomMargin = dimen(com.intuit.sdp.R.dimen._8sdp)
            }
            background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_filter_field)
            hint = getString(R.string.dynamic_category_search_hint)
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
                    categorySearchQuery = text?.toString().orEmpty()
                    renderCategories()
                }
            })
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
                    text = getString(R.string.dynamic_category_reset)
                    isAllCaps = false
                    minWidth = 0
                    insetTop = 0
                    insetBottom = 0
                    maxLines = 1
                    ellipsize = TextUtils.TruncateAt.END
                    backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.ds_surface))
                    setTextColor(ContextCompat.getColor(requireContext(), R.color.ds_primary))
                    setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(com.intuit.ssp.R.dimen._11ssp))
                    setOnClickListener { returnSelection(SelectedCategorySpecs()) }
                }
            )
            addView(
                View(requireContext()).apply {
                    layoutParams = LinearLayout.LayoutParams(dimen(com.intuit.sdp.R.dimen._16sdp), 1)
                }
            )
            nextButton = AppButtonView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(0, dimen(com.intuit.sdp.R.dimen._46sdp), 1f)
                isAllCaps = false
                minWidth = 0
                insetTop = 0
                insetBottom = 0
                maxLines = 1
                ellipsize = TextUtils.TruncateAt.END
                cornerRadius = dimen(com.intuit.sdp.R.dimen._2sdp)
                setTextColor(ContextCompat.getColor(requireContext(), R.color.ds_on_primary))
                setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(com.intuit.ssp.R.dimen._12ssp))
                backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.ds_primary))
                setOnClickListener { continueWithSelectedCategory() }
            }
            addView(nextButton)
        }
    }

    private fun handleCategoryClick(category: DynamicCategoryFilter) {
        val currentCategoryName = selectedSpecs.categoryName
        val changingCategory = currentCategoryName != null && currentCategoryName != category.name
        if (changingCategory && selectedSpecs.hasSelectedOptions) {
            MaterialAlertDialogBuilder(requireContext())
                .setMessage(R.string.dynamic_category_change_warning)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.dynamic_category_change_action) { _, _ ->
                    selectCategory(category)
                }
                .show()
            return
        }

        selectCategory(category)
    }

    private fun selectCategory(category: DynamicCategoryFilter) {
        selectedSpecs = if (selectedSpecs.categoryName == category.name) {
            selectedSpecs.copy(categoryName = category.name)
        } else {
            SelectedCategorySpecs(categoryName = category.name)
        }
        renderCategories()
    }

    private fun continueWithSelectedCategory() {
        val category = selectedCategory() ?: return
        if (category.filters.isEmpty()) {
            returnSelection(SelectedCategorySpecs(categoryName = category.name))
            return
        }

        findNavController().navigate(
            R.id.iaDynamicCategorySpecsFragment,
            bundleOf(
                DynamicCategorySpecsResult.ARG_CATEGORY_NAME to category.name,
                DynamicCategorySpecsResult.ARG_SELECTED_SPECS_JSON to DynamicCategorySpecsResult.toJson(selectedSpecs)
            )
        )
    }

    private fun renderCategories() {
        val filteredCategories = categories.filter {
            categorySearchQuery.isBlank() || it.name.contains(categorySearchQuery, ignoreCase = true)
        }
        categoryAdapter.update(
            categories = filteredCategories,
            selectedCategoryName = selectedSpecs.categoryName
        )
        emptyText.isVisible = filteredCategories.isEmpty()
        val selectedCategory = selectedCategory()
        nextButton.text = getString(
            when {
                selectedCategory == null -> R.string.next
                selectedCategory.filters.isEmpty() -> R.string.dynamic_category_done
                else -> R.string.next
            }
        )
        nextButton.isEnabled = selectedCategory != null
        nextButton.alpha = if (nextButton.isEnabled) 1f else DISABLED_ALPHA
    }

    private fun selectedCategory(): DynamicCategoryFilter? {
        val selectedCategoryName = selectedSpecs.categoryName ?: return null
        return categories.firstOrNull { it.name == selectedCategoryName }
    }

    private fun returnSelection(specs: SelectedCategorySpecs) {
        findNavController().getBackStackEntry(R.id.iaProductsFragment)
            .savedStateHandle[DynamicCategorySpecsResult.REQUEST_KEY] = DynamicCategorySpecsResult.toJson(specs)
        findNavController().popBackStack(R.id.iaProductsFragment, false)
    }

    private fun createEmptyText(textRes: Int): TextView {
        return TextView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
            gravity = Gravity.CENTER
            text = getString(textRes)
            setTextColor(ContextCompat.getColor(requireContext(), R.color.ds_text_muted))
            setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(com.intuit.ssp.R.dimen._12ssp))
            isVisible = false
        }
    }

    private fun dimen(resId: Int): Int = resources.getDimensionPixelSize(resId)

    private companion object {
        const val CATEGORY_GRID_SPAN_COUNT = 2
        const val DISABLED_ALPHA = 0.45f
    }
}

private class DynamicCategoryGridAdapter(
    private var selectedCategoryName: String?,
    private val onCategoryClick: (DynamicCategoryFilter) -> Unit
) : RecyclerView.Adapter<DynamicCategoryGridAdapter.CategoryViewHolder>() {
    private val categories = mutableListOf<DynamicCategoryFilter>()

    fun update(categories: List<DynamicCategoryFilter>, selectedCategoryName: String?) {
        this.categories.clear()
        this.categories.addAll(categories)
        this.selectedCategoryName = selectedCategoryName
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val resources = parent.resources
        val card = LinearLayout(parent.context).apply {
            layoutParams = RecyclerView.LayoutParams(
                RecyclerView.LayoutParams.MATCH_PARENT,
                resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._52sdp)
            ).apply {
                setMargins(
                    resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._4sdp),
                    resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._4sdp),
                    resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._4sdp),
                    resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._4sdp)
                )
            }
            gravity = Gravity.CENTER_VERTICAL
            orientation = LinearLayout.HORIZONTAL
            setPadding(
                resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._12sdp),
                0,
                resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._12sdp),
                0
            )
        }
        val label = TextView(parent.context).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            ellipsize = TextUtils.TruncateAt.END
            maxLines = 2
            setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(com.intuit.ssp.R.dimen._10ssp))
        }
        card.addView(label)
        return CategoryViewHolder(card, label)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val category = categories[position]
        val selected = category.name == selectedCategoryName
        holder.label.text = category.name
        holder.label.setTextColor(
            ContextCompat.getColor(
                holder.itemView.context,
                if (selected) R.color.ds_primary else R.color.ds_text_primary
            )
        )
        holder.itemView.background = categoryCardBackground(holder.itemView, selected)
        holder.itemView.setOnClickListener { onCategoryClick(category) }
    }

    override fun getItemCount(): Int = categories.size

    private fun categoryCardBackground(view: View, selected: Boolean): GradientDrawable {
        val resources = view.resources
        return GradientDrawable().apply {
            cornerRadius = resources.getDimension(com.intuit.sdp.R.dimen._8sdp)
            setColor(ContextCompat.getColor(view.context, if (selected) R.color.ds_primary_container else R.color.ds_surface))
            setStroke(
                resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._1sdp),
                ContextCompat.getColor(view.context, if (selected) R.color.ds_primary else R.color.ds_outline)
            )
        }
    }

    class CategoryViewHolder(
        itemView: View,
        val label: TextView
    ) : RecyclerView.ViewHolder(itemView)
}



