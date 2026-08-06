package com.axelliant.hris.features.inventory.products.presentation

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.StyleSpan
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.axelliant.hris.R
import com.axelliant.hris.databinding.FragmentProductComparisonBinding
import com.axelliant.hris.features.inventory.products.presentation.ProductPickerResultBundles.toProductListItem
import com.axelliant.hris.features.quotes.presentation.SmartQuoteProductFlow
import com.axelliant.hris.ui.designsystem.components.AppTextView
import kotlinx.coroutines.launch

class ProductComparisonFragment : Fragment() {
    private val viewModel: ProductComparisonViewModel by viewModels()
    private var _binding: FragmentProductComparisonBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProductComparisonBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupInteractions()
        observeProductSelectionResult()
        observeState()
    }

    private fun setupInteractions() {
        binding.backButton.setOnClickListener { findNavController().navigateUp() }
        binding.resetComparisonButton.setOnClickListener { viewModel.resetComparison() }
        binding.productOneCard.setOnClickListener {
            openProductPicker(ProductComparisonFlow.SLOT_ONE, viewModel.uiState.value.productOne)
        }
        binding.productTwoCard.setOnClickListener {
            openProductPicker(ProductComparisonFlow.SLOT_TWO, viewModel.uiState.value.productTwo)
        }
        binding.productOneEditButton.setOnClickListener {
            openProductPicker(ProductComparisonFlow.SLOT_ONE, viewModel.uiState.value.productOne)
        }
        binding.productTwoEditButton.setOnClickListener {
            openProductPicker(ProductComparisonFlow.SLOT_TWO, viewModel.uiState.value.productTwo)
        }
        binding.productOneDeleteButton.setOnClickListener {
            viewModel.removeProduct(ProductComparisonFlow.SLOT_ONE)
        }
        binding.productTwoDeleteButton.setOnClickListener {
            viewModel.removeProduct(ProductComparisonFlow.SLOT_TWO)
        }
        binding.overviewTab.setOnClickListener { viewModel.selectTab(ProductComparisonTab.Overview) }
        binding.priceStockTab.setOnClickListener { viewModel.selectTab(ProductComparisonTab.PriceStock) }
        binding.moreDetailsTab.setOnClickListener { viewModel.selectTab(ProductComparisonTab.MoreDetails) }
        binding.compareNowButton.setOnClickListener { viewModel.compareProducts() }
        binding.saveComparisonButton.setOnClickListener { saveComparison() }
        binding.shareComparisonButton.setOnClickListener { shareComparison(viewModel.uiState.value) }
        binding.doneButton.setOnClickListener { findNavController().navigateUp() }
    }

    private fun observeProductSelectionResult() {
        val savedStateHandle = runCatching {
            findNavController().getBackStackEntry(R.id.iaProductComparisonFragment).savedStateHandle
        }.getOrNull() ?: return
        savedStateHandle
            .getLiveData<Bundle>(ProductComparisonFlow.RESULT_PRODUCT)
            .observe(viewLifecycleOwner) { bundle ->
                savedStateHandle.remove<Bundle>(ProductComparisonFlow.RESULT_PRODUCT)
                val slot = savedStateHandle.get<Int>(ProductComparisonFlow.RESULT_SLOT)
                    ?: ProductComparisonFlow.SLOT_ONE
                savedStateHandle.remove<Int>(ProductComparisonFlow.RESULT_SLOT)
                bundle.toProductListItem()?.let { product ->
                    viewModel.setProduct(slot, product)
                }
            }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect(::renderState)
            }
        }
    }

    private fun openProductPicker(slot: Int, currentProduct: ProductListItemUi?) {
        val args = Bundle().apply {
            putString(
                SmartQuoteProductFlow.ARG_PRODUCT_PICKER_FLOW,
                ProductComparisonFlow.FLOW_PRODUCT_COMPARISON
            )
            putInt(ProductComparisonFlow.ARG_COMPARE_SLOT, slot)
            currentProduct?.let {
                putBundle(
                    ProductComparisonFlow.ARG_PRESELECTED_PRODUCT,
                    ProductPickerResultBundles.fromProduct(it)
                )
            }
        }
        findNavController().navigate(R.id.iaAskAiProductSearchFragment, args)
    }

    private fun renderState(state: ProductComparisonUiState) {
        binding.titleText.text = getString(R.string.product_compare_title_format, state.selectedCount)
        renderProductCard(
            slot = ProductComparisonFlow.SLOT_ONE,
            product = state.productOne
        )
        renderProductCard(
            slot = ProductComparisonFlow.SLOT_TWO,
            product = state.productTwo
        )
        renderRecommendation(state)
        renderBottomActions(state)
        renderTabs(state.selectedTab)
        renderComparisonTable(state)
    }

    private fun renderProductCard(slot: Int, product: ProductListItemUi?) {
        val isFirst = slot == ProductComparisonFlow.SLOT_ONE
        val card = if (isFirst) binding.productOneCard else binding.productTwoCard
        val content = if (isFirst) binding.productOneContent else binding.productTwoContent
        val headerRow = if (isFirst) binding.productOneHeaderRow else binding.productTwoHeaderRow
        val placeholderIcon = if (isFirst) binding.productOnePlaceholderIconFrame else binding.productTwoPlaceholderIconFrame
        val spacer = if (isFirst) binding.productOneSpacer else binding.productTwoSpacer
        val nameView = if (isFirst) binding.productOneName else binding.productTwoName
        val skuView = if (isFirst) binding.productOneSku else binding.productTwoSku
        val priceView = if (isFirst) binding.productOnePrice else binding.productTwoPrice
        val statusView = if (isFirst) binding.productOneStatus else binding.productTwoStatus
        val actionView = if (isFirst) binding.productOneAction else binding.productTwoAction
        actionView.isVisible = false

        if (product == null) {
            card.setStrokeColor(ContextCompat.getColor(requireContext(), R.color.ds_primary_container))
            content.gravity = Gravity.CENTER
            spacer.isVisible = false
            headerRow.isVisible = false
            placeholderIcon.isVisible = true
            nameView.text = getString(
                if (isFirst) R.string.product_compare_choose_product_one else R.string.product_compare_choose_product_two
            )
            skuView.isVisible = false
            priceView.isVisible = false
            statusView.isVisible = false
            nameView.maxLines = 2
            nameView.gravity = Gravity.CENTER
            nameView.setTextColor(ContextCompat.getColor(requireContext(), R.color.ds_text_secondary))
            nameView.setTextSizeFromDimen(com.intuit.ssp.R.dimen._9ssp)
            return
        }

        card.setStrokeColor(ContextCompat.getColor(requireContext(), R.color.ds_outline))
        content.gravity = Gravity.NO_GRAVITY
        spacer.isVisible = true
        headerRow.isVisible = true
        placeholderIcon.isVisible = false
        nameView.text = product.name.ifBlank { getString(R.string.product_compare_unnamed_product) }
        nameView.maxLines = 3
        nameView.gravity = Gravity.START
        nameView.setTextColor(ContextCompat.getColor(requireContext(), R.color.ds_text_primary))
        nameView.setTextSizeFromDimen(com.intuit.ssp.R.dimen._8ssp)
        skuView.isVisible = false
        priceView.isVisible = true
        statusView.isVisible = true
        priceView.text = product.displayPrice.ifBlank { product.listPrice.ifBlank { "-" } }
        priceView.setTextSizeFromDimen(com.intuit.ssp.R.dimen._8ssp)
        statusView.setTextSizeFromDimen(com.intuit.ssp.R.dimen._7ssp)
        renderStatusChip(statusView, product)
        actionView.text = getString(R.string.product_compare_edit_product)
    }

    private fun renderStatusChip(view: AppTextView, product: ProductListItemUi) {
        val inStock = product.availability == ProductAvailability.InStock
        val quantity = product.availableQuantity()
        val positiveStock = inStock && quantity > 0
        view.text = if (inStock && quantity > 0) {
            getString(R.string.product_compare_in_stock_quantity, quantity)
        } else {
            product.availability.label
        }
        view.setTextColor(
            ContextCompat.getColor(
                requireContext(),
                if (positiveStock) R.color.ds_success else R.color.ds_error
            )
        )
        view.setBackgroundResource(if (positiveStock) R.drawable.bg_status_success else R.drawable.bg_status_error)
    }

    private fun renderRecommendation(state: ProductComparisonUiState) {
        binding.compareNowButton.isVisible = state.canCompare && !state.showRecommendation && !state.isComparing
        binding.comparisonLoadingContainer.isVisible = state.isComparing
        binding.recommendationCard.isVisible = state.showRecommendation
        if (state.showRecommendation && state.productOne != null && state.productTwo != null) {
            val warning = state.productOne.isSameProductAs(state.productTwo)
            renderRecommendationStyle(warning)
            binding.recommendationText.text = buildRecommendation(state.productOne, state.productTwo)
        }
    }

    private fun renderRecommendationStyle(warning: Boolean) {
        val accentColor = ContextCompat.getColor(
            requireContext(),
            if (warning) R.color.ds_warning else R.color.ds_success
        )
        binding.recommendationCard.setBackgroundResource(
            if (warning) R.drawable.bg_ask_ai_warning else R.drawable.bg_compare_recommendation
        )
        binding.recommendationIcon.imageTintList = ContextCompat.getColorStateList(
            requireContext(),
            if (warning) R.color.ds_warning else R.color.ds_success
        )
        binding.recommendationTitleText.setTextColor(accentColor)
        binding.recommendationTitleText.text = getString(
            if (warning) R.string.product_compare_warning_title else R.string.product_compare_ai_recommendation
        )
    }

    private fun renderBottomActions(state: ProductComparisonUiState) {
        binding.saveComparisonButton.isEnabled = state.canCompare
        binding.shareComparisonButton.isEnabled = state.canCompare
    }

    private fun buildRecommendation(productOne: ProductListItemUi, productTwo: ProductListItemUi): CharSequence {
        if (productOne.isSameProductAs(productTwo)) {
            return buildSameProductWarning(productOne)
        }
        val productOneStrengths = productStrengths(productOne, productTwo)
        val productTwoStrengths = productStrengths(productTwo, productOne)
        val recommendedProduct = recommendedProduct(productOne, productTwo, productOneStrengths, productTwoStrengths)
        return SpannableStringBuilder().apply {
            if (recommendedProduct == null) {
                append(getString(R.string.product_compare_no_clear_winner))
            } else {
                append(getString(R.string.product_compare_recommended_prefix))
                appendBold(recommendedProduct.name.cleanValue())
                append(" ")
                append(getString(R.string.product_compare_recommended_suffix))
            }
            append("\n\n")
            appendBold(getString(R.string.product_compare_product_one))
            append(" - ")
            appendBold(productOne.name.cleanValue())
            append("\n")
            appendStrengths(productOneStrengths)
            append("\n")
            appendBold(getString(R.string.product_compare_product_two))
            append(" - ")
            appendBold(productTwo.name.cleanValue())
            append("\n")
            appendStrengths(productTwoStrengths)
        }
    }

    private fun buildSameProductWarning(product: ProductListItemUi): CharSequence {
        return SpannableStringBuilder().apply {
            append(getString(R.string.product_compare_same_product_warning_prefix))
            appendBold(product.name.cleanValue())
            append(getString(R.string.product_compare_same_product_warning_suffix))
        }
    }

    private fun productStrengths(product: ProductListItemUi, other: ProductListItemUi): List<String> {
        val strengths = mutableListOf<String>()
        val productPrice = product.numericPrice()
        val otherPrice = other.numericPrice()
        if (productPrice != null && otherPrice != null && productPrice < otherPrice) {
            strengths += getString(
                R.string.product_compare_strength_lower_price,
                product.displayPrice.cleanValue(),
                other.displayPrice.cleanValue()
            )
        }
        val productInStock = product.availability == ProductAvailability.InStock && product.availableQuantity() > 0
        val otherInStock = other.availability == ProductAvailability.InStock && other.availableQuantity() > 0
        if (productInStock && !otherInStock) {
            strengths += getString(R.string.product_compare_strength_in_stock)
        }
        val productQuantity = product.availableQuantity()
        val otherQuantity = other.availableQuantity()
        if (productQuantity > otherQuantity) {
            strengths += getString(
                R.string.product_compare_strength_higher_quantity,
                productQuantity,
                otherQuantity
            )
        }
        if (product.vendors.size > other.vendors.size) {
            strengths += getString(
                R.string.product_compare_strength_vendor_offers,
                product.vendors.size,
                other.vendors.size
            )
        }
        if (
            product.manufacturer.isNotBlank() &&
            other.manufacturer.isNotBlank() &&
            product.manufacturer.equals(other.manufacturer, ignoreCase = true)
        ) {
            strengths += getString(R.string.product_compare_strength_same_manufacturer, product.manufacturer)
        }
        return strengths.take(MAX_RECOMMENDATION_STRENGTHS)
    }

    private fun recommendedProduct(
        productOne: ProductListItemUi,
        productTwo: ProductListItemUi,
        productOneStrengths: List<String>,
        productTwoStrengths: List<String>
    ): ProductListItemUi? {
        val oneScore = recommendationScore(productOne, productTwo, productOneStrengths.size)
        val twoScore = recommendationScore(productTwo, productOne, productTwoStrengths.size)
        return when {
            oneScore > twoScore -> productOne
            twoScore > oneScore -> productTwo
            else -> null
        }
    }

    private fun recommendationScore(product: ProductListItemUi, other: ProductListItemUi, strengthsCount: Int): Int {
        val productPrice = product.numericPrice()
        val otherPrice = other.numericPrice()
        val priceScore = if (productPrice != null && otherPrice != null && productPrice < otherPrice) 2 else 0
        val stockScore = if (
            product.availability == ProductAvailability.InStock &&
            product.availableQuantity() > 0 &&
            (other.availability != ProductAvailability.InStock || other.availableQuantity() == 0)
        ) 3 else 0
        val quantityScore = if (product.availableQuantity() > other.availableQuantity()) 1 else 0
        val vendorScore = if (product.vendors.size > other.vendors.size) 1 else 0
        return priceScore + stockScore + quantityScore + vendorScore + strengthsCount
    }

    private fun SpannableStringBuilder.appendStrengths(strengths: List<String>) {
        if (strengths.isEmpty()) {
            append("- ")
            append(getString(R.string.product_compare_strength_none))
            append("\n")
            return
        }
        strengths.forEach { strength ->
            append("- ")
            append(strength)
            append("\n")
        }
    }

    private fun SpannableStringBuilder.appendBold(value: String) {
        val start = length
        append(value)
        setSpan(StyleSpan(Typeface.BOLD), start, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }

    private fun ProductListItemUi.isSameProductAs(other: ProductListItemUi): Boolean {
        return (id.isNotBlank() && id == other.id) ||
            (sku.isNotBlank() && sku.equals(other.sku, ignoreCase = true))
    }

    private fun renderTabs(selectedTab: ProductComparisonTab) {
        val tabs = listOf(
            Triple(ProductComparisonTab.Overview, binding.overviewTabText, binding.overviewTabIndicator),
            Triple(ProductComparisonTab.PriceStock, binding.priceStockTabText, binding.priceStockTabIndicator),
            Triple(ProductComparisonTab.MoreDetails, binding.moreDetailsTabText, binding.moreDetailsTabIndicator)
        )
        tabs.forEach { (tab, textView, indicator) ->
            val selected = tab == selectedTab
            textView.setTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    if (selected) R.color.ds_primary else R.color.ds_text_secondary
                )
            )
            indicator.isVisible = selected
        }
    }

    private fun renderComparisonTable(state: ProductComparisonUiState) {
        binding.comparisonTableContainer.removeAllViews()
        binding.comparisonTableContainer.addView(
            createTableRow(
                listOf(
                    getString(R.string.product_compare_table_detail),
                    getString(R.string.product_compare_product_one),
                    getString(R.string.product_compare_product_two),
                    getString(R.string.product_compare_table_comparison)
                ),
                isHeader = true
            )
        )
        comparisonRows(state).forEach { row ->
            binding.comparisonTableContainer.addView(
                createTableRow(
                    listOf(row.label, row.productOneValue, row.productTwoValue, row.comparison)
                )
            )
        }
    }

    private fun comparisonRows(state: ProductComparisonUiState): List<ComparisonRow> {
        val one = state.productOne
        val two = state.productTwo
        return when (state.selectedTab) {
            ProductComparisonTab.Overview -> listOf(
                textRow("Manufacturer", one?.manufacturer, two?.manufacturer),
                textRow("Category", one?.category, two?.category),
                textRow("AXE Part No", one?.sku, two?.sku),
                textRow("MFG Part No", one?.manufacturerPartNumber, two?.manufacturerPartNumber),
                priceRow(one, two),
                stockRow(one, two),
                quantityRow(one, two),
                vendorOfferRow(one, two)
            )
            ProductComparisonTab.PriceStock -> listOf(
                priceRow(one, two),
                stockRow(one, two),
                quantityRow(one, two),
                vendorOfferRow(one, two),
                textRow("Primary Vendor", one?.vendor, two?.vendor),
                textRow("Last Update", one?.lastUpdate, two?.lastUpdate)
            )
            ProductComparisonTab.MoreDetails -> listOf(
                textRow("Description", one?.description, two?.description),
                textRow("Country", one?.countryOfOrigin, two?.countryOfOrigin),
                textRow("Screen Size", one?.screenSize, two?.screenSize),
                textRow("Dimensions", one?.dimensions, two?.dimensions),
                textRow("Weight", one?.weight, two?.weight),
                textRow("Warranty", one?.warrantyPeriod, two?.warrantyPeriod),
                textRow("Validity", one?.validityPeriod, two?.validityPeriod)
            )
        }
    }

    private fun textRow(label: String, one: String?, two: String?): ComparisonRow {
        val oneValue = one.cleanValue()
        val twoValue = two.cleanValue()
        return ComparisonRow(
            label = label,
            productOneValue = oneValue,
            productTwoValue = twoValue,
            comparison = when {
                oneValue == "-" || twoValue == "-" -> "-"
                oneValue.equals(twoValue, ignoreCase = true) -> getString(R.string.product_compare_same)
                else -> "-"
            }
        )
    }

    private fun priceRow(one: ProductListItemUi?, two: ProductListItemUi?): ComparisonRow {
        val onePrice = one?.numericPrice()
        val twoPrice = two?.numericPrice()
        return ComparisonRow(
            label = "Price (List)",
            productOneValue = one?.displayPrice.cleanValue(),
            productTwoValue = two?.displayPrice.cleanValue(),
            comparison = when {
                onePrice == null || twoPrice == null -> "-"
                onePrice == twoPrice -> getString(R.string.product_compare_same)
                onePrice < twoPrice -> getString(R.string.product_compare_p1_lower)
                else -> getString(R.string.product_compare_p2_lower)
            }
        )
    }

    private fun stockRow(one: ProductListItemUi?, two: ProductListItemUi?): ComparisonRow {
        val oneStock = one?.availability?.label.cleanValue()
        val twoStock = two?.availability?.label.cleanValue()
        return ComparisonRow(
            label = "Stock Status",
            productOneValue = one?.stockLabel().cleanValue(),
            productTwoValue = two?.stockLabel().cleanValue(),
            comparison = when {
                one == null || two == null -> "-"
                one.availability == two.availability -> getString(R.string.product_compare_same)
                two.availability == ProductAvailability.InStock -> getString(R.string.product_compare_p2_in_stock)
                one.availability == ProductAvailability.InStock -> getString(R.string.product_compare_p1_in_stock)
                else -> "$oneStock / $twoStock"
            }
        )
    }

    private fun quantityRow(one: ProductListItemUi?, two: ProductListItemUi?): ComparisonRow {
        val oneQty = one?.availableQuantity()
        val twoQty = two?.availableQuantity()
        return ComparisonRow(
            label = "Quantity",
            productOneValue = oneQty?.toString() ?: "-",
            productTwoValue = twoQty?.toString() ?: "-",
            comparison = when {
                oneQty == null || twoQty == null -> "-"
                oneQty == twoQty -> getString(R.string.product_compare_same)
                oneQty > twoQty -> getString(R.string.product_compare_p1_higher)
                else -> getString(R.string.product_compare_p2_higher)
            }
        )
    }

    private fun vendorOfferRow(one: ProductListItemUi?, two: ProductListItemUi?): ComparisonRow {
        val oneCount = one?.vendors?.size
        val twoCount = two?.vendors?.size
        return ComparisonRow(
            label = "Vendor Offers",
            productOneValue = oneCount?.let { resources.getQuantityString(R.plurals.product_compare_offer_count, it, it) } ?: "-",
            productTwoValue = twoCount?.let { resources.getQuantityString(R.plurals.product_compare_offer_count, it, it) } ?: "-",
            comparison = when {
                oneCount == null || twoCount == null -> "-"
                oneCount == twoCount -> getString(R.string.product_compare_same)
                oneCount > twoCount -> getString(R.string.product_compare_p1_more)
                else -> getString(R.string.product_compare_p2_more)
            }
        )
    }

    private fun createTableRow(values: List<String>, isHeader: Boolean = false): View {
        return LinearLayout(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            orientation = LinearLayout.HORIZONTAL
            values.forEachIndexed { index, value ->
                val highlightZeroQuantity = !isHeader &&
                    values.firstOrNull() == "Quantity" &&
                    index in 1..2 &&
                    value == "0"
                addView(
                    createTableCell(
                        text = value,
                        isHeader = isHeader,
                        weight = if (index == 0) 1.18f else 1f,
                        highlightError = highlightZeroQuantity
                    )
                )
            }
        }
    }

    private fun saveComparison() {
        if (!viewModel.uiState.value.canCompare) return
        Toast.makeText(
            requireContext(),
            R.string.product_compare_saved_message,
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun shareComparison(state: ProductComparisonUiState) {
        val one = state.productOne ?: return
        val two = state.productTwo ?: return
        val shareText = buildString {
            appendLine(getString(R.string.product_compare_share_title))
            appendLine("${getString(R.string.product_compare_product_one)}: ${one.name}")
            appendLine("${getString(R.string.product_compare_product_two)}: ${two.name}")
            appendLine("${getString(R.string.product_compare_price)}: ${one.displayPrice.cleanValue()} / ${two.displayPrice.cleanValue()}")
            appendLine("${getString(R.string.product_compare_stock_status)}: ${one.stockLabel()} / ${two.stockLabel()}")
            if (state.showRecommendation) {
                appendLine()
                appendLine(buildRecommendation(one, two).toString())
            }
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, getString(R.string.product_compare_share_title))
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        startActivity(Intent.createChooser(intent, getString(R.string.product_compare_share)))
    }

    private fun AppTextView.setTextSizeFromDimen(dimenRes: Int) {
        setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(dimenRes))
    }

    private fun createTableCell(
        text: String,
        isHeader: Boolean,
        weight: Float,
        highlightError: Boolean = false
    ): AppTextView {
        return AppTextView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                weight
            )
            minHeight = resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._38sdp)
            gravity = Gravity.CENTER
            setBackgroundResource(
                if (isHeader) R.drawable.bg_compare_table_header_cell else R.drawable.bg_compare_table_cell
            )
            setPadding(
                resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._5sdp),
                resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._6sdp),
                resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._5sdp),
                resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._6sdp)
            )
            this.text = text
            maxLines = 3
            setTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    when {
                        highlightError -> R.color.ds_error
                        isHeader -> R.color.ds_text_primary
                        text.contains("Out of Stock", ignoreCase = true) -> R.color.ds_error
                        text.contains("In Stock", ignoreCase = true) ||
                            text.contains("Same", ignoreCase = true) ||
                            text.contains("Higher", ignoreCase = true) ||
                            text.contains("Lower", ignoreCase = true) ||
                            text.contains("More", ignoreCase = true) -> R.color.ds_success
                        else -> R.color.ds_text_primary
                    }
                )
            )
            textSize = if (isHeader) 8.5f else 8f
            typeface = if (isHeader) {
                android.graphics.Typeface.DEFAULT_BOLD
            } else {
                android.graphics.Typeface.DEFAULT
            }
        }
    }

    private fun ProductListItemUi.stockLabel(): String {
        val quantity = availableQuantity()
        return if (availability == ProductAvailability.InStock && quantity > 0) {
            getString(R.string.product_compare_in_stock_quantity, quantity)
        } else {
            availability.label
        }
    }

    private fun ProductListItemUi.availableQuantity(): Int {
        return vendors.sumOf { vendor ->
            vendor.unit.filter(Char::isDigit).toIntOrNull() ?: 0
        }
    }

    private fun ProductListItemUi.numericPrice(): Double? {
        return displayPrice.ifBlank { listPrice }
            .replace("$", "")
            .replace(",", "")
            .trim()
            .toDoubleOrNull()
    }

    private fun String?.cleanValue(): String {
        return this?.trim()?.takeIf { it.isNotBlank() } ?: "-"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private companion object {
        const val MAX_RECOMMENDATION_STRENGTHS = 4
    }
}

private data class ComparisonRow(
    val label: String,
    val productOneValue: String,
    val productTwoValue: String,
    val comparison: String
)

