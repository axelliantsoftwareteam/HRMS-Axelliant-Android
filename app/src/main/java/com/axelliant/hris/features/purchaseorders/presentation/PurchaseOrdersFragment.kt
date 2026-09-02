package com.axelliant.hris.features.purchaseorders.presentation

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
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
import com.axelliant.hris.databinding.FragmentPurchaseOrdersBinding
import com.axelliant.hris.features.internalapps.navigation.InternalAppsNavigator
import com.axelliant.hris.features.purchaseorders.domain.model.PurchaseOrderListUiModel
import com.axelliant.hris.features.purchaseorders.domain.model.PurchaseOrderModel
import com.axelliant.hris.features.purchaseorders.domain.model.PurchaseOrderStatus
import dagger.hilt.android.AndroidEntryPoint
import java.text.NumberFormat
import java.util.Locale
import kotlinx.coroutines.launch

@AndroidEntryPoint
class PurchaseOrdersFragment : Fragment() {

    private val viewModel: PurchaseOrdersViewModel by viewModels()
    private var _binding: FragmentPurchaseOrdersBinding? = null
    private val binding get() = _binding!!

    private val shimmerHelper = ShimmerAnimatorHelper()
    private lateinit var ordersAdapter: PurchaseOrdersAdapter
    private lateinit var filterChipAdapter: PurchaseOrderFilterChipAdapter
    private lateinit var actionMenuHandler: PurchaseOrderActionMenuHandler
    private lateinit var fabMenuBackCallback: OnBackPressedCallback
    private var historyBottomSheet: PurchaseOrderHistoryBottomSheet? = null

