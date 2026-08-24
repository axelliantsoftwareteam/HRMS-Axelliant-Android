package com.axelliant.hris.features.subscriptions.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.core.content.getSystemService
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager
import com.axelliant.hris.R
import com.axelliant.hris.databinding.FragmentSubscriptionPlansBinding
import com.axelliant.hris.extention.showSuccessMsg
import com.axelliant.hris.ui.designsystem.components.AppTextFieldLayout

class SubscriptionPlansFragment : Fragment() {
    private var _binding: FragmentSubscriptionPlansBinding? = null
    private val binding get() = _binding!!
    private val filterTabAdapter = SubscriptionFilterTabAdapter { tab ->
        onFilterTabSelected(tab)
    }
    private var selectedFilterId = FILTER_ALL

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View {
        _binding = FragmentSubscriptionPlansBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) = with(binding) {
        appTopBar.titleView.textSize = 14f
        SubscriptionCommerceTabs.bind(
            this@SubscriptionPlansFragment,
            commerceTabs,
            SubscriptionCommerceTabs.PLANS,
        )
        filterTabsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = filterTabAdapter
        }
        submitFilterTabs(
            listOf(
                SubscriptionFilterTab(FILTER_ALL, R.string.subscription_filter_all),
                SubscriptionFilterTab(FILTER_ACTIVE, R.string.subscription_status_active),
                SubscriptionFilterTab(FILTER_DRAFT, R.string.subscription_status_draft),
                SubscriptionFilterTab(FILTER_ARCHIVED, R.string.subscription_status_archived),
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
        }
        filterButton.setOnClickListener { requireContext().showSuccessMsg(getString(R.string.subscriptions_filter_coming_soon)) }
        newPlanButton.setOnClickListener { findNavController().navigate(R.id.iaNewSubscriptionPlanFragment) }
    }

    fun submitFilterTabs(tabs: List<SubscriptionFilterTab>) {
        selectedFilterId = selectedFilterId.takeIf { id -> tabs.any { it.id == id } }
            ?: tabs.firstOrNull()?.id.orEmpty()
        filterTabAdapter.submitTabs(tabs, selectedFilterId)
    }

    private fun onFilterTabSelected(tab: SubscriptionFilterTab) {
        selectedFilterId = tab.id
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
            requireContext().getSystemService<InputMethodManager>()
                ?.hideSoftInputFromWindow(binding.searchEditText.windowToken, 0)
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private companion object {
        const val FILTER_ALL = "all"
        const val FILTER_ACTIVE = "active"
        const val FILTER_DRAFT = "draft"
        const val FILTER_ARCHIVED = "archived"
    }
}
