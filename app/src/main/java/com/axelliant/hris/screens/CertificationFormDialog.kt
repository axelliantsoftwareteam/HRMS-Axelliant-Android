package com.axelliant.hris.screens

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.axelliant.hris.R
import com.axelliant.hris.databinding.CertDialogFormBinding
import com.axelliant.hris.extention.showErrorMsg
import com.axelliant.hris.extention.showSuccessMsg
import com.axelliant.hris.model.payslip.cleaned
import com.axelliant.hris.model.payslip.userMessage
import com.axelliant.hris.model.profile.CertificationCreateRequest
import com.axelliant.hris.network.observeResult
import com.axelliant.hris.viewmodel.ProfileViewModel
import com.google.android.material.datepicker.MaterialDatePicker
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Full-screen "Add Certification" form. Issue and expiry dates are optional: a switch turns each on
 * and a [MaterialDatePicker] sets it; an unset date is never sent. State is held by the parent
 * [CertificationsFragment]'s [ProfileViewModel].
 */
class CertificationFormDialog : DialogFragment() {

    private var _binding: CertDialogFormBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProfileViewModel by lazy { (requireParentFragment() as CertificationsFragment).viewModel }

    /** Picked dates as UTC-midnight milliseconds (what MaterialDatePicker returns); null = not set. */
    private var issueUtc: Long? = null
    private var expiryUtc: Long? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, com.google.android.material.R.style.Theme_MaterialComponents_Light_NoActionBar)
        if (savedInstanceState != null) {
            issueUtc = savedInstanceState.getLong(STATE_ISSUE, NONE).takeIf { it != NONE }
            expiryUtc = savedInstanceState.getLong(STATE_EXPIRY, NONE).takeIf { it != NONE }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong(STATE_ISSUE, issueUtc ?: NONE)
        outState.putLong(STATE_EXPIRY, expiryUtc ?: NONE)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = CertDialogFormBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.let { window ->
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
            window.statusBarColor = ContextCompat.getColor(requireContext(), R.color.ws_screen_bg)
            WindowCompat.getInsetsController(window, binding.root).isAppearanceLightStatusBars = true
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ViewCompat.setOnApplyWindowInsetsListener(binding.certFormRoot) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
            v.updatePadding(top = bars.top, bottom = bars.bottom)
            insets
        }

        binding.certFormToolbar.setOnBackClickListener { dismiss() }
        binding.certNameInput.doAfterTextChanged { binding.certNameLayout.error = null }

        binding.certIssueSwitch.isChecked = issueUtc != null
        binding.certExpirySwitch.isChecked = expiryUtc != null
        binding.certIssueSwitch.setOnCheckedChangeListener { _, checked ->
            if (checked) {
                if (issueUtc == null) issueUtc = MaterialDatePicker.todayInUtcMilliseconds()
                showPicker(R.string.cert_field_issue, issueUtc) { issueUtc = it }
            } else {
                issueUtc = null
            }
            updateDateRows()
        }
        binding.certExpirySwitch.setOnCheckedChangeListener { _, checked ->
            if (checked) {
                if (expiryUtc == null) expiryUtc = MaterialDatePicker.todayInUtcMilliseconds()
                showPicker(R.string.cert_field_expiry, expiryUtc) { expiryUtc = it }
            } else {
                expiryUtc = null
            }
            updateDateRows()
        }
        binding.certIssueValue.setOnClickListener { showPicker(R.string.cert_field_issue, issueUtc) { issueUtc = it } }
        binding.certExpiryValue.setOnClickListener { showPicker(R.string.cert_field_expiry, expiryUtc) { expiryUtc = it } }
        updateDateRows()

        binding.certSubmit.setOnClickListener { submit() }

        viewModel.submitting.observe(viewLifecycleOwner) { submitting ->
            binding.certSubmit.isEnabled = !submitting
            binding.certSubmit.setText(if (submitting) R.string.cert_submitting else R.string.cert_submit)
        }
        viewModel.submitResult.observeResult(
            viewLifecycleOwner,
            onError = { message -> requireContext().showErrorMsg(userMessage(message, getString(R.string.cert_error_add))) }
        ) { _, message ->
            requireContext().showSuccessMsg(message.cleaned() ?: getString(R.string.cert_added))
            dismissAllowingStateLoss()
        }
    }

    private fun showPicker(titleRes: Int, current: Long?, onPicked: (Long) -> Unit) {
        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText(titleRes)
            .setSelection(current ?: MaterialDatePicker.todayInUtcMilliseconds())
            .build()
        picker.addOnPositiveButtonClickListener { selection ->
            onPicked(selection)
            _binding?.let { updateDateRows() }
        }
        picker.show(childFragmentManager, "cert_date_picker")
    }

    private fun updateDateRows() {
        val issue = issueUtc
        val expiry = expiryUtc
        binding.certIssueValue.isVisible = issue != null
        binding.certIssueValue.text = issue?.let { displayDay(it) }
        binding.certExpiryValue.isVisible = expiry != null
        binding.certExpiryValue.text = expiry?.let { displayDay(it) }
        // Keep the switches in step without re-triggering their listeners.
        if (binding.certIssueSwitch.isChecked != (issue != null)) binding.certIssueSwitch.isChecked = issue != null
        if (binding.certExpirySwitch.isChecked != (expiry != null)) binding.certExpirySwitch.isChecked = expiry != null
        if (issue == null || expiry == null || expiry >= issue) binding.certExpiryError.isVisible = false
    }

    private fun submit() {
        val name = binding.certNameInput.text?.toString().orEmpty().trim()
        val issue = issueUtc
        val expiry = expiryUtc
        binding.certExpiryError.isVisible = false
        if (name.isEmpty()) {
            binding.certNameLayout.error = getString(R.string.cert_error_name)
            return
        }
        binding.certNameLayout.error = null
        if (issue != null && expiry != null && expiry < issue) {
            binding.certExpiryError.isVisible = true
            return
        }
        viewModel.submitCertification(
            CertificationCreateRequest(
                certification_name = name,
                issuing_authority = text(binding.certAuthorityInput.text),
                credential_id = text(binding.certCredentialIdInput.text),
                issue_date = issue?.let { apiDay(it) },
                expiry_date = expiry?.let { apiDay(it) },
                renewal_required = binding.certRenewalSwitch.isChecked,
                credential_url = text(binding.certUrlInput.text),
                notes = text(binding.certNotesInput.text)
            )
        )
    }

    private fun text(value: CharSequence?): String = value?.toString().orEmpty().trim()

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        private const val TAG = "CertificationFormDialog"
        private const val STATE_ISSUE = "issue"
        private const val STATE_EXPIRY = "expiry"
        private const val NONE = Long.MIN_VALUE

        fun show(fm: FragmentManager) {
            if (fm.findFragmentByTag(TAG) != null || fm.isStateSaved) return
            CertificationFormDialog().show(fm, TAG)
        }

        private fun utcFormat(pattern: String) =
            SimpleDateFormat(pattern, Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }

        /** "yyyy-MM-dd" for the API. */
        fun apiDay(utcMillis: Long): String = utcFormat("yyyy-MM-dd").format(Date(utcMillis))

        /** "dd-MM-yyyy" for display. */
        fun displayDay(utcMillis: Long): String = utcFormat("dd-MM-yyyy").format(Date(utcMillis))
    }
}
