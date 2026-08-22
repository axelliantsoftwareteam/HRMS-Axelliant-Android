package com.axelliant.hris.features.subscriptions.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.axelliant.hris.R
import com.axelliant.hris.databinding.FragmentNewSubscriptionPlanBinding
import com.axelliant.hris.extention.showSuccessMsg

class NewSubscriptionPlanFragment : Fragment() {
    private var _binding: FragmentNewSubscriptionPlanBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View {
        _binding = FragmentNewSubscriptionPlanBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) = with(binding) {
        appTopBar.titleView.textSize = 14f
        appTopBar.setOnBackClickListener { findNavController().navigateUp() }
        appTopBar.setOnActionClickListener { requireContext().showSuccessMsg(getString(R.string.subscriptions_more_actions_coming_soon)) }
        cancelButton.setOnClickListener { findNavController().navigateUp() }
        createPlanButton.setOnClickListener {
            requireContext().showSuccessMsg(getString(R.string.subscription_plan_created))
            findNavController().navigateUp()
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
