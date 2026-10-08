package com.axelliant.hris.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.axelliant.hris.config.AppState
import com.axelliant.hris.event.Event
import com.axelliant.hris.model.profile.CertificationCreateRequest
import com.axelliant.hris.model.profile.CertificationCreateResponse
import com.axelliant.hris.model.profile.CertificationSummary
import com.axelliant.hris.model.profile.ProfileCertification
import com.axelliant.hris.model.profile.ProfileResponse
import com.axelliant.hris.network.ApiResult
import com.axelliant.hris.repos.ProfileRepo

/**
 * State for the Profile and Certifications screens (and the add-certification form).
 * The dashboard employee that Home already loaded fills the profile immediately; HRIS data then
 * replaces blanks and keeps the dashboard fields on a failed refresh.
 */
class ProfileViewModel(private val profileRepo: ProfileRepo) : BaseViewModel() {

    data class ProfileUi(
        val profile: ProfileResponse = ProfileResponse(),
        val hasLoaded: Boolean = false,
        val loadFailed: Boolean = false,
        val refreshing: Boolean = false
    )

    data class CertificationsUi(
        val items: List<ProfileCertification> = emptyList(),
        val summary: CertificationSummary = CertificationSummary(),
        val hasLoaded: Boolean = false,
        val loadFailed: Boolean = false,
        val refreshing: Boolean = false
    )

    private val _profileUi = MutableLiveData(ProfileUi(ProfileResponse.fromDashboard(AppState.currentEmployee.value)))
    val profileUi: LiveData<ProfileUi> get() = _profileUi

    /** Fires when the profile could not be loaded and there is nothing to show. */
    private val _profileFailed = MutableLiveData<Event<Unit>>()
    val profileFailed: LiveData<Event<Unit>> get() = _profileFailed

    private val _certUi = MutableLiveData(CertificationsUi())
    val certificationsUi: LiveData<CertificationsUi> get() = _certUi

    /** Fires on every failed certification list load (the screen shows a toast). */
    private val _certFailed = MutableLiveData<Event<Unit>>()
    val certificationsFailed: LiveData<Event<Unit>> get() = _certFailed

    private val _submitting = MutableLiveData(false)
    val submitting: LiveData<Boolean> get() = _submitting

    private val _submitResult = MutableLiveData<Event<ApiResult<CertificationCreateResponse>>>()
    val submitResult: LiveData<Event<ApiResult<CertificationCreateResponse>>> get() = _submitResult

    private var profileLoading = false
    private var certLoading = false

    // ------------------------------------------------------------------------------------ profile

    /** [pull] is true for pull-to-refresh (no full-screen loader). */
    fun loadProfile(pull: Boolean = false) {
        val current = _profileUi.value ?: ProfileUi()
        if (profileLoading) {
            if (pull) _profileUi.value = current.copy(refreshing = true)
            return
        }
        profileLoading = true
        if (!pull) isLoading.value = Event(true)
        // Home may have loaded the dashboard employee after this screen was created.
        val dashboard = ProfileResponse.fromDashboard(AppState.currentEmployee.value)
        profileRepo.getProfile { result ->
            profileLoading = false
            if (!pull) isLoading.value = Event(false)
            val latest = _profileUi.value ?: current
            val loaded = (result as? ApiResult.Success)?.data?.takeIf { it.hasEmployeeRecord }
            if (loaded != null) {
                _profileUi.value = ProfileUi(loaded.mergedWith(dashboard), hasLoaded = true)
            } else {
                // Keep whatever is already on screen; only complain when there is nothing to show.
                val base = if (latest.profile.hasEmployeeRecord) latest.profile else latest.profile.mergedWith(dashboard)
                val nothing = !base.hasEmployeeRecord
                _profileUi.value = ProfileUi(base, hasLoaded = true, loadFailed = nothing)
                if (nothing) _profileFailed.value = Event(Unit)
            }
        }
    }

    // ------------------------------------------------------------------------------ certifications

    /** [pull] is true for pull-to-refresh; the full-screen loader only shows on the very first load. */
    fun loadCertifications(pull: Boolean = false) {
        val current = _certUi.value ?: CertificationsUi()
        if (certLoading) {
            if (pull) _certUi.value = current.copy(refreshing = true)
            return
        }
        certLoading = true
        val showOverlay = !pull && !current.hasLoaded
        if (showOverlay) isLoading.value = Event(true)
        if (pull) _certUi.value = current.copy(refreshing = true)
        profileRepo.getCertifications { result ->
            certLoading = false
            if (showOverlay) isLoading.value = Event(false)
            val latest = _certUi.value ?: current
            when (result) {
                is ApiResult.Success -> _certUi.value = CertificationsUi(
                    items = result.data.rows(),
                    summary = result.data.summary ?: CertificationSummary(),
                    hasLoaded = true
                )
                is ApiResult.Failure -> {
                    _certUi.value = latest.copy(hasLoaded = true, loadFailed = latest.items.isEmpty(), refreshing = false)
                    _certFailed.value = Event(Unit)
                }
            }
        }
    }

    /** Adds a certification; the list reloads by itself after HRIS accepts it. */
    fun submitCertification(request: CertificationCreateRequest) {
        if (_submitting.value == true) return
        _submitting.value = true
        profileRepo.createCertification(request) { result ->
            _submitting.value = false
            _submitResult.value = Event(result)
            if (result is ApiResult.Success) loadCertifications()
        }
    }
}
