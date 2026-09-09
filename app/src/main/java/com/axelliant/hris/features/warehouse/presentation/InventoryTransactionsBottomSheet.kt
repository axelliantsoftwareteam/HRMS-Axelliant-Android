package com.axelliant.hris.features.warehouse.presentation

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.axelliant.hris.R
import com.axelliant.hris.core.ui.ShimmerAnimatorHelper
import com.axelliant.hris.core.ui.UiState
import com.axelliant.hris.databinding.BottomSheetInventoryTransactionsBinding
import com.axelliant.hris.features.warehouse.domain.model.InventoryModel
import com.axelliant.hris.ui.designsystem.components.createAppBottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog

class InventoryTransactionsBottomSheet(
    private val fragment: Fragment
) {
    private var dialog: BottomSheetDialog? = null
    private var binding: BottomSheetInventoryTransactionsBinding? = null
    private val adapter = InventoryTransactionsAdapter()
    private val shimmerHelper = ShimmerAnimatorHelper()

    fun show(inventory: InventoryModel) {
        if (dialog?.isShowing == true) return

        val inflater = LayoutInflater.from(fragment.requireContext())
        val sheetBinding = BottomSheetInventoryTransactionsBinding.inflate(inflater)
        binding = sheetBinding
        sheetBinding.productNameText.text = inventory.productName
        sheetBinding.transactionCountText.text = fragment.resources.getQuantityString(
            R.plurals.inventory_transactions_count,
            0,
            0
        )

        val sheetDialog = fragment.requireContext().createAppBottomSheetDialog()
        dialog = sheetDialog

        sheetBinding.closeButton.setOnClickListener { sheetDialog.dismiss() }
        sheetBinding.transactionsRecyclerView.apply {
            layoutManager = LinearLayoutManager(fragment.requireContext())
            adapter = this@InventoryTransactionsBottomSheet.adapter
            clipToPadding = false
        }

        sheetDialog.setContentView(sheetBinding.root)
        sheetDialog.window?.let { window ->
            WindowCompat.setDecorFitsSystemWindows(window, false)
            window.navigationBarColor = ContextCompat.getColor(
                fragment.requireContext(),
                R.color.ds_surface
            )
        }
        sheetDialog.setOnDismissListener {
            shimmerHelper.release()
            binding = null
            dialog = null
        }
        sheetDialog.setOnShowListener {
            val bottomSheet = sheetDialog.findViewById<android.widget.FrameLayout>(
                com.google.android.material.R.id.design_bottom_sheet
            ) ?: return@setOnShowListener

            bottomSheet.background = ContextCompat.getDrawable(
                fragment.requireContext(),
                R.drawable.bg_filter_sheet
            )
            bottomSheet.layoutParams = bottomSheet.layoutParams.apply {
                height = ViewGroup.LayoutParams.MATCH_PARENT
            }
            BottomSheetBehavior.from(bottomSheet).apply {
                state = BottomSheetBehavior.STATE_EXPANDED
                skipCollapsed = true
            }

            ViewCompat.setOnApplyWindowInsetsListener(sheetBinding.root) { view, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                view.updatePadding(bottom = systemBars.bottom)
                insets
            }
        }
        sheetDialog.show()
    }

    fun render(state: UiState<InventoryTransactionsUiModel>) {
        val sheetBinding = binding ?: return
        when (state) {
            UiState.Loading -> showLoading(sheetBinding)
            is UiState.Success -> showTransactions(sheetBinding, state.data)
            is UiState.Error -> showError(sheetBinding, state.message)
            else -> Unit
        }
    }

    fun dismiss() {
        dialog?.dismiss()
    }

    private fun showLoading(binding: BottomSheetInventoryTransactionsBinding) {
        adapter.submitList(emptyList())
        binding.transactionsRecyclerView.isVisible = false
        binding.transactionsEmptyContainer.isVisible = false
        binding.transactionsErrorText.isVisible = false
        binding.transactionsFooter.isVisible = false
        binding.transactionsShimmerContainer.isVisible = true
        binding.transactionCountText.text = fragment.resources.getQuantityString(
            R.plurals.inventory_transactions_count,
            0,
            0
        )
        shimmerHelper.populate(
            container = binding.transactionsShimmerContainer,
            inflater = LayoutInflater.from(fragment.requireContext()),
            itemLayoutRes = R.layout.item_purchase_order_history_shimmer,
            count = TRANSACTIONS_SHIMMER_COUNT
        )
    }

    private fun showTransactions(
        binding: BottomSheetInventoryTransactionsBinding,
        data: InventoryTransactionsUiModel
    ) {
        shimmerHelper.clear(binding.transactionsShimmerContainer)
        binding.transactionsShimmerContainer.isVisible = false
        binding.productNameText.text = data.inventory.productName
        binding.transactionCountText.text = fragment.resources.getQuantityString(
            R.plurals.inventory_transactions_count,
            data.totalCount,
            data.totalCount
        )
        binding.transactionsErrorText.isVisible = false
        binding.transactionsRecyclerView.isVisible = data.transactions.isNotEmpty()
        binding.transactionsEmptyContainer.isVisible = data.transactions.isEmpty()
        binding.transactionsFooter.isVisible = data.transactions.isNotEmpty()
        adapter.submitList(data.transactions)
    }

    private fun showError(
        binding: BottomSheetInventoryTransactionsBinding,
        message: String
    ) {
        shimmerHelper.clear(binding.transactionsShimmerContainer)
        adapter.submitList(emptyList())
        binding.transactionsShimmerContainer.isVisible = false
        binding.transactionsRecyclerView.isVisible = false
        binding.transactionsEmptyContainer.isVisible = false
        binding.transactionsFooter.isVisible = false
        binding.transactionsErrorText.isVisible = true
        binding.transactionsErrorText.text = message.ifBlank {
            fragment.getString(R.string.inventory_transactions_error)
        }
    }

    companion object {
        private const val TRANSACTIONS_SHIMMER_COUNT = 4
    }
}
