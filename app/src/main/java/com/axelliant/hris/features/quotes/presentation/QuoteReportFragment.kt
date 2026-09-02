package com.axelliant.hris.features.quotes.presentation

import android.os.Bundle
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.axelliant.hris.R
import com.axelliant.hris.core.ui.UiState
import com.axelliant.hris.databinding.FragmentQuoteReportBinding
import com.axelliant.hris.databinding.ItemQuoteReportLineItemBinding
import com.axelliant.hris.databinding.LayoutQuoteReportFieldRowBinding
import com.axelliant.hris.features.quotes.domain.model.QuoteReportFieldUiModel
import com.axelliant.hris.features.quotes.domain.model.QuoteReportLineItemUiModel
import com.axelliant.hris.features.quotes.domain.model.QuoteReportUiModel
import com.axelliant.hris.ui.designsystem.components.AppButtonView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class QuoteReportFragment : Fragment() {

    private val viewModel: QuoteReportViewModel by viewModels()
    private var _binding: FragmentQuoteReportBinding? = null
    private val binding get() = _binding!!

    private var currentLineItems: List<QuoteReportLineItemUiModel> = emptyList()
    private var showAllProducts = false
    private var selectedSection = ReportSection.Items
    private var totalsExpanded = false
    private val expandedProductKeys = mutableSetOf<String>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentQuoteReportBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.appTopBar.setOnBackClickListener { findNavController().navigateUp() }
        binding.retryButton.setOnClickListener { viewModel.loadReport() }
        binding.productsToggleButton.setOnClickListener {
            showAllProducts = !showAllProducts
            renderLineItems()
        }
        binding.itemsTabButton.setOnClickListener { setReportSection(ReportSection.Items) }
        binding.infoTabButton.setOnClickListener { setReportSection(ReportSection.Info) }
        binding.stickyTotalHeader.setOnClickListener { setTotalsExpanded(!totalsExpanded) }
        binding.totalsChevronButton.setOnClickListener { setTotalsExpanded(!totalsExpanded) }
        observeReport()
    }

    private fun observeReport() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.reportState.collect { state ->
                    when (state) {
                        UiState.Loading -> showLoading()
                        is UiState.Success -> showContent(state.data)
                        is UiState.Error -> showError(state.message)
                        UiState.Unauthorized -> showError(getString(R.string.quotes_error))
                        UiState.Empty -> showError(getString(R.string.quote_report_empty))
                        UiState.Idle -> Unit
                    }
                }
            }
        }
    }

    private fun showLoading() {
        binding.contentScroll.isVisible = false
        binding.errorStateGroup.isVisible = false
        binding.stickyTotalBar.isVisible = false
        binding.loadingProgress.isVisible = true
    }

    private fun showContent(report: QuoteReportUiModel) {
        binding.loadingProgress.isVisible = false
        binding.errorStateGroup.isVisible = false
        binding.contentScroll.isVisible = true
        binding.stickyTotalBar.isVisible = true
        bindReport(report)
    }

    private fun showError(message: String) {
        binding.loadingProgress.isVisible = false
        binding.contentScroll.isVisible = false
        binding.stickyTotalBar.isVisible = false
        binding.errorStateGroup.isVisible = true
        binding.errorStateText.text = message.ifBlank { getString(R.string.quote_report_error) }
    }

    private fun bindReport(report: QuoteReportUiModel) {
        binding.quoteNumberText.text = report.quoteNumber
        binding.createdDateText.text = report.createdDate
        binding.customerNameText.text = report.customerName
        binding.customerNumberText.text = report.customerNumber
        binding.companyAddressText.text = report.companyAddress
        binding.companyPhoneText.text = report.companyPhone
        binding.billingAddressText.text = report.billingAddress
        binding.shippingAddressText.text = report.shippingAddress
        binding.quoteTitleText.text = report.quoteTitle
        binding.createdBySummaryText.text = report.contactInformation.fieldValue("Created By")
        binding.stickyGrandTotalText.text = report.grandTotal
        binding.footerText.text = report.footerText

        bindRows(binding.quoteInfoContainer, report.quoteInformation)
        bindRows(binding.contactInfoContainer, report.contactInformation)
        showAllProducts = false
        expandedProductKeys.clear()
        bindLineItems(report.lineItems)
        bindRows(binding.totalsContainer, report.totals)
        bindRows(binding.acceptanceContainer, report.acceptance)
        setReportSection(selectedSection)
        setTotalsExpanded(false)
    }

    private fun setReportSection(section: ReportSection) {
        selectedSection = section
        binding.itemsSection.isVisible = section == ReportSection.Items
        binding.infoSection.isVisible = section == ReportSection.Info
        bindTabState(binding.itemsTabButton, isSelected = section == ReportSection.Items)
        bindTabState(binding.infoTabButton, isSelected = section == ReportSection.Info)
    }

    private fun bindTabState(button: AppButtonView, isSelected: Boolean) {
        val backgroundColor = ContextCompat.getColor(
            requireContext(),
            if (isSelected) R.color.ds_primary else R.color.ds_surface
        )
        val textColor = ContextCompat.getColor(
            requireContext(),
            if (isSelected) R.color.ds_neutral_white else R.color.ds_text_primary
        )
        button.backgroundTintList = ColorStateList.valueOf(backgroundColor)
        button.setTextColor(textColor)
    }

    private fun setTotalsExpanded(expanded: Boolean) {
        totalsExpanded = expanded
        binding.stickyTotalsDetails.isVisible = expanded
        binding.totalsChevronButton.setIconResource(
            if (expanded) R.drawable.ic_chevron_up else R.drawable.ic_chevron_down
        )
        binding.totalsChevronButton.contentDescription = getString(
            if (expanded) R.string.quote_report_collapse_total else R.string.quote_report_expand_total
        )
    }

    private fun bindRows(container: ViewGroup, rows: List<QuoteReportFieldUiModel>) {
        container.removeAllViews()
        rows.forEach { row ->
            val rowBinding = LayoutQuoteReportFieldRowBinding.inflate(layoutInflater, container, false)
            rowBinding.fieldLabel.text = row.label
            rowBinding.fieldValue.text = row.value
            container.addView(rowBinding.root)
        }
    }

    private fun bindLineItems(items: List<QuoteReportLineItemUiModel>) {
        currentLineItems = items
        renderLineItems()
    }

    private fun renderLineItems() {
        binding.lineItemsContainer.removeAllViews()
        binding.emptyLineItemsText.isVisible = currentLineItems.isEmpty()
        binding.productsCountText.text = resources.getQuantityString(
            R.plurals.quote_report_products_count,
            currentLineItems.size,
            currentLineItems.size
        )
        val shouldLimitProducts = currentLineItems.size > COLLAPSED_PRODUCT_LIMIT
        binding.productsToggleButton.isVisible = shouldLimitProducts
        binding.productsToggleButton.text = if (showAllProducts) {
            getString(R.string.quote_report_show_less_products)
        } else {
            getString(R.string.quote_report_show_all_products)
        }

        val visibleItems = if (shouldLimitProducts && !showAllProducts) {
            currentLineItems.take(COLLAPSED_PRODUCT_LIMIT)
        } else {
            currentLineItems
        }
        visibleItems.forEach { item ->
            val itemBinding = ItemQuoteReportLineItemBinding.inflate(
                layoutInflater,
                binding.lineItemsContainer,
                false
            )
            itemBinding.lineNumberText.text = item.lineNumber
            itemBinding.productNameText.text = item.name
            itemBinding.descriptionText.text = item.description
            itemBinding.quantityText.text = item.quantity
            itemBinding.unitPriceText.text = item.unitPrice
            itemBinding.lineTotalText.text = item.lineTotal
            itemBinding.mfgPartNoText.text = item.mfgPartNo
            bindRows(
                itemBinding.productDetailsContainer,
                listOf(
                    QuoteReportFieldUiModel(getString(R.string.quote_report_manufacturer), item.manufacturer),
                    QuoteReportFieldUiModel(getString(R.string.quote_report_axe_part_no), item.axePartNo),
                    QuoteReportFieldUiModel(getString(R.string.quote_report_duration), item.duration)
                )
            )
            val key = item.lineNumber
            val isExpanded = key in expandedProductKeys
            itemBinding.expandedDetailsContainer.isVisible = isExpanded
            itemBinding.detailsToggleButton.setText("")
            itemBinding.detailsToggleButton.setIconResource(
                if (isExpanded) R.drawable.ic_chevron_up else R.drawable.ic_chevron_down
            )
            itemBinding.detailsToggleButton.contentDescription = getString(
                if (isExpanded) R.string.quote_report_hide_details else R.string.quote_report_view_details
            )
            itemBinding.detailsToggleButton.setOnClickListener {
                if (key in expandedProductKeys) {
                    expandedProductKeys.remove(key)
                } else {
                    expandedProductKeys.add(key)
                }
                renderLineItems()
            }
            binding.lineItemsContainer.addView(itemBinding.root)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private companion object {
        const val COLLAPSED_PRODUCT_LIMIT = 5
    }

    private enum class ReportSection {
        Items,
        Info
    }
}

private fun List<QuoteReportFieldUiModel>.fieldValue(label: String, fallback: String = "-"): String {
    return firstOrNull { it.label.equals(label, ignoreCase = true) }
        ?.value
        ?.takeIf { it.isNotBlank() && it != "-" }
        ?: fallback
}
