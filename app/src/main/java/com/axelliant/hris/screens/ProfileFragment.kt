package com.axelliant.hris.screens

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.axelliant.hris.R
import com.axelliant.hris.base.BaseFragment
import com.axelliant.hris.databinding.FragmentProfileBinding
import com.axelliant.hris.event.EventObserver
import com.axelliant.hris.extention.showErrorMsg
import com.axelliant.hris.viewmodel.ProfileViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

/** The signed-in employee's profile. The dashboard employee fills the page at once; HRIS data refines it. */
class ProfileFragment : BaseFragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ProfileViewModel by viewModel()
    private var renderer: ProfileRenderer? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        renderer = ProfileRenderer(requireContext(), binding.profContent)

        binding.profRefresh.setColorSchemeResources(R.color.ws_primary)
        binding.profRefresh.setOnRefreshListener { viewModel.loadProfile(pull = true) }
        binding.profTryAgain.setOnClickListener { viewModel.loadProfile() }

        viewModel.getIsLoading().observe(viewLifecycleOwner, EventObserver { loading ->
            if (loading) showDialog() else hideDialog()
        })
        viewModel.profileUi.observe(viewLifecycleOwner) { render(it) }
        viewModel.profileFailed.observe(viewLifecycleOwner, EventObserver {
            requireContext().showErrorMsg(getString(R.string.prof_error_load))
        })

        if (viewModel.profileUi.value?.hasLoaded != true) viewModel.loadProfile()
    }

    private fun render(state: ProfileViewModel.ProfileUi) {
        binding.profRefresh.isRefreshing = state.refreshing
        if (state.profile.hasEmployeeRecord) {
            binding.profUnavailable.visibility = View.GONE
            binding.profContent.visibility = View.VISIBLE
            renderer?.render(state.profile)
        } else {
            binding.profContent.removeAllViews()
            binding.profUnavailable.visibility = if (state.loadFailed) View.VISIBLE else View.GONE
        }
    }

    override fun onDestroyView() {
        renderer = null
        _binding = null
        super.onDestroyView()
    }
}
