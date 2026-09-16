package com.axelliant.hris.features.warehouse.presentation

import android.content.res.ColorStateList
import android.os.Bundle
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
import androidx.recyclerview.widget.LinearLayoutManager
import com.axelliant.hris.R
import com.axelliant.hris.core.ui.UiState
import com.axelliant.hris.databinding.FragmentWarehouseReceivingDetailBinding
import com.axelliant.hris.features.warehouse.domain.model.WarehouseReceiptDetailModel
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Locale
import kotlinx.coroutines.launch

@AndroidEntryPoint
class WarehouseReceivingDetailFragment : Fragment() {

    private val viewModel: WarehouseReceivingDetailViewModel by viewModels()
    private var _binding: FragmentWarehouseReceivingDetailBinding? = null
    private val binding get() = _binding!!
    private val linesAdapter = WarehouseReceivingLineAdapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWarehouseReceivingDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.appTopBar.setOnBackClickListener { findNavController().navigateUp() }
        binding.closeButton.setOnClickListener { findNavController().navigateUp() }
        setupLinesList()
        observeDetail()
    }

    private fun setupLinesList() {
        binding.linesRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = linesAdapter
            isNestedScrollingEnabled = false
            setHasFixedSize(false)
        }
    }

    private fun observeDetail() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.detailState.collect { state ->
                    when (state) {
                        UiState.Loading -> showLoading()
                        is UiState.Success -> showDetail(state.data)
                        is UiState.Error -> showError(state.message)
                        UiState.Unauthorized -> showError(getString(R.string.receiving_detail_error))
                        else -> Unit
                    }
                }
            }
        }
    }

    private fun showLoading() {
        binding.loadingProgress.isVisible = true
        binding.contentScroll.isVisible = false
        binding.errorStateText.isVisible = false
    }

    private fun showError(message: String) {
        binding.loadingProgress.isVisible = false
        binding.contentScroll.isVisible = false
        binding.errorStateText.isVisible = true
        binding.errorStateText.text = message.ifBlank {
            getString(R.string.receiving_detail_error)
        }
    }

    private fun showDetail(detail: WarehouseReceiptDetailModel) = with(binding) {
        loadingProgress.isVisible = false
        errorStateText.isVisible = false
        contentScroll.isVisible = true

        val receipt = detail.receipt
        appTopBar.setTitle(getString(R.string.receiving_detail_title_format, receipt.receiptNumber))
        receiptNumberField.fieldLabel.text = getString(R.string.receiving_detail_receipt_number)
        receiptNumberField.fieldValue.text = receipt.receiptNumber.displayValue()
        warehouseField.fieldLabel.text = getString(R.string.warehouse_label)
        warehouseField.fieldValue.text = receipt.warehouseName.displayValue()
        receiptTypeField.fieldLabel.text = getString(R.string.receiving_filter_type)
        receiptTypeField.fieldValue.text = receipt.receiptTypeName.displayValue()
        purchaseOrderField.fieldLabel.text = getString(R.string.receiving_filter_purchase_order)
        purchaseOrderField.fieldValue.text = receipt.poNumber.displayValue()
        expectedDateField.fieldLabel.text = getString(R.string.receiving_detail_expected_date)
        expectedDateField.fieldValue.text = getString(R.string.warehouse_not_available)
        receivedDateField.fieldLabel.text = getString(R.string.receiving_received_date_label)
        receivedDateField.fieldValue.text = receipt.receivedDate.formatApiDate()
        statusBadge.text = receipt.receiptStatusName.displayValue()
        tintStatusBadge(receipt.receiptStatus)
        linesCountBadge.text = resources.getQuantityString(
            R.plurals.receiving_detail_item_count,
            detail.lines.size,
            detail.lines.size
        )
        linesAdapter.submitList(detail.lines)
    }

    private fun tintStatusBadge(status: Int?) {
        val isComplete = status == COMPLETED_STATUS
        binding.statusBadge.setBackgroundResource(
            if (isComplete) R.drawable.bg_quote_status_approved else R.drawable.bg_quote_status_submitted
        )
        binding.statusBadge.backgroundTintList = ColorStateList.valueOf(
            ContextCompat.getColor(
                requireContext(),
                if (isComplete) R.color.quotes_status_approved_bg else R.color.quotes_status_submitted_bg
            )
        )
        binding.statusBadge.setTextColor(
            ContextCompat.getColor(
                requireContext(),
                if (isComplete) R.color.quotes_status_approved_text else R.color.quotes_status_submitted_text
            )
        )
    }

    private fun String.displayValue(): String = takeIf { it.isNotBlank() && it != "-" }
        ?: getString(R.string.warehouse_not_available)

    private fun String.formatApiDate(): String {
        if (isBlank() || this == "-") return getString(R.string.warehouse_not_available)
        return runCatching {
            val input = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val output = SimpleDateFormat("MMM d, yyyy", Locale.US)
            output.format(input.parse(this)!!)
        }.getOrElse { this }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private companion object {
        const val COMPLETED_STATUS = 7
    }
}
