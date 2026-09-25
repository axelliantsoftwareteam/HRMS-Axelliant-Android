package com.axelliant.hris.features.profiles.presentation

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.axelliant.hris.R
import com.axelliant.hris.core.auth.GlobalLogoutCoordinator
import com.axelliant.hris.core.contracts.session.WorkspaceSessionProvider
import com.axelliant.hris.core.ui.UiState
import com.axelliant.hris.databinding.ItemProfileInfoRowBinding
import com.axelliant.hris.databinding.ItemProfileSettingRowBinding
import com.axelliant.hris.databinding.FragmentProfilesBinding
import com.axelliant.hris.features.internalapps.navigation.InternalAppsNavigator
import com.axelliant.hris.features.profiles.data.local.ProfileSettingsStore
import com.axelliant.hris.features.profiles.data.remote.dto.MicrosoftProfileResponse
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class ProfilesFragment : Fragment() {
    @Inject
    lateinit var workspaceSessionProvider: WorkspaceSessionProvider
    @Inject
    lateinit var globalLogoutCoordinator: GlobalLogoutCoordinator

    private val viewModel: ProfilesViewModel by viewModels()
    private var _binding: FragmentProfilesBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfilesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupInteractions()
        bindStaticRows()
        observeProfile()
        if (viewModel.uiState.value.profileState is UiState.Idle) {
            viewModel.loadProfile()
        }
    }

    private fun setupInteractions() = with(binding) {
        appTopBar.setOnBackClickListener {
            InternalAppsNavigator.returnToHomeShell(findNavController())
        }
        logoutRow.setOnClickListener { showLogoutConfirmationDialog() }
        timeZoneRow.root.setOnClickListener {
            showChoiceDialog(
                title = getString(R.string.profile_select_time_zone),
                options = ProfileSettingsStore.TIME_ZONE_OPTIONS,
                selectedOption = viewModel.uiState.value.settings.timeZoneId,
                onSelected = viewModel::saveTimeZone
            )
        }
        dateFormatRow.root.setOnClickListener {
            showChoiceDialog(
                title = getString(R.string.profile_select_date_format),
                options = ProfileSettingsStore.DATE_FORMAT_OPTIONS,
                selectedOption = viewModel.uiState.value.settings.dateFormat,
                onSelected = viewModel::saveDateFormat
            )
        }
        timeFormatRow.root.setOnClickListener {
            showChoiceDialog(
                title = getString(R.string.profile_select_time_format),
                options = ProfileSettingsStore.TIME_FORMAT_OPTIONS,
                selectedOption = viewModel.uiState.value.settings.timeFormat,
                onSelected = viewModel::saveTimeFormat
            )
        }
    }

    private fun bindStaticRows() = with(binding) {
        avatarText.text = getString(R.string.ia_app_name).toInitials()
        profileNameText.text = getString(R.string.profile_value_empty)
        profileEmailText.text = getString(R.string.profile_value_empty)

        companyRow.bindInfoRow(R.drawable.ic_detail_building, R.string.profile_company)
        departmentRow.bindInfoRow(R.drawable.ic_profile_briefcase, R.string.profile_department)
        jobTitleRow.bindInfoRow(R.drawable.ic_profile_id_card, R.string.profile_job_title)
        officeLocationRow.bindInfoRow(R.drawable.ic_profile_location, R.string.profile_office_location)
        businessPhoneRow.bindInfoRow(R.drawable.ic_profile_phone, R.string.profile_business_phone)
        mobilePhoneRow.bindInfoRow(R.drawable.ic_profile_mobile, R.string.profile_mobile_phone)
        mobilePhoneRow.infoDivider.isVisible = false

        timeZoneRow.bindSettingRow(
            iconRes = R.drawable.ic_detail_globe,
            titleRes = R.string.profile_time_zone,
            subtitleRes = R.string.profile_time_zone_subtitle
        )
        dateFormatRow.bindSettingRow(
            iconRes = R.drawable.ia_ic_filter_calendar,
            titleRes = R.string.profile_date_format,
            subtitleRes = R.string.profile_date_format_subtitle
        )
        timeFormatRow.bindSettingRow(
            iconRes = R.drawable.ic_profile_clock,
            titleRes = R.string.profile_time_format,
            subtitleRes = R.string.profile_time_format_subtitle
        )
        timeFormatRow.settingDivider.isVisible = false
    }

    private fun observeProfile() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    renderState(state)
                }
            }
        }
    }

    private fun renderState(state: ProfileScreenUiState) = with(binding) {
        progress.isVisible = state.profileState is UiState.Loading
        errorText.isVisible = state.profileState is UiState.Error || state.profileState is UiState.Unauthorized
        errorText.text = when (val profileState = state.profileState) {
            is UiState.Error -> profileState.message.ifBlank { getString(R.string.profile_load_error) }
            UiState.Unauthorized -> getString(R.string.profile_load_error)
            else -> getString(R.string.profile_load_error)
        }

        if (state.profileState is UiState.Success) {
            renderProfile(state.profileState.data)
        }
        renderSettings(state)
    }

    private fun renderProfile(profile: MicrosoftProfileResponse) = with(binding) {
        val name = profile.displayName.orDash()
        profileNameText.text = name
        profileEmailText.text = profile.mail.orDash()
        avatarText.text = name.toInitials()
        companyRow.infoValue.text = profile.companyName.orDash()
        departmentRow.infoValue.text = profile.department.orDash()
        jobTitleRow.infoValue.text = profile.jobTitle.orDash()
        officeLocationRow.infoValue.text = profile.officeLocation.orDash()
        businessPhoneRow.infoValue.text = profile.primaryBusinessPhone.orDash()
        mobilePhoneRow.infoValue.text = profile.mobilePhone.orDash()
    }

    private fun renderSettings(state: ProfileScreenUiState) = with(binding) {
        timeZoneRow.settingValueTop.text = state.settings.timeZoneOffsetLabel
        timeZoneRow.settingValueBottom.text = state.settings.timeZoneId
        timeZoneRow.settingValueBottom.isVisible = true
        dateFormatRow.settingValueTop.text = state.settings.dateFormat
        dateFormatRow.settingValueBottom.isVisible = false
        timeFormatRow.settingValueTop.text = state.settings.timeFormat
        timeFormatRow.settingValueBottom.isVisible = false
        appNameValueText.text = state.appName
        appVersionValueText.text = state.appVersion
    }

    private fun ItemProfileInfoRowBinding.bindInfoRow(
        iconRes: Int,
        labelRes: Int
    ) {
        infoIcon.setImageResource(iconRes)
        infoLabel.setText(labelRes)
        infoValue.text = getString(R.string.profile_value_empty)
    }

    private fun ItemProfileSettingRowBinding.bindSettingRow(
        iconRes: Int,
        titleRes: Int,
        subtitleRes: Int
    ) {
        settingIcon.setImageResource(iconRes)
        settingTitle.setText(titleRes)
        settingSubtitle.setText(subtitleRes)
    }

    private fun showChoiceDialog(
        title: String,
        options: List<String>,
        selectedOption: String,
        onSelected: (String) -> Unit
    ) {
        val selectedIndex = options.indexOf(selectedOption).coerceAtLeast(0)
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setSingleChoiceItems(options.toTypedArray(), selectedIndex) { dialog, which ->
                onSelected(options[which])
                dialog.dismiss()
            }
            .show()
    }

    private fun logoutAndOpenLogin() {
        viewLifecycleOwner.lifecycleScope.launch {
            globalLogoutCoordinator.logout()
            if (!isAdded) return@launch
            findNavController().navigate(
                R.id.commonLoginFragment,
                null,
                NavOptions.Builder()
                    .setPopUpTo(R.id.main_nav_graph, true)
                    .build()
            )
        }
    }

    private fun showLogoutConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.info))
            .setMessage(getString(R.string.logout_message))
            .setPositiveButton(getString(R.string.yes)) { _, _ ->
                logoutAndOpenLogin()
            }
            .setNegativeButton(getString(R.string.no)) { _, _ -> }
            .show()
    }

    private fun String?.orDash(): String = this?.takeIf { it.isNotBlank() }
        ?: getString(R.string.profile_value_empty)

    private fun String.toInitials(): String {
        val initials = trim()
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }
            .take(2)
            .joinToString("") { it.first().uppercaseChar().toString() }
        return initials.ifBlank { getString(R.string.ia_app_name).take(2) }
            .uppercase(Locale.US)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}


