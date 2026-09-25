package com.axelliant.hris.features.purchaseorders.presentation

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
import com.axelliant.hris.databinding.BottomSheetPurchaseOrderHistoryBinding
import com.axelliant.hris.features.purchaseorders.domain.model.PurchaseOrderHistoryItemUiModel
import com.axelliant.hris.ui.designsystem.components.createAppBottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog

class PurchaseOrderHistoryBottomSheet(
    private val fragment: Fragment
) {
    private var dialog: BottomSheetDialog? = null
    private var binding: BottomSheetPurchaseOrderHistoryBinding? = null
    private val adapter = PurchaseOrderHistoryAdapter()
    private val shimmerHelper = ShimmerAnimatorHelper()

    fun show() {
        if (dialog?.isShowing == true) return

        val inflater = LayoutInflater.from(fragment.requireContext())
        val sheetBinding = BottomSheetPurchaseOrderHistoryBinding.inflate(inflater)
        binding = sheetBinding

        val sheetDialog = fragment.requireContext().createAppBottomSheetDialog()
        dialog = sheetDialog

        sheetBinding.closeButton.setOnClickListener { sheetDialog.dismiss() }
        sheetBinding.historyRecyclerView.apply {
            layoutManager = LinearLayoutManager(fragment.requireContext())
            adapter = this@PurchaseOrderHistoryBottomSheet.adapter
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

    fun render(state: UiState<List<PurchaseOrderHistoryItemUiModel>>) {
        val sheetBinding = binding ?: return
        when (state) {
            UiState.Loading -> showLoading(sheetBinding)
            is UiState.Success -> showHistory(sheetBinding, state.data)
            UiState.Empty -> showHistory(sheetBinding, emptyList())
            else -> Unit
        }
    }

    fun dismiss() {
        dialog?.dismiss()
    }

    private fun showLoading(binding: BottomSheetPurchaseOrderHistoryBinding) {
        adapter.submitList(emptyList())
        binding.historyRecyclerView.isVisible = false
        binding.historyEmptyContainer.isVisible = false
        binding.historyFooter.isVisible = false
        binding.historyShimmerContainer.isVisible = true
        shimmerHelper.populate(
            container = binding.historyShimmerContainer,
            inflater = LayoutInflater.from(fragment.requireContext()),
            itemLayoutRes = R.layout.item_purchase_order_history_shimmer,
            count = HISTORY_SHIMMER_COUNT
        )
    }

    private fun showHistory(
        binding: BottomSheetPurchaseOrderHistoryBinding,
        items: List<PurchaseOrderHistoryItemUiModel>
    ) {
        shimmerHelper.clear(binding.historyShimmerContainer)
        binding.historyShimmerContainer.isVisible = false
        binding.historyRecyclerView.isVisible = items.isNotEmpty()
        binding.historyEmptyContainer.isVisible = items.isEmpty()
        binding.historyFooter.isVisible = items.isNotEmpty()
        adapter.submitList(items)
    }

    companion object {
        private const val HISTORY_SHIMMER_COUNT = 4
    }
}
