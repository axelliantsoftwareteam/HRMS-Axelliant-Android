package com.axelliant.hris.features.profiles.presentation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.axelliant.hris.BuildConfig
import com.axelliant.hris.R
import com.axelliant.hris.core.ui.UiState
import com.axelliant.hris.core.ui.toUiState
import com.axelliant.hris.features.profiles.data.ProfilesRepository
import com.axelliant.hris.features.profiles.data.local.ProfileLocalSettings
import com.axelliant.hris.features.profiles.data.local.ProfileSettingsStore
import com.axelliant.hris.features.profiles.data.remote.dto.MicrosoftProfileResponse
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfilesViewModel @Inject constructor(
    private val profilesRepository: ProfilesRepository,
    private val settingsStore: ProfileSettingsStore,
    @ApplicationContext context: Context
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        ProfileScreenUiState(
            settings = settingsStore.getSettings(),
            appName = context.getString(R.string.ia_app_name),
            appVersion = "v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"
        )
    )
    val uiState = _uiState.asStateFlow()

    fun loadProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(profileState = UiState.Loading) }
            val state = profilesRepository.getCurrentProfile().toUiState()
            _uiState.update { it.copy(profileState = state) }
        }
    }

    fun saveTimeZone(timeZoneId: String) {
        _uiState.update { it.copy(settings = settingsStore.saveTimeZone(timeZoneId)) }
    }

    fun saveDateFormat(dateFormat: String) {
        _uiState.update { it.copy(settings = settingsStore.saveDateFormat(dateFormat)) }
    }

    fun saveTimeFormat(timeFormat: String) {
        _uiState.update { it.copy(settings = settingsStore.saveTimeFormat(timeFormat)) }
    }
}

data class ProfileScreenUiState(
    val profileState: UiState<MicrosoftProfileResponse> = UiState.Idle,
    val settings: ProfileLocalSettings,
    val appName: String,
    val appVersion: String
)
