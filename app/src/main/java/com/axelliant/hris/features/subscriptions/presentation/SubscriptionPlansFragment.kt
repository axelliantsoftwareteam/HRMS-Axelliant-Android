package com.axelliant.hris.features.subscriptions.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.axelliant.hris.R
import com.axelliant.hris.databinding.FragmentSubscriptionPlansBinding
import com.axelliant.hris.extention.showSuccessMsg

class SubscriptionPlansFragment : Fragment() {
    private var _binding: FragmentSubscriptionPlansBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View {
        _binding = FragmentSubscriptionPlansBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) = with(binding) {
        appTopBar.setOnBackClickListener { findNavController().navigateUp() }
        appTopBar.setOnSearchClickListener { searchInputLayout.requestFocus() }
        appTopBar.setOnActionClickListener { requireContext().showSuccessMsg(getString(R.string.subscriptions_more_actions_coming_soon)) }
        subscriptionsTab.setOnClickListener { findNavController().navigateUp() }
        filterButton.setOnClickListener { requireContext().showSuccessMsg(getString(R.string.subscriptions_filter_coming_soon)) }
        newPlanButton.setOnClickListener { findNavController().navigate(R.id.iaNewSubscriptionPlanFragment) }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
