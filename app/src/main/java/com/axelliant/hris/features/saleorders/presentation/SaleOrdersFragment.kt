package com.axelliant.hris.features.saleorders.presentation

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.axelliant.hris.R
import com.axelliant.hris.core.extensions.hideKeyboard
import com.axelliant.hris.core.extensions.showKeyboard
import com.axelliant.hris.core.ui.ShimmerAnimatorHelper
import com.axelliant.hris.core.ui.UiState
import com.axelliant.hris.databinding.FragmentSaleOrdersBinding
import com.axelliant.hris.features.internalapps.navigation.InternalAppsNavigator
import com.axelliant.hris.features.quotes.presentation.QuoteWorkflowBottomSheet
import com.axelliant.hris.features.saleorders.domain.model.SaleOrderListUiModel
import com.axelliant.hris.features.saleorders.domain.model.SaleOrderModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.view.animation.DecelerateInterpolator
import androidx.activity.OnBackPressedCallback

@AndroidEntryPoint
class SaleOrdersFragment : Fragment() {

    private val viewModel: SaleOrdersViewModel by viewModels()
    private var _binding: FragmentSaleOrdersBinding? = null
    private val binding get() = _binding!!

    private val shimmerHelper = ShimmerAnimatorHelper()
    private lateinit var ordersAdapter: SaleOrdersAdapter
    private lateinit var filterChipAdapter: SaleOrderFilterChipAdapter
    private lateinit var actionMenuHandler: SaleOrderActionMenuHandler

    private var isSearchVisible = false
    private var isFabMenuOpen = false
    private lateinit var fabMenuBackCallback: OnBackPressedCallback

