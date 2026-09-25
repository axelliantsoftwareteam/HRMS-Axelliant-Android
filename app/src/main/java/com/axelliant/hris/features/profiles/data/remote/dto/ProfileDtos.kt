package com.axelliant.hris.features.profiles.data.remote.dto

import com.google.gson.annotations.SerializedName

data class MicrosoftProfileResponse(
    @SerializedName("displayName")
    val displayName: String? = null,
    @SerializedName("mail")
    val mail: String? = null,
    @SerializedName("department")
    val department: String? = null,
    @SerializedName("jobTitle")
    val jobTitle: String? = null,
    @SerializedName("companyName")
    val companyName: String? = null,
    @SerializedName("businessPhones")
    val businessPhones: List<String>? = emptyList(),
    @SerializedName("mobilePhone")
    val mobilePhone: String? = null,
    @SerializedName("officeLocation")
    val officeLocation: String? = null
) {
    val primaryBusinessPhone: String?
        get() = businessPhones.orEmpty().firstOrNull { it.isNotBlank() }
}
