package com.axelliant.hris.features.subscriptions.presentation

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.view.inputmethod.InputMethodManager
import androidx.activity.OnBackPressedCallback
import androidx.core.content.getSystemService
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager
import com.axelliant.hris.R
import com.axelliant.hris.core.ui.ShimmerAnimatorHelper
import com.axelliant.hris.core.ui.UiState
import com.axelliant.hris.databinding.FragmentSubscriptionsBinding
import com.axelliant.hris.extention.showSuccessMsg
import com.axelliant.hris.features.subscriptions.domain.model.SubscriptionListUiModel
import com.axelliant.hris.features.subscriptions.domain.model.SubscriptionModel
import com.axelliant.hris.features.subscriptions.domain.model.SubscriptionStatus
import com.axelliant.hris.ui.designsystem.components.AppTextFieldLayout
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SubscriptionsFragment : Fragment() {

    private val viewModel: SubscriptionsViewModel by viewModels()
    private var _binding: FragmentSubscriptionsBinding? = null
    private val binding get() = _binding!!
    private val loadingShimmerHelper = ShimmerAnimatorHelper()
    private val paginationShimmerHelper = ShimmerAnimatorHelper()
    private lateinit var subscriptionsAdapter: SubscriptionsAdapter
    private val filterTabAdapter = SubscriptionFilterTabAdapter { tab ->
        onFilterTabSelected(tab)
    }
    private var selectedFilterId = FILTER_ALL

    private var isFabMenuOpen = false
    private lateinit var fabMenuBackCallback: OnBackPressedCallback



    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSubscriptionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupFabMenu()

        SubscriptionCommerceTabs.bind(
            this,
            binding.commerceTabs,
            SubscriptionCommerceTabs.SUBSCRIPTIONS,
        )
        binding.filterTabsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = filterTabAdapter
        }
        subscriptionsAdapter = SubscriptionsAdapter()
        binding.subscriptionsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = subscriptionsAdapter
            setHasFixedSize(false)
        }
        submitFilterTabs(
            listOf(
                SubscriptionFilterTab(FILTER_ALL, R.string.subscription_filter_all_count),
                SubscriptionFilterTab(FILTER_ACTIVE, R.string.subscription_filter_active_count),
                SubscriptionFilterTab(FILTER_TRIAL, R.string.subscription_filter_trial_count),
            )
        )
        binding.appTopBar.setOnBackClickListener { findNavController().navigateUp() }
        binding.appTopBar.setOnSearchClickListener { toggleSearchField() }
        binding.searchEditText.doAfterTextChanged { text ->
            binding.searchInputLayout.endIconMode = if (text.isNullOrEmpty()) {
                AppTextFieldLayout.END_ICON_NONE
            } else {
                AppTextFieldLayout.END_ICON_CLEAR_TEXT
            }
            viewModel.onSearchQueryChanged(text?.toString().orEmpty())
        }
        binding.filterButton.setOnClickListener {
            requireContext().showSuccessMsg(getString(R.string.subscriptions_filter_coming_soon))
        }
       /* binding.newSubscriptionButton.setOnClickListener {
            findNavController().navigate(R.id.iaNewSubscriptionFragment)
        }*/

        binding.createSaleOrderFab.setOnClickListener { toggleFabMenu() }
        binding.fabMenuScrim.setOnClickListener { closeFabMenu() }

        binding.addSaleOrderSubFab.setOnClickListener {
            closeFabMenu()
            findNavController().navigate(R.id.iaNewSubscriptionFragment)
        }
        binding.addSaleOrderLabel.setOnClickListener { binding.addSaleOrderSubFab.performClick() }


        binding.errorRetryButton.setOnClickListener { viewModel.loadSubscriptions() }
        binding.emptyRetryButton.setOnClickListener { viewModel.loadSubscriptions() }
        binding.subscriptionsScrollView.setOnScrollChangeListener(
            NestedScrollView.OnScrollChangeListener { scrollView, _, scrollY, _, _ ->
                val child = scrollView.getChildAt(0) ?: return@OnScrollChangeListener
                val hasReachedBottom =
                    scrollY >= child.measuredHeight - scrollView.measuredHeight - PAGINATION_THRESHOLD_PX
                if (hasReachedBottom) viewModel.loadNextPage()
            }
        )
        observeSubscriptions()
        viewModel.loadSubscriptionsIfNeeded()
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

    private fun toggleFabMenu() {
        if (isFabMenuOpen) closeFabMenu() else openFabMenu()
    }

    private fun applyContentBlur(enabled: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            binding.subscriptionsContent.setRenderEffect(
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

    fun submitFilterTabs(tabs: List<SubscriptionFilterTab>) {
        selectedFilterId = selectedFilterId.takeIf { id -> tabs.any { it.id == id } }
            ?: tabs.firstOrNull()?.id.orEmpty()
        filterTabAdapter.submitTabs(tabs, selectedFilterId)
    }

    private fun onFilterTabSelected(tab: SubscriptionFilterTab) {
        selectedFilterId = tab.id
        val data = (viewModel.subscriptionsState.value as? UiState.Success)?.data ?: return
        renderSuccess(data)
    }

    private fun toggleSearchField() {
        val showSearch = !binding.searchInputLayout.isVisible
        TransitionManager.beginDelayedTransition(
            binding.subscriptionsContent,
            AutoTransition().setDuration(180L),
        )
        binding.searchInputLayout.isVisible = showSearch
        if (showSearch) {
            binding.searchEditText.requestFocus()
            binding.searchEditText.post {
                requireContext().getSystemService<InputMethodManager>()
                    ?.showSoftInput(binding.searchEditText, InputMethodManager.SHOW_IMPLICIT)
            }
        } else {
            binding.searchEditText.clearFocus()
            binding.searchEditText.setText("")
            viewModel.clearSearchQuery()
            requireContext().getSystemService<InputMethodManager>()
                ?.hideSoftInputFromWindow(binding.searchEditText.windowToken, 0)
        }
    }

    private fun observeSubscriptions() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.subscriptionsState.collect { state ->
                    when (state) {
                        UiState.Idle -> Unit
                        UiState.Loading -> showLoadingState()
                        is UiState.Success -> renderSuccess(state.data)
                        is UiState.Error -> showErrorState(state.message)
                        UiState.Unauthorized -> showErrorState(getString(R.string.subscriptions_error_description))
                        UiState.Empty -> renderSuccess(SubscriptionListUiModel(emptyList()))
                    }
                }
            }
        }
    }

    private fun showLoadingState() {
        binding.subscriptionCountText.text = getString(R.string.subscriptions_count_format, 0)
        subscriptionsAdapter.submitList(emptyList())
        binding.subscriptionsRecyclerView.isVisible = false
        binding.emptyStateContainer.isVisible = false
        binding.errorStateContainer.isVisible = false
        setPaginationLoading(false)
        showLoading(true)
    }

    private fun renderSuccess(data: SubscriptionListUiModel) {
        showLoading(false)
        setPaginationLoading(data.isLoadingNextPage)
        submitFilterTabs(buildFilterTabs(data.subscriptions, data.totalCount))
        subscriptionsAdapter.submitList(filterSubscriptions(data.subscriptions))
        binding.subscriptionCountText.text = resources.getQuantityString(
            R.plurals.subscriptions_count,
            data.totalCount,
            data.totalCount
        )
        val isEmpty = filterSubscriptions(data.subscriptions).isEmpty()
        binding.subscriptionsRecyclerView.isVisible = !isEmpty
        binding.emptyStateContainer.isVisible = isEmpty
        binding.errorStateContainer.isVisible = false
    }

    private fun showErrorState(message: String) {
        showLoading(false)
        setPaginationLoading(false)
        subscriptionsAdapter.submitList(emptyList())
        binding.subscriptionCountText.text = getString(R.string.subscriptions_count_format, 0)
        binding.errorDescriptionText.text = message.ifBlank {
            getString(R.string.subscriptions_error_description)
        }
        binding.subscriptionsRecyclerView.isVisible = false
        binding.emptyStateContainer.isVisible = false
        binding.errorStateContainer.isVisible = true
    }

    private fun showLoading(isLoading: Boolean) {
        binding.shimmerContainer.isVisible = isLoading
        if (isLoading) {
            loadingShimmerHelper.populate(
                container = binding.shimmerContainer,
                inflater = layoutInflater,
                itemLayoutRes = R.layout.item_subscription_shimmer,
                count = SHIMMER_ITEM_COUNT
            )
        } else {
            loadingShimmerHelper.clear(binding.shimmerContainer)
        }
    }

    private fun setPaginationLoading(isLoading: Boolean) {
        binding.paginationShimmerContainer.isVisible = isLoading
        if (isLoading) {
            paginationShimmerHelper.populate(
                container = binding.paginationShimmerContainer,
                inflater = layoutInflater,
                itemLayoutRes = R.layout.item_subscription_shimmer,
                count = 1
            )
            binding.subscriptionsScrollView.post {
                if (_binding == null || !binding.paginationShimmerContainer.isVisible) {
                    return@post
                }
                binding.subscriptionsScrollView.smoothScrollTo(
                    0,
                    binding.subscriptionsScrollView.getChildAt(0)?.bottom ?: 0
                )
            }
        } else {
            paginationShimmerHelper.clear(binding.paginationShimmerContainer)
        }
    }

    private fun buildFilterTabs(
        subscriptions: List<SubscriptionModel>,
        totalCount: Int
    ): List<SubscriptionFilterTab> {
        return listOf(
            SubscriptionFilterTab(
                FILTER_ALL,
                R.string.subscription_filter_all_count,
                totalCount.coerceAtLeast(subscriptions.size)
            ),
            SubscriptionFilterTab(
                FILTER_ACTIVE,
                R.string.subscription_filter_active_count,
                subscriptions.count { it.status == SubscriptionStatus.ACTIVE }
            ),
            SubscriptionFilterTab(
                FILTER_TRIAL,
                R.string.subscription_filter_trial_count,
                subscriptions.count { it.status == SubscriptionStatus.TRIAL }
            )
        )
    }

    private fun filterSubscriptions(
        subscriptions: List<SubscriptionModel>
    ): List<SubscriptionModel> {
        return when (selectedFilterId) {
            FILTER_ACTIVE -> subscriptions.filter { it.status == SubscriptionStatus.ACTIVE }
            FILTER_TRIAL -> subscriptions.filter { it.status == SubscriptionStatus.TRIAL }
            else -> subscriptions
        }
    }

    override fun onDestroyView() {
        loadingShimmerHelper.release()
        paginationShimmerHelper.release()
        super.onDestroyView()
        _binding = null
    }

    private companion object {
        const val FILTER_ALL = "all"
        const val FILTER_ACTIVE = "active"
        const val FILTER_TRIAL = "trial"
        const val SHIMMER_ITEM_COUNT = 5
        const val PAGINATION_THRESHOLD_PX = 240

        private const val FAB_MENU_ANIMATION_MS = 220L
        private const val FAB_ACTION_TRANSLATION_Y = 28f
        private const val CONTENT_BLUR_RADIUS = 28f

        private const val SEARCH_ANIMATION_DURATION_MS = 180L

    }
}
