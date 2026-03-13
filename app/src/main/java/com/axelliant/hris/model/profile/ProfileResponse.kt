package com.axelliant.hris.model.profile

import com.axelliant.hris.model.base.Meta

data class ProfileResponse(
    val user_id: String? = null,
    val employee_name: String? = null,
    val father_name: String? = null,
    val date_of_birth: String? = null,
    val gender: String? = null,
    val custom_cnic: String? = null,
    val company: String? = null,
    val custom_employee_code: String? = null,
    val designation: String? = null,
    val department: String? = null,
    val date_of_joining: String? = null,
    val final_confirmation_date: String? = null,
    val personal_email: String? = null,
    val cell_number: String? = null,
    val emergency_phone_number: String? = null,
    val relation: String? = null,
    val permanent_address: String? = null,
    val reports_to: ProfileManager? = null,
    val certifications: List<ProfileCertification> = emptyList(),
    val certifications_summary: CertificationSummary? = null,
    val meta: Meta = Meta("")
)

data class ProfileManager(
    val employee_name: String? = null,
    val user_id: String? = null,
    val name: String? = null
)

data class ProfileCertification(
    val name: String? = null,
    val certification_name: String? = null,
    val issuing_authority: String? = null,
    val credential_id: String? = null,
    val issue_date: String? = null,
    val expiry_date: String? = null,
    val renewal_required: Boolean? = null,
    val credential_url: String? = null,
    val notes: String? = null,
    val status: String? = null,
    val tone: String? = null,
    val days_until_expiry: Int? = null,
    val needs_renewal: Boolean? = null,
    val is_expired: Boolean? = null,
    val is_expiring_soon: Boolean? = null
)

data class CertificationSummary(
    val total: Int? = 0,
    val active: Int? = 0,
    val expiring_soon: Int? = 0,
    val expired: Int? = 0,
    val no_expiry: Int? = 0,
    val needs_renewal: Int? = 0
)

data class CertificationListResponse(
    val items: List<ProfileCertification> = emptyList(),
    val summary: CertificationSummary? = null,
    val meta: Meta = Meta("")
)

data class CertificationCreateRequest(
    val certification_name: String,
    val issuing_authority: String? = null,
    val credential_id: String? = null,
    val issue_date: String? = null,
    val expiry_date: String? = null,
    val renewal_required: Boolean = true,
    val credential_url: String? = null,
    val notes: String? = null
)

data class CertificationCreateResponse(
    val item: ProfileCertification? = null,
    val summary: CertificationSummary? = null,
    val status_message: String? = null,
    val meta: Meta = Meta("")
)
