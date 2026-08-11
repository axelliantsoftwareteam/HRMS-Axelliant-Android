package com.axelliant.hris.features.purchaseorders.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import com.axelliant.hris.databinding.FragmentViewPurchaseOrderBinding
import com.axelliant.hris.features.purchaseorders.domain.model.PurchaseOrderDetailModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ViewPurchaseOrderFragment : Fragment() {

    private val viewModel: ViewPurchaseOrderViewModel by viewModels()
    private var _binding: FragmentViewPurchaseOrderBinding? = null
    private val binding get() = _binding!!

    private lateinit var productsAdapter: PurchaseOrderProductLineAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentViewPurchaseOrderBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupProductsList()
        binding.appTopBar.setOnBackClickListener { findNavController().navigateUp() }
        observeDetail()
    }

    private fun setupProductsList() {
        productsAdapter = PurchaseOrderProductLineAdapter()
        binding.productsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = productsAdapter
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
                        UiState.Unauthorized -> showError(getString(R.string.po_details_error))
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
            getString(R.string.po_details_error)
        }
    }

    private fun showDetail(detail: PurchaseOrderDetailModel) {
        binding.loadingProgress.isVisible = false
        binding.errorStateText.isVisible = false
        binding.contentScroll.isVisible = true

        val utilizationUi = PurchaseOrderStatusUiMapper.mapUtilization(detail.utilization)
        binding.utilizationBadge.text = getString(utilizationUi.labelRes)
        binding.poNumberText.text = detail.poNumber
        binding.totalAmountValue.text = detail.grandTotal
        binding.vendorValue.text = detail.vendorName

        val fulfillmentUi = PurchaseOrderStatusUiMapper.mapStatus(detail.fulfillmentStatus)
        binding.fulfillmentStatusValue.text = getString(fulfillmentUi.labelRes)
        binding.createdByValue.text = detail.createdBy
        binding.createdDateValue.text = detail.createdDate
        binding.billingAddressValue.text = detail.billingAddress
        binding.shippingAddressValue.text = detail.shippingAddress

        val itemCount = detail.products.size
        binding.productsCountBadge.text = if (itemCount == 1) {
            getString(R.string.po_details_items_count, itemCount)
        } else {
            getString(R.string.po_details_items_count_plural, itemCount)
        }
        productsAdapter.submitList(detail.products)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
