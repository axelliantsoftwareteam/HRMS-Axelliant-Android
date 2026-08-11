package com.axelliant.hris.features.saleorders.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import com.axelliant.hris.core.ui.UiState
import com.axelliant.hris.databinding.FragmentViewSaleOrderBinding
import com.axelliant.hris.features.saleorders.domain.model.SaleOrderDetailModel
import com.axelliant.hris.features.saleorders.domain.model.SaleOrderStatus
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ViewSaleOrderFragment : Fragment() {

    private val viewModel: ViewSaleOrderViewModel by viewModels()
    private var _binding: FragmentViewSaleOrderBinding? = null
    private val binding get() = _binding!!

    private lateinit var productsAdapter: SaleOrderProductLineAdapter
    private var quoteDetailExpanded = false
    private var dealRegistrationExpanded = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentViewSaleOrderBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupProductsList()
        setupInteractions()
        observeDetail()
        observeSubmit()
    }

    private fun setupProductsList() {
        productsAdapter = SaleOrderProductLineAdapter()
        binding.productsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = productsAdapter
            setHasFixedSize(false)
        }
    }

    private fun setupInteractions() {
        binding.appTopBar.setOnBackClickListener { findNavController().navigateUp() }
        binding.quoteDetailHeader.setOnClickListener {
            quoteDetailExpanded = !quoteDetailExpanded
            updateQuoteDetailExpandedState()
        }
        binding.dealRegistrationHeader.setOnClickListener {
            dealRegistrationExpanded = !dealRegistrationExpanded
            updateDealRegistrationExpandedState()
        }
        binding.changeDateButton.isVisible = false
        binding.submitOrderButton.setOnClickListener { viewModel.submitOrder() }
        updateQuoteDetailExpandedState()
        updateDealRegistrationExpandedState()
    }

    private fun observeDetail() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.detailState.collect { state ->
                    when (state) {
                        UiState.Loading -> showLoading()
                        is UiState.Success -> showDetail(state.data)
                        is UiState.Error -> showError(state.message)
                        UiState.Unauthorized -> showError(getString(R.string.sale_order_details_error))
                        else -> Unit
                    }
                }
            }
        }
    }

    private fun observeSubmit() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.submitState.collect { state ->
                    when (state) {
                        UiState.Loading -> binding.submitOrderButton.isEnabled = false
                        is UiState.Success -> {
                            Toast.makeText(requireContext(), state.data, Toast.LENGTH_SHORT).show()
                            viewModel.onSubmitHandled()
                            viewModel.loadDetail()
                        }
                        is UiState.Error -> {
                            binding.submitOrderButton.isEnabled = true
                            Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                            viewModel.onSubmitHandled()
                        }
                        UiState.Unauthorized -> {
                            binding.submitOrderButton.isEnabled = true
                            Toast.makeText(
                                requireContext(),
                                R.string.sale_order_details_error,
                                Toast.LENGTH_SHORT
                            ).show()
                            viewModel.onSubmitHandled()
                        }
                        else -> Unit
                    }
                }
            }
        }
    }

    private fun showLoading() {
        binding.loadingProgress.isVisible = true
        binding.contentScroll.isVisible = false
        binding.bottomActionBar.isVisible = false
        binding.errorStateText.isVisible = false
    }

    private fun showError(message: String) {
        binding.loadingProgress.isVisible = false
        binding.contentScroll.isVisible = false
        binding.bottomActionBar.isVisible = false
        binding.errorStateText.isVisible = true
        binding.errorStateText.text = message.ifBlank {
            getString(R.string.sale_order_details_error)
        }
    }

    private fun showDetail(detail: SaleOrderDetailModel) {
        binding.loadingProgress.isVisible = false
        binding.errorStateText.isVisible = false
        binding.contentScroll.isVisible = true
        binding.bottomActionBar.isVisible = true

        val statusUi = SaleOrderStatusUiMapper.mapStatus(detail.status)
        binding.orderNumberText.text = detail.orderNumber
        binding.createdDateText.text = getString(R.string.sale_order_created_format, detail.createdDate)
        binding.statusBadge.text = getString(statusUi.labelRes)
        binding.statusBadge.setBackgroundResource(statusUi.backgroundRes)
        binding.statusBadge.setTextColor(
            ContextCompat.getColor(requireContext(), statusUi.textColorRes)
        )
        binding.assigneeNameText.text = detail.assigneeName
        binding.customerNameValue.text = detail.customerName
        binding.paymentTermsValue.text = detail.paymentTerms
        binding.billingAddressValue.text = detail.billingAddress
        binding.shippingAddressValue.text = detail.shippingAddress

        val itemCount = detail.products.size
        binding.productsCountText.text = if (itemCount == 1) {
            getString(R.string.sale_order_items_count, itemCount)
        } else {
            getString(R.string.sale_order_items_count_plural, itemCount)
        }
        productsAdapter.submitList(detail.products)

        binding.deliveryDateValue.text = detail.deliveryDate
        binding.validityValue.text = detail.validity
        binding.expiresOnValue.text = detail.expiresOn
        binding.registrationIdValue.text = detail.dealRegistrationId
        binding.dealStatusValue.text = detail.dealRegistrationStatus
        binding.dealDocumentValue.text = detail.dealRegistrationDocument
        binding.subtotalValue.text = detail.subtotal
        binding.taxValue.text = detail.tax
        binding.shippingValue.text = detail.shipping
        binding.grandTotalValue.text = detail.grandTotal
        renderActionButtons(detail)
    }

    private fun renderActionButtons(detail: SaleOrderDetailModel) {
        val canSubmit = detail.status == SaleOrderStatus.DRAFT ||
            detail.status == SaleOrderStatus.PENDING
        binding.changeDateButton.isVisible = false
        binding.submitOrderButton.isVisible = canSubmit
        binding.bottomActionBar.isVisible = canSubmit
        binding.submitOrderButton.isEnabled = viewModel.submitState.value !is UiState.Loading
    }

    private fun updateQuoteDetailExpandedState() {
        binding.quoteDetailContent.isVisible = quoteDetailExpanded
        binding.quoteDetailChevron.rotation = if (quoteDetailExpanded) 0f else 180f
    }

    private fun updateDealRegistrationExpandedState() {
        binding.dealRegistrationContent.isVisible = dealRegistrationExpanded
        binding.dealRegistrationChevron.rotation = if (dealRegistrationExpanded) 0f else 180f
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