    private var isSearchVisible = false
    private var isFabMenuOpen = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPurchaseOrdersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupSearchUi()
        setupAdapters()
        setupFabMenu()
        setupInteractions()
        setupPagination()
        observePurchaseOrderCreated()
        observeOrders()
        observePurchaseOrderHistory()
        viewModel.loadOrdersIfNeeded()
    }

    private fun observePurchaseOrderCreated() {
        val savedStateHandle = findNavController().currentBackStackEntry?.savedStateHandle ?: return
        savedStateHandle.getLiveData<Boolean>(KEY_PURCHASE_ORDER_CREATED)
            .observe(viewLifecycleOwner) { created ->
                if (created != true) return@observe
                savedStateHandle[KEY_PURCHASE_ORDER_CREATED] = false
                viewModel.loadOrders()
            }
    }

    private fun setupSearchUi() {
        isSearchVisible = false
        binding.searchInputLayout.isVisible = false
        binding.searchInputLayout.alpha = 0f
        binding.appTopBar.setSearchIconResource(R.drawable.ia_ic_search)
        binding.appTopBar.searchButton.contentDescription = getString(R.string.purchase_orders_search_open)
    }

    private fun setupAdapters() {
        actionMenuHandler = PurchaseOrderActionMenuHandler { actionId, order ->
            when (actionId) {
                R.id.action_purchase_order_view -> navigateToViewPurchaseOrder(order)
                R.id.action_purchase_order_edit -> navigateToEditPurchaseOrder(order)
                R.id.action_purchase_order_show_report,
                R.id.action_purchase_order_submit_po,
                R.id.action_purchase_order_send_to_vendor -> {
                    Toast.makeText(requireContext(), R.string.coming_soon, Toast.LENGTH_SHORT).show()
                }
                R.id.action_purchase_order_history -> openPurchaseOrderHistory(order)
            }
        }
        ordersAdapter = PurchaseOrdersAdapter { order, anchor ->
            if (isFabMenuOpen) {
                closeFabMenu()
                return@PurchaseOrdersAdapter
            }
            actionMenuHandler.show(anchor, order)
        }
        binding.purchaseOrdersRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = ordersAdapter
            setHasFixedSize(false)
        }
        filterChipAdapter = PurchaseOrderFilterChipAdapter { chip ->
            viewModel.selectUtilizationFilter(chip)
        }
        binding.filterChipsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = filterChipAdapter
            setHasFixedSize(true)
        }
    }

    private fun navigateToViewPurchaseOrder(order: PurchaseOrderModel) {
        findNavController().navigate(
            R.id.iaViewPurchaseOrderFragment,
            bundleOf(ViewPurchaseOrderViewModel.ARG_PURCHASE_ORDER_ID to order.id)
        )
    }

    private fun navigateToEditPurchaseOrder(order: PurchaseOrderModel) {
        findNavController().navigate(
            R.id.iaEditPurchaseOrderFragment,
            bundleOf(EditPurchaseOrderViewModel.ARG_PURCHASE_ORDER_ID to order.id)
        )
    }

    private fun openPurchaseOrderHistory(order: PurchaseOrderModel) {
        historyBottomSheet?.dismiss()
        historyBottomSheet = PurchaseOrderHistoryBottomSheet(this).also { sheet ->
            sheet.show()
            sheet.render(UiState.Loading)
        }
        viewModel.loadPurchaseOrderHistory(order)
    }

    private fun setupFabMenu() {
        binding.fabMenuScrim.isVisible = false
        binding.fabMenuScrim.alpha = 0f
        binding.addPosActionRow.isVisible = false
        binding.createManualPoActionRow.isVisible = false
        prepareClosedFabAction(binding.addPosActionRow)
        prepareClosedFabAction(binding.createManualPoActionRow)

        fabMenuBackCallback = object : OnBackPressedCallback(false) {
            override fun handleOnBackPressed() {
                closeFabMenu()
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, fabMenuBackCallback)
    }

    private fun setupPagination() {
        binding.purchaseOrdersRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
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
        binding.appTopBar.setOnSearchClickListener {
            if (isFabMenuOpen) {
                closeFabMenu()
                return@setOnSearchClickListener
            }
            toggleSearchField()
        }
        binding.createPurchaseOrderFab.setOnClickListener { toggleFabMenu() }
        binding.fabMenuScrim.setOnClickListener { closeFabMenu() }
        binding.addPosFab.setOnClickListener {
            closeFabMenu()
            findNavController().navigate(R.id.iaAddPurchaseOrderFragment)
        }
        binding.createManualPoFab.setOnClickListener {
            closeFabMenu()
            findNavController().navigate(R.id.iaCreateManualPurchaseOrderFragment)
        }
        binding.addPosLabel.setOnClickListener { binding.addPosFab.performClick() }
        binding.createManualPoLabel.setOnClickListener { binding.createManualPoFab.performClick() }
        binding.emptyCreateOrderButton.setOnClickListener {
            if (isFabMenuOpen) closeFabMenu() else openFabMenu()
        }
        binding.errorRetryButton.setOnClickListener { viewModel.loadOrders() }
        binding.errorCheckConnectionButton.setOnClickListener {
            Toast.makeText(
                requireContext(),
                R.string.purchase_orders_error_description,
                Toast.LENGTH_SHORT
            ).show()
        }

        binding.searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
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

        binding.createPurchaseOrderFab.setImageResource(R.drawable.ic_close)
        binding.createPurchaseOrderFab.contentDescription = getString(R.string.purchase_orders_fab_close)
        binding.createPurchaseOrderFab.animate()
            .rotation(90f)
            .setDuration(FAB_MENU_ANIMATION_MS)
            .start()

        showFabAction(binding.createManualPoActionRow, delayMs = 40L)
        showFabAction(binding.addPosActionRow, delayMs = 90L)
    }

    private fun closeFabMenu() {
        if (!isFabMenuOpen) return
        isFabMenuOpen = false
        fabMenuBackCallback.isEnabled = false

        binding.createPurchaseOrderFab.setImageResource(R.drawable.ia_ic_add)
        binding.createPurchaseOrderFab.contentDescription = getString(R.string.purchase_orders_fab_open)
        binding.createPurchaseOrderFab.animate()
            .rotation(0f)
            .setDuration(FAB_MENU_ANIMATION_MS)
            .start()

        hideFabAction(binding.addPosActionRow, delayMs = 0L)
        hideFabAction(binding.createManualPoActionRow, delayMs = 40L)

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

    private fun showFabAction(row: View, delayMs: Long) {
        row.animate().cancel()
        prepareClosedFabAction(row)
        row.isVisible = true
        row.animate()
            .alpha(1f)
            .translationY(0f)
            .scaleX(1f)
            .scaleY(1f)
            .setStartDelay(delayMs)
            .setDuration(FAB_MENU_ANIMATION_MS)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    private fun hideFabAction(row: View, delayMs: Long) {
        row.animate().cancel()
        row.animate()
            .alpha(0f)
            .translationY(FAB_ACTION_TRANSLATION_Y)
            .scaleX(0.85f)
            .scaleY(0.85f)
            .setStartDelay(delayMs)
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
        binding.appTopBar.searchButton.contentDescription = getString(R.string.purchase_orders_search_close)
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
        binding.appTopBar.searchButton.contentDescription = getString(R.string.purchase_orders_search_open)
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
            viewModel.loadOrders()
        }
    }

    private fun submitSearch() {
        val query = binding.searchEditText.text?.toString().orEmpty().trim()
        binding.searchEditText.clearFocus()
        binding.searchEditText.hideKeyboard()
        viewModel.submitSearch(query)
    }

    private fun observeOrders() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.ordersState.collect { state ->
                    when (state) {
                        UiState.Idle -> Unit
                        UiState.Loading -> {
                            updateSummaryForOrders(emptyList(), totalCount = 0)
                            showLoading(true)
                            binding.filterChipsRecyclerView.isVisible = false
                            binding.emptyStateContainer.isVisible = false
                            binding.errorStateContainer.isVisible = false
                            binding.purchaseOrdersRecyclerView.isVisible = false
                            setPaginationLoading(false)
                        }
                        is UiState.Success -> renderSuccess(state.data)
                        is UiState.Error -> {
                            showErrorState()
                        }
                        UiState.Unauthorized -> {
                            showErrorState()
                        }
                        UiState.Empty -> {
                            updateSummaryForOrders(emptyList(), totalCount = 0)
                            showLoading(false)
                            setPaginationLoading(false)
                            binding.filterChipsRecyclerView.isVisible = false
                            binding.purchaseOrdersRecyclerView.isVisible = false
                            binding.errorStateContainer.isVisible = false
                            binding.emptyStateContainer.isVisible = true
                        }
                    }
                }
            }
        }
    }

    private fun observePurchaseOrderHistory() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.historyState.collect { state ->
                    when (state) {
                        UiState.Idle -> Unit
                        UiState.Loading,
                        is UiState.Success,
                        UiState.Empty -> historyBottomSheet?.render(state)
                        is UiState.Error -> {
                            historyBottomSheet?.dismiss()
                            historyBottomSheet = null
                            Toast.makeText(
                                requireContext(),
                                state.message.ifBlank {
                                    getString(R.string.purchase_orders_error_title)
                                },
                                Toast.LENGTH_SHORT
                            ).show()
                            viewModel.resetHistoryState()
                        }
                        UiState.Unauthorized -> {
                            historyBottomSheet?.dismiss()
                            historyBottomSheet = null
                            Toast.makeText(
                                requireContext(),
                                R.string.purchase_orders_error_title,
                                Toast.LENGTH_SHORT
                            ).show()
                            viewModel.resetHistoryState()
                        }
                    }
                }
            }
        }
    }

    private fun renderSuccess(data: PurchaseOrderListUiModel) {
        showLoading(false)
        setPaginationLoading(data.isLoadingNextPage)
        filterChipAdapter.setSelectedFilterId(data.selectedFilterId)
        filterChipAdapter.submitList(data.filterChips)
        ordersAdapter.submitList(data.orders)
        updateSummaryForOrders(data.orders, data.totalCount)
        val isEmpty = data.orders.isEmpty()
        binding.purchaseOrdersRecyclerView.isVisible = !isEmpty
        binding.filterChipsRecyclerView.isVisible = data.filterChips.isNotEmpty()
        binding.emptyStateContainer.isVisible = isEmpty
        binding.errorStateContainer.isVisible = false
    }

    private fun showErrorState() {
        updateSummaryForError()
        showLoading(false)
        setPaginationLoading(false)
        binding.filterChipsRecyclerView.isVisible = false
        binding.purchaseOrdersRecyclerView.isVisible = false
        binding.emptyStateContainer.isVisible = false
        binding.errorStateContainer.isVisible = true
    }

    private fun updateSummaryForOrders(
        orders: List<PurchaseOrderModel>,
        totalCount: Int
    ) {
        val displayCount = totalCount.coerceAtLeast(orders.size)
        val openCount = orders.count { it.status == PurchaseOrderStatus.RELEASED }
        val totalValue = orders.sumOf { parseCurrency(it.grandTotal) }

        binding.totalOrdersValue.text = displayCount.toString()
        binding.openOrdersValue.text = openCount.toString()
        binding.totalValueAmount.text = NumberFormat
            .getCurrencyInstance(Locale.US)
            .format(totalValue)
    }

    private fun updateSummaryForError() {
        binding.totalOrdersValue.text = "-"
        binding.openOrdersValue.text = "-"
        binding.totalValueAmount.text = "-"
    }

    private fun parseCurrency(value: String): Double {
        return value.replace(Regex("[^0-9.]"), "").toDoubleOrNull() ?: 0.0
    }

    private fun showLoading(isLoading: Boolean) {
        binding.shimmerContainer.isVisible = isLoading
        if (isLoading) {
            shimmerHelper.populate(
                container = binding.shimmerContainer,
                inflater = layoutInflater,
                itemLayoutRes = R.layout.item_purchase_order_shimmer,
                count = SHIMMER_ITEM_COUNT
            )
        } else {
            shimmerHelper.clear(binding.shimmerContainer)
        }
    }

    private fun setPaginationLoading(isLoading: Boolean) {
        binding.paginationShimmerContainer.isVisible = isLoading
        if (isLoading) {
            shimmerHelper.populate(
                container = binding.paginationShimmerContainer,
                inflater = layoutInflater,
                itemLayoutRes = R.layout.item_purchase_order_shimmer,
                count = 1
            )
        } else {
            shimmerHelper.clear(binding.paginationShimmerContainer)
        }
    }

    override fun onDestroyView() {
        if (_binding != null) {
            applyContentBlur(false)
        }
        historyBottomSheet?.dismiss()
        historyBottomSheet = null
        shimmerHelper.release()
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val KEY_PURCHASE_ORDER_CREATED = "purchase_order_created"
        private const val PAGINATION_THRESHOLD_ITEMS = 2
        private const val SHIMMER_ITEM_COUNT = 5
        private const val SEARCH_ANIMATION_DURATION_MS = 180L
        private const val FAB_MENU_ANIMATION_MS = 220L
        private const val FAB_ACTION_TRANSLATION_Y = 28f
        private const val CONTENT_BLUR_RADIUS = 28f
    }
}
