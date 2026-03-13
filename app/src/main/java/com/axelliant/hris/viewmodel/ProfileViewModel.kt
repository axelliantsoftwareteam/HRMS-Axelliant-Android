package com.axelliant.hris.viewmodel

import androidx.lifecycle.MutableLiveData
import com.axelliant.hris.event.Event
import com.axelliant.hris.model.profile.CertificationCreateRequest
import com.axelliant.hris.model.profile.CertificationCreateResponse
import com.axelliant.hris.model.profile.CertificationListResponse
import com.axelliant.hris.model.profile.ProfileResponse
import com.axelliant.hris.repos.ProfileRepo

class ProfileViewModel(private val profileRepo: ProfileRepo) : BaseViewModel() {

    val profileResponse: MutableLiveData<Event<ProfileResponse?>> by lazy { MutableLiveData<Event<ProfileResponse?>>() }
    val certificationsResponse: MutableLiveData<Event<CertificationListResponse?>> by lazy { MutableLiveData<Event<CertificationListResponse?>>() }
    val certificationCreateResponse: MutableLiveData<Event<CertificationCreateResponse?>> by lazy { MutableLiveData<Event<CertificationCreateResponse?>>() }

    fun loadProfile() {
        isLoading.value = Event(true)
        profileRepo.getProfile().observeForever { data ->
            isLoading.value = Event(false)
            profileResponse.value = Event(data?.message?.data)
        }
    }

    fun loadCertifications() {
        isLoading.value = Event(true)
        profileRepo.getCertifications().observeForever { data ->
            isLoading.value = Event(false)
            certificationsResponse.value = Event(data?.message?.data)
        }
    }

    fun createCertification(request: CertificationCreateRequest) {
        isLoading.value = Event(true)
        profileRepo.createCertification(request).observeForever { data ->
            isLoading.value = Event(false)
            certificationCreateResponse.value = Event(data?.message?.data)
        }
    }
}
