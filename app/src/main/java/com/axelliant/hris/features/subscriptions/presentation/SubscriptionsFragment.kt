package com.axelliant.hris.features.subscriptions.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.axelliant.hris.R
import com.axelliant.hris.databinding.FragmentSubscriptionsBinding
import com.axelliant.hris.extention.showSuccessMsg

class SubscriptionsFragment : Fragment() {

    private var _binding: FragmentSubscriptionsBinding? = null
    private val binding get() = _binding!!

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
        binding.appTopBar.setOnBackClickListener { findNavController().navigateUp() }
        binding.appTopBar.setOnSearchClickListener {
            binding.searchInputLayout.requestFocus()
        }
        binding.appTopBar.setOnActionClickListener {
            requireContext().showSuccessMsg(getString(R.string.subscriptions_more_actions_coming_soon))
        }
        binding.filterButton.setOnClickListener {
            requireContext().showSuccessMsg(getString(R.string.subscriptions_filter_coming_soon))
        }
        binding.newSubscriptionButton.setOnClickListener {
            findNavController().navigate(R.id.iaNewSubscriptionFragment)
        }
        binding.plansTab.setOnClickListener {
            findNavController().navigate(R.id.iaSubscriptionPlansFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
