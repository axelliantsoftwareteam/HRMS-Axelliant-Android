package com.axelliant.hris.features.subscriptions.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
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
import com.axelliant.hris.databinding.FragmentSubscriptionPlansBinding
import com.axelliant.hris.extention.showSuccessMsg
import com.axelliant.hris.features.subscriptions.domain.model.SubscriptionPlanListUiModel
import com.axelliant.hris.features.subscriptions.domain.model.SubscriptionPlanModel
import com.axelliant.hris.features.subscriptions.domain.model.SubscriptionPlanStatus
import com.axelliant.hris.ui.designsystem.components.AppTextFieldLayout
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SubscriptionPlansFragment : Fragment() {
    private val viewModel: SubscriptionPlansViewModel by viewModels()
    private var _binding: FragmentSubscriptionPlansBinding? = null
    private val binding get() = _binding!!
    private val loadingShimmerHelper = ShimmerAnimatorHelper()
    private val paginationShimmerHelper = ShimmerAnimatorHelper()
    private lateinit var plansAdapter: SubscriptionPlansAdapter
    private val filterTabAdapter = SubscriptionFilterTabAdapter { tab ->
        onFilterTabSelected(tab)
    }
    private var selectedFilterId = FILTER_ALL

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View {
        _binding = FragmentSubscriptionPlansBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) = with(binding) {
        SubscriptionCommerceTabs.bind(
            this@SubscriptionPlansFragment,
            commerceTabs,
            SubscriptionCommerceTabs.PLANS,
        )
        filterTabsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = filterTabAdapter
        }
        plansAdapter = SubscriptionPlansAdapter()
        plansRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = plansAdapter
            setHasFixedSize(false)
        }
        submitFilterTabs(
            listOf(
                SubscriptionFilterTab(FILTER_ALL, R.string.subscription_filter_all_count),
                SubscriptionFilterTab(FILTER_ACTIVE, R.string.subscription_filter_active_count),
                SubscriptionFilterTab(FILTER_DRAFT, R.string.subscription_filter_draft_count),
                SubscriptionFilterTab(FILTER_ARCHIVED, R.string.subscription_filter_archived_count),
            )
        )
        appTopBar.setOnBackClickListener { findNavController().navigateUp() }
        appTopBar.setOnSearchClickListener { toggleSearchField() }
        searchInputLayout.endIconMode = AppTextFieldLayout.END_ICON_NONE
        searchEditText.doAfterTextChanged { text ->
            searchInputLayout.endIconMode = if (text.isNullOrEmpty()) {
                AppTextFieldLayout.END_ICON_NONE
            } else {
                AppTextFieldLayout.END_ICON_CLEAR_TEXT
            }
            viewModel.onSearchQueryChanged(text?.toString().orEmpty())
        }
        filterButton.setOnClickListener { requireContext().showSuccessMsg(getString(R.string.subscriptions_filter_coming_soon)) }
        newPlanButton.setOnClickListener { findNavController().navigate(R.id.iaNewSubscriptionPlanFragment) }
        errorRetryButton.setOnClickListener { viewModel.loadPlans() }
        emptyRetryButton.setOnClickListener { viewModel.loadPlans() }
        plansScrollView.setOnScrollChangeListener(
            NestedScrollView.OnScrollChangeListener { scrollView, _, scrollY, _, _ ->
                val child = scrollView.getChildAt(0) ?: return@OnScrollChangeListener
                val hasReachedBottom =
                    scrollY >= child.measuredHeight - scrollView.measuredHeight - PAGINATION_THRESHOLD_PX
                if (hasReachedBottom) viewModel.loadNextPage()
            }
        )
        observePlans()
        viewModel.loadPlansIfNeeded()
    }

    fun submitFilterTabs(tabs: List<SubscriptionFilterTab>) {
        selectedFilterId = selectedFilterId.takeIf { id -> tabs.any { it.id == id } }
            ?: tabs.firstOrNull()?.id.orEmpty()
        filterTabAdapter.submitTabs(tabs, selectedFilterId)
    }

    private fun onFilterTabSelected(tab: SubscriptionFilterTab) {
        selectedFilterId = tab.id
        val data = (viewModel.plansState.value as? UiState.Success)?.data ?: return
        renderSuccess(data)
    }

    private fun toggleSearchField() {
        val showSearch = !binding.searchInputLayout.isVisible
        TransitionManager.beginDelayedTransition(
            binding.root,
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

    private fun observePlans() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.plansState.collect { state ->
                    when (state) {
                        UiState.Idle -> Unit
                        UiState.Loading -> showLoadingState()
                        is UiState.Success -> renderSuccess(state.data)
                        is UiState.Error -> showErrorState(state.message)
                        UiState.Unauthorized -> showErrorState(getString(R.string.subscription_plans_error_description))
                        UiState.Empty -> renderSuccess(SubscriptionPlanListUiModel(emptyList()))
                    }
                }
            }
        }
    }

    private fun showLoadingState() {
        binding.plansCountText.text = getString(R.string.subscription_plans_count_format, 0)
        plansAdapter.submitList(emptyList())
        binding.plansRecyclerView.isVisible = false
        binding.emptyStateContainer.isVisible = false
        binding.errorStateContainer.isVisible = false
        setPaginationLoading(false)
        showLoading(true)
    }

    private fun renderSuccess(data: SubscriptionPlanListUiModel) {
        showLoading(false)
        setPaginationLoading(data.isLoadingNextPage)
        submitFilterTabs(buildFilterTabs(data.plans, data.totalCount))
        val displayedPlans = filterPlans(data.plans)
        plansAdapter.submitList(displayedPlans)
        binding.plansCountText.text = resources.getQuantityString(
            R.plurals.subscription_plans_count_plural,
            data.totalCount,
            data.totalCount
        )
        val isEmpty = displayedPlans.isEmpty()
        binding.plansRecyclerView.isVisible = !isEmpty
        binding.emptyStateContainer.isVisible = isEmpty
        binding.errorStateContainer.isVisible = false
    }

    private fun showErrorState(message: String) {
        showLoading(false)
        setPaginationLoading(false)
        plansAdapter.submitList(emptyList())
        binding.plansCountText.text = getString(R.string.subscription_plans_count_format, 0)
        binding.errorDescriptionText.text = message.ifBlank {
            getString(R.string.subscription_plans_error_description)
        }
        binding.plansRecyclerView.isVisible = false
        binding.emptyStateContainer.isVisible = false
        binding.errorStateContainer.isVisible = true
    }

    private fun showLoading(isLoading: Boolean) {
        binding.shimmerContainer.isVisible = isLoading
        if (isLoading) {
            loadingShimmerHelper.populate(
                container = binding.shimmerContainer,
                inflater = layoutInflater,
                itemLayoutRes = R.layout.item_subscription_plan_shimmer,
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
                itemLayoutRes = R.layout.item_subscription_plan_shimmer,
                count = 1
            )
            binding.plansScrollView.post {
                if (_binding == null || !binding.paginationShimmerContainer.isVisible) {
                    return@post
                }
                binding.plansScrollView.smoothScrollTo(
                    0,
                    binding.plansScrollView.getChildAt(0)?.bottom ?: 0
                )
            }
        } else {
            paginationShimmerHelper.clear(binding.paginationShimmerContainer)
        }
    }

    private fun buildFilterTabs(
        plans: List<SubscriptionPlanModel>,
        totalCount: Int
    ): List<SubscriptionFilterTab> {
        return listOf(
            SubscriptionFilterTab(
                FILTER_ALL,
                R.string.subscription_filter_all_count,
                totalCount.coerceAtLeast(plans.size)
            ),
            SubscriptionFilterTab(
                FILTER_ACTIVE,
                R.string.subscription_filter_active_count,
                plans.count { it.status == SubscriptionPlanStatus.ACTIVE }
            ),
            SubscriptionFilterTab(
                FILTER_DRAFT,
                R.string.subscription_filter_draft_count,
                plans.count { it.status == SubscriptionPlanStatus.DRAFT }
            ),
            SubscriptionFilterTab(
                FILTER_ARCHIVED,
                R.string.subscription_filter_archived_count,
                plans.count { it.status == SubscriptionPlanStatus.ARCHIVED }
            )
        )
    }

    private fun filterPlans(plans: List<SubscriptionPlanModel>): List<SubscriptionPlanModel> {
        return when (selectedFilterId) {
            FILTER_ACTIVE -> plans.filter { it.status == SubscriptionPlanStatus.ACTIVE }
            FILTER_DRAFT -> plans.filter { it.status == SubscriptionPlanStatus.DRAFT }
            FILTER_ARCHIVED -> plans.filter { it.status == SubscriptionPlanStatus.ARCHIVED }
            else -> plans
        }
    }

    override fun onDestroyView() {
        loadingShimmerHelper.release()
        paginationShimmerHelper.release()
        _binding = null
        super.onDestroyView()
    }

    private companion object {
        const val FILTER_ALL = "all"
        const val FILTER_ACTIVE = "active"
        const val FILTER_DRAFT = "draft"
        const val FILTER_ARCHIVED = "archived"
        const val SHIMMER_ITEM_COUNT = 5
        const val PAGINATION_THRESHOLD_PX = 240
    }
}
