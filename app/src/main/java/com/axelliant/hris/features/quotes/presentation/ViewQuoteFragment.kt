package com.axelliant.hris.features.quotes.presentation

import android.os.Bundle
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
import androidx.recyclerview.widget.LinearLayoutManager
import com.axelliant.hris.R
import com.axelliant.hris.core.ui.ShimmerAnimatorHelper
import com.axelliant.hris.core.ui.UiState
import com.axelliant.hris.databinding.FragmentViewQuoteBinding
import com.axelliant.hris.databinding.LayoutQuotePreviewFieldRowBinding
import com.axelliant.hris.databinding.LayoutQuotePreviewLabeledCellBinding
import com.axelliant.hris.features.quotes.domain.model.QuotePreviewUiModel
import com.axelliant.hris.features.quotes.domain.model.QuoteStatus
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ViewQuoteFragment : Fragment() {

    private val viewModel: ViewQuoteViewModel by viewModels()
    private var _binding: FragmentViewQuoteBinding? = null
    private val binding get() = _binding!!

    private val shimmerHelper = ShimmerAnimatorHelper()
    private val productAdapter = QuotePreviewProductAdapter()
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentViewQuoteBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupInteractions()
        setupAmountRows()
        observePreview()
        observeSubmitState()
        observeWorkflowState()
    }

    private fun setupRecyclerView() {
        binding.productsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = productAdapter
            isNestedScrollingEnabled = false
        }
    }

    private fun setupAmountRows() {
        binding.subtotalRow.amountLabel.setText(R.string.quote_preview_subtotal)
        binding.taxRow.amountLabel.setText(R.string.quote_preview_tax)
        binding.taxRow.taxInfoIcon.isVisible = true
        binding.shippingRow.amountLabel.setText(R.string.quote_preview_shipping)
    }

    private fun setupInteractions() {
        binding.backButton.setOnClickListener { findNavController().navigateUp() }
        binding.infoTab.setOnClickListener { selectTab(PreviewTab.Info) }
        binding.itemsTab.setOnClickListener { selectTab(PreviewTab.Items) }
        binding.detailsTab.setOnClickListener { selectTab(PreviewTab.Details) }
        binding.viewAllButton.setOnClickListener { showComingSoon() }
        binding.saveDraftButton.setOnClickListener { showComingSoon() }
        binding.submitQuoteButton.setOnClickListener { viewModel.onPrimaryAction() }
    }

    private fun observeWorkflowState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.workflowState.collect { state ->
                    when (state) {
                        UiState.Loading -> setSubmitActionsEnabled(false)
                        is UiState.Success -> {
                            setSubmitActionsEnabled(true)
                            QuoteWorkflowBottomSheet(
                                fragment = this@ViewQuoteFragment,
                                workflow = state.data
                            ).show()
                            viewModel.resetWorkflowState()
                        }
                        is UiState.Error -> {
                            setSubmitActionsEnabled(true)
                            Toast.makeText(
                                requireContext(),
                                state.message.ifBlank {
                                    getString(R.string.quote_workflow_load_failed)
                                },
                                Toast.LENGTH_LONG
                            ).show()
                            viewModel.resetWorkflowState()
                        }
                        UiState.Unauthorized -> {
                            setSubmitActionsEnabled(true)
                            Toast.makeText(
                                requireContext(),
                                R.string.quotes_error,
                                Toast.LENGTH_SHORT
                            ).show()
                            viewModel.resetWorkflowState()
                        }
                        else -> Unit
                    }
                }
            }
        }
    }

    private fun observeSubmitState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.submitState.collect { state ->
                    when (state) {
                        UiState.Loading -> setSubmitActionsEnabled(false)
                        is UiState.Success -> {
                            setSubmitActionsEnabled(true)
                            showSubmitSuccess(state.data.message)
                        }
                        is UiState.Error -> {
                            setSubmitActionsEnabled(true)
                            Toast.makeText(
                                requireContext(),
                                state.message.ifBlank { getString(R.string.quote_submit_failed) },
                                Toast.LENGTH_LONG
                            ).show()
                            viewModel.resetSubmitState()
                        }
                        UiState.Unauthorized -> {
                            setSubmitActionsEnabled(true)
                            Toast.makeText(
                                requireContext(),
                                R.string.quotes_error,
                                Toast.LENGTH_SHORT
                            ).show()
                            viewModel.resetSubmitState()
                        }
                        else -> setSubmitActionsEnabled(true)
                    }
                }
            }
        }
    }

    private fun setSubmitActionsEnabled(enabled: Boolean) {
        binding.submitQuoteButton.isEnabled = enabled
        binding.saveDraftButton.isEnabled = enabled
    }

    private fun showSubmitSuccess(message: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setMessage(
                message.ifBlank { getString(R.string.quote_submit_success) }
            )
            .setPositiveButton(android.R.string.ok) { _, _ ->
                findNavController().previousBackStackEntry?.savedStateHandle
                    ?.set(ViewQuoteViewModel.RESULT_QUOTE_SUBMITTED, true)
                viewModel.resetSubmitState()
                findNavController().popBackStack(R.id.iaQuotesFragment, false)
            }
            .setCancelable(false)
            .show()
    }

    private fun observePreview() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.previewState.collect { state ->
                    when (state) {
                        UiState.Loading -> showLoading()
                        is UiState.Success -> showSuccess(state.data)
                        is UiState.Error -> showError(state.message)
                        UiState.Unauthorized -> showError(getString(R.string.quotes_error))
                        else -> Unit
                    }
                }
            }
        }
    }

    private fun showLoading() {
        binding.contentScroll.isVisible = false
        binding.errorStateText.isVisible = false
        binding.bottomPanel.isVisible = false
        binding.shimmerScroll.isVisible = true
        shimmerHelper.populate(
            container = binding.shimmerContainer,
            inflater = layoutInflater,
            itemLayoutRes = R.layout.item_quote_preview_shimmer,
            count = SHIMMER_BLOCK_COUNT
        )
    }

    private fun showSuccess(preview: QuotePreviewUiModel) {
        shimmerHelper.clear(binding.shimmerContainer)
        binding.shimmerScroll.isVisible = false
        binding.errorStateText.isVisible = false
        binding.contentScroll.isVisible = true
        binding.bottomPanel.isVisible = true
        bindPreview(preview)
    }

    private fun showError(message: String) {
        shimmerHelper.clear(binding.shimmerContainer)
        binding.shimmerScroll.isVisible = false
        binding.contentScroll.isVisible = false
        binding.bottomPanel.isVisible = false
        binding.errorStateText.isVisible = true
        binding.errorStateText.text = message.ifBlank { getString(R.string.quote_preview_error) }
    }

    private fun bindPreview(preview: QuotePreviewUiModel) {
        val statusUi = QuoteStatusUiMapper.mapApprovalStatusLabel(preview.statusLabel)
        binding.quoteTypeLabel.text = preview.quoteTypeLabel
        binding.quoteNumberText.text = getString(R.string.quote_preview_number_format, preview.quoteNumber)
        binding.statusBadge.apply {
            text = preview.statusLabel
            setBackgroundResource(statusUi.backgroundRes)
            setTextColor(ContextCompat.getColor(requireContext(), statusUi.textColorRes))
        }

        bindCell(binding.createdDateCell, R.string.quote_preview_created_date, preview.createdDate)
        bindCell(binding.createdByCell, R.string.quotes_created_by, preview.createdBy)
        bindCell(binding.emailCell, R.string.quote_preview_email_id, preview.email)
        bindCell(binding.aeCell, R.string.quote_preview_ae, preview.ae)

        bindField(binding.customerNameField, R.string.customer_name_label, preview.customerName, showDivider = false)
        bindField(binding.paymentTermsField, R.string.quote_preview_payment_terms, preview.paymentTerms)
        bindField(binding.billingField, R.string.quote_preview_billing_details, preview.billingDetails)
        bindField(binding.shippingField, R.string.quote_preview_shipping_details, preview.shippingDetails)

        binding.productsTitle.text = getString(R.string.quote_preview_products_format, preview.products.size)
        productAdapter.submitList(preview.products)

        binding.commentsEmptyState.isVisible = !preview.hasComments

        binding.subtotalRow.amountValue.text = preview.subtotal
        binding.taxRow.amountValue.text = preview.tax
        binding.shippingRow.amountValue.text = preview.shipping
        binding.grandTotalValue.text = preview.grandTotal

        bindPrimaryAction(preview)
    }

    private fun bindPrimaryAction(preview: QuotePreviewUiModel) {
        val showsWorkflow = preview.status == QuoteStatus.Approved ||
            preview.status == QuoteStatus.Submitted
        val isDraft = preview.status == QuoteStatus.Draft

        binding.submitQuoteButton.setText(
            if (showsWorkflow) {
                R.string.quote_preview_open_workflow
            } else {
                R.string.quote_preview_submit
            }
        )
        binding.saveDraftButton.isVisible = isDraft
        (binding.submitQuoteButton.layoutParams as LinearLayout.LayoutParams).apply {
            if (showsWorkflow || !isDraft) {
                width = ViewGroup.LayoutParams.MATCH_PARENT
                weight = 0f
                marginStart = 0
            } else {
                width = 0
                weight = 1f
                marginStart = resources.getDimensionPixelSize(R.dimen.ds_space_6)
            }
        }
    }

    private fun bindCell(cell: LayoutQuotePreviewLabeledCellBinding, labelRes: Int, value: String) {
        cell.cellLabel.setText(labelRes)
        cell.cellValue.text = value
    }

    private fun bindField(
        field: LayoutQuotePreviewFieldRowBinding,
        labelRes: Int,
        value: String,
        showDivider: Boolean = true
    ) {
        field.fieldLabel.setText(labelRes)
        field.fieldValue.text = value
        field.fieldDivider.isVisible = showDivider
    }

    private fun selectTab(tab: PreviewTab) {
        val context = requireContext()
        binding.infoTab.apply {
            setBackgroundResource(if (tab == PreviewTab.Info) R.drawable.bg_quote_tab_selected else 0)
            setTextColor(
                ContextCompat.getColor(
                    context,
                    if (tab == PreviewTab.Info) R.color.ds_text_primary else R.color.ds_text_secondary
                )
            )
        }
        binding.itemsTab.apply {
            setBackgroundResource(if (tab == PreviewTab.Items) R.drawable.bg_quote_tab_selected else 0)
            setTextColor(
                ContextCompat.getColor(
                    context,
                    if (tab == PreviewTab.Items) R.color.ds_text_primary else R.color.ds_text_secondary
                )
            )
        }
        binding.detailsTab.apply {
            setBackgroundResource(if (tab == PreviewTab.Details) R.drawable.bg_quote_tab_selected else 0)
            setTextColor(
                ContextCompat.getColor(
                    context,
                    if (tab == PreviewTab.Details) R.color.ds_text_primary else R.color.ds_text_secondary
                )
            )
        }
    }

    private fun showComingSoon() {
        Toast.makeText(requireContext(), R.string.coming_soon, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        shimmerHelper.release()
        super.onDestroyView()
        _binding = null
    }

    private enum class PreviewTab {
        Info,
        Items,
        Details
    }

    companion object {
        private const val SHIMMER_BLOCK_COUNT = 1
    }
}


