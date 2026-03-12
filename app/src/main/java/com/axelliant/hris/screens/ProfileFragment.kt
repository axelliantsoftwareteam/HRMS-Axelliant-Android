package com.axelliant.hris.screens

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.axelliant.hris.adapter.ProfileCertificationAdapter
import com.axelliant.hris.base.BaseFragment
import com.axelliant.hris.databinding.FragmentProfileBinding
import com.axelliant.hris.event.EventObserver
import com.axelliant.hris.extention.showErrorMsg
import com.axelliant.hris.extention.showSuccessMsg
import com.axelliant.hris.model.profile.CertificationCreateRequest
import com.axelliant.hris.model.profile.ProfileCertification
import com.axelliant.hris.model.profile.ProfileResponse
import com.axelliant.hris.viewmodel.ProfileViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.util.Calendar

class ProfileFragment : BaseFragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val profileViewModel: ProfileViewModel by viewModel()
    private val certificationsAdapter = ProfileCertificationAdapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupList()
        setupActions()
        observeViewModel()
        profileViewModel.loadProfile()
        profileViewModel.loadCertifications()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private fun setupList() {
        binding.rvCertifications.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = certificationsAdapter
        }
    }

    private fun setupActions() {
        binding.etIssueDate.setOnClickListener { openDatePicker(binding.etIssueDate) }
        binding.etExpiryDate.setOnClickListener { openDatePicker(binding.etExpiryDate) }
        binding.btnAddCertification.setOnClickListener { submitCertification() }
    }

    private fun observeViewModel() {
        profileViewModel.getIsLoading().observe(viewLifecycleOwner, EventObserver {
            if (it) showDialog() else hideDialog()
        })

        profileViewModel.profileResponse.observe(viewLifecycleOwner, EventObserver { profile ->
            if (profile != null) {
                renderProfile(profile)
                if (profile.certifications.isNotEmpty()) {
                    renderCertifications(profile.certifications)
                }
                renderCertificationSummary(
                    total = profile.certifications_summary?.total ?: profile.certifications.size,
                    renewalDue = profile.certifications_summary?.needs_renewal ?: 0
                )
            }
        })

        profileViewModel.certificationsResponse.observe(viewLifecycleOwner, EventObserver { response ->
            if (response != null) {
                renderCertifications(response.items)
                renderCertificationSummary(
                    total = response.summary?.total ?: response.items.size,
                    renewalDue = response.summary?.needs_renewal ?: 0
                )
            }
        })

        profileViewModel.certificationCreateResponse.observe(viewLifecycleOwner, EventObserver { response ->
            if (response?.meta?.status == true) {
                requireContext().showSuccessMsg(response.status_message ?: "Certification has been added.")
                clearCertificationForm()
                profileViewModel.loadProfile()
                profileViewModel.loadCertifications()
            } else if (response != null) {
                requireContext().showErrorMsg(response.meta.message)
            }
        })
    }

    private fun renderProfile(profile: ProfileResponse) {
        binding.tvEmployeeName.text = profile.employee_name ?: "Employee profile"
        binding.tvEmployeeDesignation.text = profile.designation ?: "Designation not available"
        binding.tvEmployeeMeta.text = listOfNotNull(
            profile.custom_employee_code?.takeIf { it.isNotBlank() },
            profile.department?.takeIf { it.isNotBlank() }
        ).joinToString(" · ").ifBlank { "Employee details are not available yet." }

        binding.tvContactMeta.text = listOfNotNull(
            profile.user_id?.takeIf { it.isNotBlank() },
            profile.cell_number?.takeIf { it.isNotBlank() },
            profile.company?.takeIf { it.isNotBlank() }
        ).joinToString(" · ").ifBlank { "Contact details are not available yet." }

        binding.tvPersonalBlock.text = buildString {
            appendLine("Father / emergency contact: ${profile.father_name.orDash()}")
            appendLine("Date of birth: ${profile.date_of_birth.orDash()}")
            appendLine("Gender: ${profile.gender.orDash()}")
            appendLine("CNIC: ${profile.custom_cnic.orDash()}")
            appendLine("Manager: ${profile.reports_to?.employee_name.orDash()}")
            appendLine("Date of joining: ${profile.date_of_joining.orDash()}")
            appendLine("Confirmation date: ${profile.final_confirmation_date.orDash()}")
            appendLine("Personal email: ${profile.personal_email.orDash()}")
            appendLine("Emergency phone: ${profile.emergency_phone_number.orDash()}")
            append("Address: ${profile.permanent_address.orDash()}")
        }
    }

    private fun renderCertifications(items: List<ProfileCertification>) {
        certificationsAdapter.submitList(items)
        binding.tvEmptyCertifications.isVisible = items.isEmpty()
        binding.rvCertifications.isVisible = items.isNotEmpty()
    }

    private fun renderCertificationSummary(total: Int, renewalDue: Int) {
        binding.tvCertificationSummary.text = "$total tracked · $renewalDue renewal due"
    }

    private fun submitCertification() {
        val certificationName = binding.etCertificationName.text?.toString()?.trim().orEmpty()
        if (certificationName.isBlank()) {
            requireContext().showErrorMsg("Enter the certification name first.")
            return
        }

        val issueDate = binding.etIssueDate.text?.toString()?.trim().orEmpty()
        val expiryDate = binding.etExpiryDate.text?.toString()?.trim().orEmpty()
        if (issueDate.isNotBlank() && expiryDate.isNotBlank() && expiryDate < issueDate) {
            requireContext().showErrorMsg("Expiry date cannot be earlier than the issue date.")
            return
        }

        profileViewModel.createCertification(
            CertificationCreateRequest(
                certification_name = certificationName,
                issuing_authority = binding.etCertificationAuthority.text?.toString()?.trim(),
                credential_id = binding.etCertificationId.text?.toString()?.trim(),
                issue_date = issueDate.ifBlank { null },
                expiry_date = expiryDate.ifBlank { null },
                renewal_required = binding.cbRenewalRequired.isChecked,
                credential_url = binding.etCertificationUrl.text?.toString()?.trim(),
                notes = binding.etCertificationNotes.text?.toString()?.trim()
            )
        )
    }

    private fun clearCertificationForm() {
        binding.etCertificationName.text?.clear()
        binding.etCertificationAuthority.text?.clear()
        binding.etCertificationId.text?.clear()
        binding.etIssueDate.text?.clear()
        binding.etExpiryDate.text?.clear()
        binding.etCertificationUrl.text?.clear()
        binding.etCertificationNotes.text?.clear()
        binding.cbRenewalRequired.isChecked = true
    }

    private fun openDatePicker(targetView: com.google.android.material.textfield.TextInputEditText) {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                targetView.setText(String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun String?.orDash(): String = this?.takeIf { it.isNotBlank() } ?: "Not available"
}
