package com.axelliant.hris.screens

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.axelliant.hris.R
import com.axelliant.hris.adapter.CertificationAdapter
import com.axelliant.hris.base.BaseFragment
import com.axelliant.hris.databinding.CertViewChipBinding
import com.axelliant.hris.databinding.FragmentCertificationsBinding
import com.axelliant.hris.event.EventObserver
import com.axelliant.hris.extention.showErrorMsg
import com.axelliant.hris.viewmodel.ProfileViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

/** Certification list with summary chips; the "+" button opens [CertificationFormDialog]. */
class CertificationsFragment : BaseFragment() {

    private var _binding: FragmentCertificationsBinding? = null
    private val binding get() = _binding!!

    /** Shared with the add form, which is a child dialog of this fragment. */
    val viewModel: ProfileViewModel by viewModel()

    private val adapter = CertificationAdapter { url -> openLink(url) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCertificationsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.certList.layoutManager = LinearLayoutManager(requireContext())
        binding.certList.adapter = adapter

        binding.certChipTotal.certChipLabel.setText(R.string.cert_chip_total)
        binding.certChipExpiring.certChipLabel.setText(R.string.cert_chip_expiring)
        binding.certChipRenewal.certChipLabel.setText(R.string.cert_chip_renewal)

        binding.certRefresh.setColorSchemeResources(R.color.ws_primary)
        binding.certRefresh.setOnChildScrollUpCallback { _, _ -> binding.certList.canScrollVertically(-1) }
        binding.certRefresh.setOnRefreshListener { viewModel.loadCertifications(pull = true) }
        binding.certFab.setOnClickListener { CertificationFormDialog.show(childFragmentManager) }

        viewModel.getIsLoading().observe(viewLifecycleOwner, EventObserver { loading ->
            if (loading) showDialog() else hideDialog()
        })
        viewModel.certificationsUi.observe(viewLifecycleOwner) { render(it) }
        viewModel.certificationsFailed.observe(viewLifecycleOwner, EventObserver {
            requireContext().showErrorMsg(getString(R.string.cert_error_load))
        })
    }

    /** Reload on every resume so a certification added elsewhere shows up. */
    override fun onResume() {
        super.onResume()
        viewModel.loadCertifications()
    }

    private fun render(state: ProfileViewModel.CertificationsUi) {
        bindChip(binding.certChipTotal, state.summary.total ?: 0, R.color.ws_primary)
        bindChip(binding.certChipExpiring, state.summary.expiring_soon ?: 0, R.color.ws_amber)
        bindChip(binding.certChipRenewal, state.summary.needs_renewal ?: 0, R.color.ws_red)

        adapter.submit(state.items)
        binding.certRefresh.isRefreshing = state.refreshing

        if (state.items.isEmpty() && state.hasLoaded) {
            binding.certList.visibility = View.GONE
            if (state.loadFailed) {
                binding.certEmpty.show(R.drawable.ws_ic_certificate, getString(R.string.cert_failed_title), getString(R.string.cert_failed_message))
            } else {
                binding.certEmpty.show(R.drawable.ws_ic_certificate, getString(R.string.cert_empty_title), getString(R.string.cert_empty_message))
            }
        } else {
            binding.certEmpty.hide()
            binding.certList.visibility = View.VISIBLE
        }
    }

    private fun bindChip(chip: CertViewChipBinding, value: Int, color: Int) {
        chip.certChipValue.text = value.toString()
        chip.certChipValue.setTextColor(requireContext().getColor(if (value == 0) R.color.ws_grey else color))
    }

    private fun openLink(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (e: ActivityNotFoundException) {
            requireContext().showErrorMsg(getString(R.string.cert_open_failed))
        }
    }

    override fun onDestroyView() {
        binding.certList.adapter = null
        _binding = null
        super.onDestroyView()
    }
}