    private var workflowBottomSheet: QuoteWorkflowBottomSheet? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSaleOrdersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupSearchUi()
        setupAdapters()
        setupInteractions()
        setupPagination()
        observeOrders()
        setupFabMenu()
        observeSaleOrderWorkflow()
        observeSaleOrderDecisionWorkflow()
        viewModel.loadOrdersIfNeeded()
    }

    private fun setupSearchUi() {
        isSearchVisible = false
        binding.searchInputLayout.isVisible = false
        binding.searchInputLayout.alpha = 0f
        binding.appTopBar.setSearchIconResource(R.drawable.ia_ic_search)
        binding.appTopBar.searchButton.contentDescription =
            getString(R.string.sale_orders_search_open)
    }

    private fun setupAdapters() {
        actionMenuHandler = SaleOrderActionMenuHandler { actionId, order ->
            when (actionId) {
                R.id.action_sale_order_view -> navigateToViewSaleOrder(order)
                R.id.action_sale_order_edit -> navigateToEditSaleOrder(order)
                R.id.action_sale_order_submit_workflow -> openSaleOrderWorkflow(order)
                R.id.action_sale_order_show_report -> {
                    Toast.makeText(requireContext(), R.string.coming_soon, Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }
        ordersAdapter = SaleOrdersAdapter { order, anchor ->
            actionMenuHandler.show(anchor, order)
        }
        binding.saleOrdersRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = ordersAdapter
            setHasFixedSize(false)
        }

        filterChipAdapter = SaleOrderFilterChipAdapter { chip ->
            if (isSearchVisible) {
                closeSearchField(clearText = true, reload = false)
            }
            viewModel.onFilterChipSelected(chip)
        }
        binding.statusFilterRecycler.apply {
            layoutManager =
                LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = filterChipAdapter
        }
    }

    private fun setupPagination() {
        binding.saleOrdersRecyclerView.addOnScrollListener(object :
            RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (dy <= 0) return
                val layoutManager = recyclerView.layoutManager as? LinearLayoutManager ?: return
                val lastVisible = layoutManager.findLastVisibleItemPosition()
                val totalItems = layoutManager.itemCount
                if (lastVisible >= totalItems - PAGINATION_THRESHOLD_ITEMS) {
                    viewModel.loadNextPage()
                }
            }
        })
    }

    private fun setupInteractions() {
        binding.appTopBar.setOnBackClickListener {
            if (isFabMenuOpen) {
                closeFabMenu()
            } else {
                InternalAppsNavigator.returnToHomeShell(findNavController())
            }
        }
        binding.appTopBar.setOnSearchClickListener { toggleSearchField() }
        binding.searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) =
                Unit

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(editable: Editable?) {
                if (!isSearchVisible) return
                viewModel.onSearchQueryChanged(editable?.toString().orEmpty())
            }
        })

        binding.searchEditText.setOnEditorActionListener { _, actionId, event ->
            val isSubmitAction = actionId == EditorInfo.IME_ACTION_SEARCH ||
                    actionId == EditorInfo.IME_ACTION_DONE
            val isEnterKey = event?.keyCode == KeyEvent.KEYCODE_ENTER &&
                    event.action == KeyEvent.ACTION_UP
            if (isSubmitAction || isEnterKey) {
                submitSearch()
                true
            } else {
                false
            }
        }
    }

    private fun toggleSearchField() {
        if (isSearchVisible) {
            closeSearchField(clearText = true, reload = true)
        } else {
            openSearchField()
        }
    }

    private fun openSearchField() {
        isSearchVisible = true
        binding.appTopBar.setSearchIconResource(R.drawable.ia_ic_filter_close)
        binding.appTopBar.searchButton.contentDescription =
            getString(R.string.sale_orders_search_close)

        binding.searchInputLayout.isVisible = true
        binding.searchInputLayout.alpha = 0f
        binding.searchInputLayout.animate()
            .alpha(1f)
            .setDuration(SEARCH_ANIMATION_DURATION_MS)
            .start()

        binding.searchEditText.post {
            if (_binding == null || !isSearchVisible) return@post
            binding.searchEditText.requestFocus()
            binding.searchEditText.showKeyboard()
        }
    }

    private fun closeSearchField(clearText: Boolean, reload: Boolean) {
        isSearchVisible = false
        binding.appTopBar.setSearchIconResource(R.drawable.ia_ic_search)
        binding.appTopBar.searchButton.contentDescription =
            getString(R.string.sale_orders_search_open)

        binding.searchEditText.clearFocus()
        binding.searchEditText.hideKeyboard()

        binding.searchInputLayout.animate()
            .alpha(0f)
            .setDuration(SEARCH_ANIMATION_DURATION_MS)
            .withEndAction {
                if (_binding == null) return@withEndAction
                binding.searchInputLayout.isVisible = false
            }
            .start()

        if (clearText) {
            binding.searchEditText.setText("")
            viewModel.clearSearchQuery()
        }

        if (reload) {
            binding.saleOrdersRecyclerView.scrollToPosition(0)
            viewModel.submitSearch("")
        }
    }

    private fun submitSearch() {
        if (!isSearchVisible) return
        val query = binding.searchEditText.text?.toString().orEmpty().trim()
        binding.searchEditText.clearFocus()
        binding.searchEditText.hideKeyboard()
        binding.saleOrdersRecyclerView.scrollToPosition(0)
        viewModel.submitSearch(query)
    }

    private fun observeOrders() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.ordersState.collect { state ->
                    when (state) {
                        UiState.Loading -> showLoading()
                        is UiState.Success -> showSuccess(state.data)
                        UiState.Empty -> showEmpty()
                        is UiState.Error -> showError(state.message)
                        UiState.Unauthorized -> showError(getString(R.string.sale_orders_error))
                        else -> Unit
                    }
                }
            }
        }
    }

    private fun showLoading() {
        showPaginationShimmer(false)
        binding.saleOrdersRecyclerView.isVisible = false
        binding.emptyStateText.isVisible = false
        binding.errorStateText.isVisible = false
        binding.shimmerContainer.isVisible = true
        shimmerHelper.populate(
            container = binding.shimmerContainer,
            inflater = layoutInflater,
            itemLayoutRes = R.layout.item_sale_order_shimmer,
            count = SHIMMER_ITEM_COUNT
        )
    }

    private fun showSuccess(data: SaleOrderListUiModel) {
        shimmerHelper.clear(binding.shimmerContainer)
        binding.shimmerContainer.isVisible = false
        binding.errorStateText.isVisible = false
        binding.totalEntriesText.text =
            getString(R.string.sale_orders_total_entries, data.totalCount)

        filterChipAdapter.setSelectedFilterId(viewModel.getSelectedFilterId())
        filterChipAdapter.submitList(data.filterChips)

        val isEmpty = data.orders.isEmpty()
        binding.emptyStateText.isVisible = isEmpty
        if (isEmpty) {
            binding.saleOrdersRecyclerView.isVisible = false
            ordersAdapter.submitList(emptyList())
            binding.emptyStateText.setText(
                data.emptyState?.titleRes ?: R.string.sale_orders_empty_title
            )
        } else {
            binding.saleOrdersRecyclerView.isVisible = true
            ordersAdapter.submitList(data.orders)
        }
        showPaginationShimmer(data.isLoadingNextPage)
    }

    private fun showEmpty() {
        shimmerHelper.clear(binding.shimmerContainer)
        binding.shimmerContainer.isVisible = false
        showPaginationShimmer(false)
        binding.saleOrdersRecyclerView.isVisible = false
        binding.errorStateText.isVisible = false
        binding.emptyStateText.isVisible = true
        binding.emptyStateText.setText(R.string.sale_orders_empty_title)
        ordersAdapter.submitList(emptyList())
    }

    private fun showError(message: String) {
        shimmerHelper.clear(binding.shimmerContainer)
        binding.shimmerContainer.isVisible = false
        showPaginationShimmer(false)
        binding.saleOrdersRecyclerView.isVisible = false
        binding.emptyStateText.isVisible = false
        binding.errorStateText.isVisible = true
        binding.errorStateText.text = message.ifBlank { getString(R.string.sale_orders_error) }
        ordersAdapter.submitList(emptyList())
    }

    private fun showPaginationShimmer(show: Boolean) {
        binding.paginationShimmerContainer.isVisible = show
        if (show) {
            shimmerHelper.populate(
                container = binding.paginationShimmerContainer,
                inflater = layoutInflater,
                itemLayoutRes = R.layout.item_sale_order_shimmer,
                count = 1
            )
        } else {
            shimmerHelper.clear(binding.paginationShimmerContainer)
        }
    }

    private fun navigateToViewSaleOrder(order: SaleOrderModel) {
        findNavController().navigate(
            R.id.iaViewSaleOrderFragment,
            bundleOf(ViewSaleOrderViewModel.ARG_SALE_ORDER_ID to order.id)
        )
    }

    private fun navigateToEditSaleOrder(order: SaleOrderModel) {
        findNavController().navigate(
            R.id.iaAddSaleOrderFragment,
            bundleOf(AddSaleOrderViewModel.ARG_SALE_ORDER_ID to order.id)
        )
    }

    private fun openSaleOrderWorkflow(order: SaleOrderModel) {
        viewModel.loadSaleOrderWorkflow(order)
    }

    private fun observeSaleOrderWorkflow() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.workflowState.collect { state ->
                    when (state) {
                        UiState.Loading -> Unit
                        is UiState.Success -> {
                            workflowBottomSheet?.dismiss()

                            workflowBottomSheet = QuoteWorkflowBottomSheet(
                                fragment = this@SaleOrdersFragment,
                                workflow = state.data,
                                titleResId = R.string.sale_order_workflow_title,
                                onApproveClick = { step, comments ->

                                    viewModel.decideWorkflowNode(
                                        instanceId = state.data.instanceId,
                                        nodeId = step.workflowNodeId,
                                        approved = true,
                                        comments = comments
                                    )
                                },

                                onRejectClick = { step, comments ->

                                    viewModel.decideWorkflowNode(
                                        instanceId = state.data.instanceId,
                                        nodeId = step.workflowNodeId,
                                        approved = false,
                                        comments = comments
                                    )
                                }
                            )
                            workflowBottomSheet?.show()
                            viewModel.resetWorkflowState()
                        }

                        is UiState.Error -> {
                            Toast.makeText(
                                requireContext(),
                                state.message.ifBlank {
                                    getString(R.string.sale_order_workflow_load_failed)
                                },
                                Toast.LENGTH_LONG
                            ).show()
                            viewModel.resetWorkflowState()
                        }

                        UiState.Unauthorized -> {
                            Toast.makeText(
                                requireContext(),
                                R.string.sale_orders_error,
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

    private fun observeSaleOrderDecisionWorkflow() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(
                Lifecycle.State.STARTED
            ) {
                viewModel.workflowDecisionState.collect { state ->

                    when (state) {

                        UiState.Loading -> Unit

                        is UiState.Success -> {
                            Toast.makeText(
                                requireContext(),
                                state.data.message,
                                Toast.LENGTH_SHORT
                            ).show()

                            workflowBottomSheet?.dismiss()
                            workflowBottomSheet = null

                            viewModel.resetWorkflowDecisionState()

                            viewModel.loadOrders()
                        }

                        is UiState.Error -> {
                            Toast.makeText(
                                requireContext(),
                                state.message,
                                Toast.LENGTH_SHORT
                            ).show()

                            viewModel.resetWorkflowDecisionState()

                        }

                        UiState.Unauthorized -> {
                            viewModel.resetWorkflowDecisionState()
                        }

                        else -> Unit
                    }
                }
            }
        }
    }

    private fun setupFabMenu() {
        binding.fabMenuScrim.isVisible = false
        binding.fabMenuScrim.alpha = 0f
        binding.addSaleOrderActionRow.isVisible = false
        prepareClosedFabAction(binding.addSaleOrderActionRow)

        fabMenuBackCallback = object : OnBackPressedCallback(false) {
            override fun handleOnBackPressed() {
                closeFabMenu()
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, fabMenuBackCallback)

        binding.createSaleOrderFab.setOnClickListener { toggleFabMenu() }
        binding.fabMenuScrim.setOnClickListener { closeFabMenu() }

        binding.addSaleOrderSubFab.setOnClickListener {
            closeFabMenu()
            findNavController().navigate(R.id.iaAddSaleOrderFragment)
        }
        binding.addSaleOrderLabel.setOnClickListener { binding.addSaleOrderSubFab.performClick() }
    }

    private fun toggleFabMenu() {
        if (isFabMenuOpen) closeFabMenu() else openFabMenu()
    }

    private fun openFabMenu() {
        if (isFabMenuOpen) return
        isFabMenuOpen = true
        fabMenuBackCallback.isEnabled = true

        applyContentBlur(true)

        binding.fabMenuScrim.isVisible = true
        binding.fabMenuScrim.animate()
            .alpha(1f)
            .setDuration(FAB_MENU_ANIMATION_MS)
            .setInterpolator(DecelerateInterpolator())
            .start()

        binding.createSaleOrderFab.setImageResource(R.drawable.ic_close)
        binding.createSaleOrderFab.animate()
            .rotation(90f)
            .setDuration(FAB_MENU_ANIMATION_MS)
            .start()

        showFabAction(binding.addSaleOrderActionRow)
    }

    private fun closeFabMenu() {
        if (!isFabMenuOpen) return
        isFabMenuOpen = false
        fabMenuBackCallback.isEnabled = false

        binding.createSaleOrderFab.setImageResource(R.drawable.ia_ic_add)
        binding.createSaleOrderFab.animate()
            .rotation(0f)
            .setDuration(FAB_MENU_ANIMATION_MS)
            .start()

        hideFabAction(binding.addSaleOrderActionRow)

        binding.fabMenuScrim.animate()
            .alpha(0f)
            .setDuration(FAB_MENU_ANIMATION_MS)
            .withEndAction {
                if (_binding == null) return@withEndAction
                binding.fabMenuScrim.isVisible = false
                applyContentBlur(false)
            }
            .start()
    }

    private fun prepareClosedFabAction(row: View) {
        row.alpha = 0f
        row.translationY = FAB_ACTION_TRANSLATION_Y
        row.scaleX = 0.85f
        row.scaleY = 0.85f
    }

    private fun showFabAction(row: View) {
        row.animate().cancel()
        prepareClosedFabAction(row)
        row.isVisible = true
        row.animate()
            .alpha(1f)
            .translationY(0f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(FAB_MENU_ANIMATION_MS)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    private fun hideFabAction(row: View) {
        row.animate().cancel()
        row.animate()
            .alpha(0f)
            .translationY(FAB_ACTION_TRANSLATION_Y)
            .scaleX(0.85f)
            .scaleY(0.85f)
            .setDuration(FAB_MENU_ANIMATION_MS)
            .withEndAction {
                if (_binding == null) return@withEndAction
                row.isVisible = false
            }
            .start()
    }

    private fun applyContentBlur(enabled: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            binding.contentContainer.setRenderEffect(
                if (enabled) {
                    RenderEffect.createBlurEffect(
                        CONTENT_BLUR_RADIUS,
                        CONTENT_BLUR_RADIUS,
                        Shader.TileMode.CLAMP
                    )
                } else {
                    null
                }
            )
        }
    }

    override fun onDestroyView() {
        if (_binding != null) {
            applyContentBlur(false)
        }
        shimmerHelper.release()
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val PAGINATION_THRESHOLD_ITEMS = 2
        private const val SHIMMER_ITEM_COUNT = 5
        private const val SEARCH_ANIMATION_DURATION_MS = 180L

        private const val FAB_MENU_ANIMATION_MS = 220L
        private const val FAB_ACTION_TRANSLATION_Y = 28f
        private const val CONTENT_BLUR_RADIUS = 28f

    }
}
